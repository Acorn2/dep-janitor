package com.depjanitor.core.model

enum class TimeBasis(val label: String) {
    LAST_ACCESSED("最近访问时间"),
    LAST_MODIFIED("最近修改时间"),
    DERIVED_USAGE("推测使用时间"),
    UNKNOWN("未知时间依据"),
}

fun TimeBasis.displayLabel(isFallback: Boolean = false): String {
    return if (isFallback) "$label（回退推断）" else label
}
