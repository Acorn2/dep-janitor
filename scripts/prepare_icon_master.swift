#!/usr/bin/env swift

import AppKit
import CoreGraphics
import Foundation
import ImageIO
import UniformTypeIdentifiers

struct Config {
    var inputPath: String
    var outputPath: String
    var paddingRatio: Double = 0.06
    var threshold: Int = 18
    var outputSize: Int = 1024
    var contentInsetRatio: Double = 0.0
    var shape: String = "macos"
    var cornerRadiusRatio: Double = 0.22
    var squircleExponent: Double = 5.0
    var applyRoundedMask: Bool = true
}

enum ScriptError: Error, CustomStringConvertible {
    case usage(String)
    case loadFailed(String)
    case saveFailed(String)
    case emptyContent

    var description: String {
        switch self {
        case .usage(let message): return message
        case .loadFailed(let message): return "Load failed: \(message)"
        case .saveFailed(let message): return "Save failed: \(message)"
        case .emptyContent: return "Could not detect icon content after background removal."
        }
    }
}

func parseArgs() throws -> Config {
    let args = Array(CommandLine.arguments.dropFirst())
    guard args.count >= 2 else {
        throw ScriptError.usage("""
        Usage:
          swift scripts/prepare_icon_master.swift <input.png> <output.png> [--padding 0.06] [--threshold 18] [--size 1024] [--content-inset 0.00] [--shape macos|rounded-rect] [--corner-radius 0.22] [--squircle-exponent 5.0] [--no-rounded-mask]

        Example:
          swift scripts/prepare_icon_master.swift tmp_icons/new-icon.png app-desktop/src/main/resources/icons/source/dep-janitor-1024.png
        """)
    }

    var config = Config(inputPath: args[0], outputPath: args[1])
    var index = 2
    while index < args.count {
        let flag = args[index]
        if flag == "--no-rounded-mask" {
            config.applyRoundedMask = false
            index += 1
            continue
        }
        guard index + 1 < args.count else {
            throw ScriptError.usage("Missing value for \(flag)")
        }
        let value = args[index + 1]
        switch flag {
        case "--padding":
            config.paddingRatio = Double(value) ?? config.paddingRatio
        case "--threshold":
            config.threshold = Int(value) ?? config.threshold
        case "--size":
            config.outputSize = Int(value) ?? config.outputSize
        case "--content-inset":
            config.contentInsetRatio = Double(value) ?? config.contentInsetRatio
        case "--shape":
            config.shape = value
        case "--corner-radius":
            config.cornerRadiusRatio = Double(value) ?? config.cornerRadiusRatio
        case "--squircle-exponent":
            config.squircleExponent = Double(value) ?? config.squircleExponent
        default:
            throw ScriptError.usage("Unknown flag: \(flag)")
        }
        index += 2
    }
    return config
}

func loadRGBAImage(from path: String) throws -> (width: Int, height: Int, data: [UInt8]) {
    let url = URL(fileURLWithPath: path)
    guard let source = CGImageSourceCreateWithURL(url as CFURL, nil),
          let image = CGImageSourceCreateImageAtIndex(source, 0, nil) else {
        throw ScriptError.loadFailed("Unable to decode image at \(path)")
    }

    let width = image.width
    let height = image.height
    let bytesPerRow = width * 4
    var data = [UInt8](repeating: 0, count: height * bytesPerRow)

    guard let context = CGContext(
        data: &data,
        width: width,
        height: height,
        bitsPerComponent: 8,
        bytesPerRow: bytesPerRow,
        space: CGColorSpaceCreateDeviceRGB(),
        bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue
    ) else {
        throw ScriptError.loadFailed("Unable to create bitmap context")
    }

    context.draw(image, in: CGRect(x: 0, y: 0, width: width, height: height))
    return (width, height, data)
}

func signedPow(_ value: CGFloat, exponent: CGFloat) -> CGFloat {
    let magnitude = pow(abs(value), exponent)
    return value < 0 ? -magnitude : magnitude
}

func pixelOffset(x: Int, y: Int, width: Int) -> Int {
    (y * width + x) * 4
}

func averageBackgroundColor(width: Int, height: Int, data: [UInt8]) -> (r: Int, g: Int, b: Int) {
    let sampleRadius = min(8, max(2, min(width, height) / 64))
    var totalR = 0
    var totalG = 0
    var totalB = 0
    var count = 0

    let corners = [
        (0, 0),
        (width - 1, 0),
        (0, height - 1),
        (width - 1, height - 1),
    ]

    for (cx, cy) in corners {
        let minX = max(0, cx - sampleRadius)
        let maxX = min(width - 1, cx + sampleRadius)
        let minY = max(0, cy - sampleRadius)
        let maxY = min(height - 1, cy + sampleRadius)
        for y in minY...maxY {
            for x in minX...maxX {
                let o = pixelOffset(x: x, y: y, width: width)
                totalR += Int(data[o])
                totalG += Int(data[o + 1])
                totalB += Int(data[o + 2])
                count += 1
            }
        }
    }

    return (
        r: totalR / max(count, 1),
        g: totalG / max(count, 1),
        b: totalB / max(count, 1)
    )
}

func isSimilarToBackground(offset: Int, data: [UInt8], bg: (r: Int, g: Int, b: Int), threshold: Int) -> Bool {
    let r = Int(data[offset])
    let g = Int(data[offset + 1])
    let b = Int(data[offset + 2])
    let maxDiff = max(abs(r - bg.r), max(abs(g - bg.g), abs(b - bg.b)))
    return maxDiff <= threshold
}

func floodFillExternalBackground(width: Int, height: Int, data: [UInt8], bg: (r: Int, g: Int, b: Int), threshold: Int) -> [Bool] {
    var visited = [Bool](repeating: false, count: width * height)
    var queue = [Int]()
    queue.reserveCapacity(width * 2 + height * 2)

    func enqueueIfNeeded(_ x: Int, _ y: Int) {
        let idx = y * width + x
        guard !visited[idx] else { return }
        let o = idx * 4
        guard isSimilarToBackground(offset: o, data: data, bg: bg, threshold: threshold) else { return }
        visited[idx] = true
        queue.append(idx)
    }

    for x in 0..<width {
        enqueueIfNeeded(x, 0)
        enqueueIfNeeded(x, height - 1)
    }
    if height > 2 {
        for y in 1..<(height - 1) {
            enqueueIfNeeded(0, y)
            enqueueIfNeeded(width - 1, y)
        }
    }

    var head = 0
    while head < queue.count {
        let idx = queue[head]
        head += 1
        let x = idx % width
        let y = idx / width

        if x > 0 { enqueueIfNeeded(x - 1, y) }
        if x + 1 < width { enqueueIfNeeded(x + 1, y) }
        if y > 0 { enqueueIfNeeded(x, y - 1) }
        if y + 1 < height { enqueueIfNeeded(x, y + 1) }
    }

    return visited
}

func transparentizedData(width: Int, height: Int, data: [UInt8], externalMask: [Bool]) -> ([UInt8], CGRect)? {
    var out = data
    var minX = width
    var minY = height
    var maxX = -1
    var maxY = -1

    for y in 0..<height {
        for x in 0..<width {
            let idx = y * width + x
            let o = idx * 4
            if externalMask[idx] {
                out[o] = 0
                out[o + 1] = 0
                out[o + 2] = 0
                out[o + 3] = 0
            } else if out[o + 3] > 0 {
                minX = min(minX, x)
                minY = min(minY, y)
                maxX = max(maxX, x)
                maxY = max(maxY, y)
            }
        }
    }

    guard maxX >= minX, maxY >= minY else { return nil }
    let rect = CGRect(
        x: minX,
        y: minY,
        width: maxX - minX + 1,
        height: maxY - minY + 1
    )
    return (out, rect)
}

func makeSquareCrop(contentRect: CGRect, imageWidth: Int, imageHeight: Int, paddingRatio: Double) -> CGRect {
    let contentWidth = contentRect.width
    let contentHeight = contentRect.height
    let baseSide = max(contentWidth, contentHeight)
    let padding = floor(baseSide * paddingRatio)
    let side = min(CGFloat(max(imageWidth, imageHeight)), baseSide + padding * 2)

    var originX = contentRect.midX - side / 2
    var originY = contentRect.midY - side / 2

    originX = max(0, min(originX, CGFloat(imageWidth) - side))
    originY = max(0, min(originY, CGFloat(imageHeight) - side))

    return CGRect(x: originX.rounded(.down), y: originY.rounded(.down), width: side.rounded(.down), height: side.rounded(.down))
}

func cgImageFromRGBA(width: Int, height: Int, data: inout [UInt8]) throws -> CGImage {
    let bytesPerRow = width * 4
    guard let provider = CGDataProvider(data: NSData(bytes: &data, length: data.count)) else {
        throw ScriptError.loadFailed("Unable to create data provider")
    }
    guard let image = CGImage(
        width: width,
        height: height,
        bitsPerComponent: 8,
        bitsPerPixel: 32,
        bytesPerRow: bytesPerRow,
        space: CGColorSpaceCreateDeviceRGB(),
        bitmapInfo: CGBitmapInfo(rawValue: CGImageAlphaInfo.premultipliedLast.rawValue),
        provider: provider,
        decode: nil,
        shouldInterpolate: true,
        intent: .defaultIntent
    ) else {
        throw ScriptError.loadFailed("Unable to create CGImage")
    }
    return image
}

func makeSquirclePath(in rect: CGRect, exponent: CGFloat, sampleCount: Int = 128) -> CGPath {
    let path = CGMutablePath()
    let centerX = rect.midX
    let centerY = rect.midY
    let radiusX = rect.width / 2
    let radiusY = rect.height / 2
    let coordinateExponent = 2 / exponent

    for sample in 0...sampleCount {
        let t = CGFloat(sample) / CGFloat(sampleCount) * .pi * 2
        let x = centerX + radiusX * signedPow(cos(t), exponent: coordinateExponent)
        let y = centerY + radiusY * signedPow(sin(t), exponent: coordinateExponent)
        if sample == 0 {
            path.move(to: CGPoint(x: x, y: y))
        } else {
            path.addLine(to: CGPoint(x: x, y: y))
        }
    }
    path.closeSubpath()
    return path
}

func render(
    image: CGImage,
    cropRect: CGRect,
    outputSize: Int,
    applyRoundedMask: Bool,
    shape: String,
    cornerRadiusRatio: Double,
    squircleExponent: Double,
    contentInsetRatio: Double
) throws -> CGImage {
    guard let context = CGContext(
        data: nil,
        width: outputSize,
        height: outputSize,
        bitsPerComponent: 8,
        bytesPerRow: outputSize * 4,
        space: CGColorSpaceCreateDeviceRGB(),
        bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue
    ) else {
        throw ScriptError.saveFailed("Unable to create output context")
    }

    context.interpolationQuality = .high
    context.clear(CGRect(x: 0, y: 0, width: outputSize, height: outputSize))
    guard let cropped = image.cropping(to: cropRect) else {
        throw ScriptError.saveFailed("Unable to crop image")
    }

    let canvasRect = CGRect(x: 0, y: 0, width: outputSize, height: outputSize)
    let drawInset = CGFloat(outputSize) * CGFloat(max(0, contentInsetRatio))
    let drawRect = canvasRect.insetBy(dx: drawInset, dy: drawInset)

    if applyRoundedMask {
        let rect = canvasRect.insetBy(dx: 1, dy: 1)
        let path: CGPath
        if shape == "rounded-rect" {
            let radius = CGFloat(outputSize) * CGFloat(cornerRadiusRatio)
            path = CGPath(
                roundedRect: rect,
                cornerWidth: radius,
                cornerHeight: radius,
                transform: nil
            )
        } else {
            path = makeSquirclePath(in: rect, exponent: CGFloat(squircleExponent))
        }
        context.addPath(path)
        context.clip()
    }

    context.draw(cropped, in: drawRect)

    guard let outImage = context.makeImage() else {
        throw ScriptError.saveFailed("Unable to render output image")
    }
    return outImage
}

func savePNG(_ image: CGImage, to path: String) throws {
    let url = URL(fileURLWithPath: path)
    let directory = url.deletingLastPathComponent()
    try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)

    guard let destination = CGImageDestinationCreateWithURL(
        url as CFURL,
        UTType.png.identifier as CFString,
        1,
        nil
    ) else {
        throw ScriptError.saveFailed("Unable to create image destination")
    }

    CGImageDestinationAddImage(destination, image, nil)
    guard CGImageDestinationFinalize(destination) else {
        throw ScriptError.saveFailed("Unable to write PNG to \(path)")
    }
}

do {
    let config = try parseArgs()
    let loaded = try loadRGBAImage(from: config.inputPath)
    let bg = averageBackgroundColor(width: loaded.width, height: loaded.height, data: loaded.data)
    let externalMask = floodFillExternalBackground(
        width: loaded.width,
        height: loaded.height,
        data: loaded.data,
        bg: bg,
        threshold: config.threshold
    )

    guard let (transparentData, contentRect) = transparentizedData(
        width: loaded.width,
        height: loaded.height,
        data: loaded.data,
        externalMask: externalMask
    ) else {
        throw ScriptError.emptyContent
    }

    let cropRect = makeSquareCrop(
        contentRect: contentRect,
        imageWidth: loaded.width,
        imageHeight: loaded.height,
        paddingRatio: config.paddingRatio
    )

    var mutableData = transparentData
    let cgImage = try cgImageFromRGBA(width: loaded.width, height: loaded.height, data: &mutableData)
    let rendered = try render(
        image: cgImage,
        cropRect: cropRect,
        outputSize: config.outputSize,
        applyRoundedMask: config.applyRoundedMask,
        shape: config.shape,
        cornerRadiusRatio: config.cornerRadiusRatio,
        squircleExponent: config.squircleExponent,
        contentInsetRatio: config.contentInsetRatio
    )
    try savePNG(rendered, to: config.outputPath)

    print("Prepared icon master:")
    print(" - input: \(config.inputPath)")
    print(" - output: \(config.outputPath)")
    print(" - detected background: rgb(\(bg.r), \(bg.g), \(bg.b))")
    print(" - crop rect: \(Int(cropRect.origin.x)),\(Int(cropRect.origin.y)) \(Int(cropRect.width))x\(Int(cropRect.height))")
    print(" - output size: \(config.outputSize)x\(config.outputSize)")
    print(" - rounded mask: \(config.applyRoundedMask ? "on" : "off")")
    if config.applyRoundedMask {
        print(" - shape: \(config.shape)")
        if config.shape == "rounded-rect" {
            print(" - corner radius ratio: \(config.cornerRadiusRatio)")
        } else {
            print(" - squircle exponent: \(config.squircleExponent)")
        }
    }
    print(" - content inset ratio: \(config.contentInsetRatio)")
} catch {
    fputs("\(error)\n", stderr)
    exit(1)
}
