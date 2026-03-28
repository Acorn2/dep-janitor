package com.depjanitor.core.model

enum class WhitelistEntryType(val label: String) {
    COORDINATE("坐标"),
    PATH("路径"),
    SOURCE("来源"),
}

data class WhitelistEntry(
    val type: WhitelistEntryType,
    val value: String,
) {
    companion object {
        fun coordinate(value: String): WhitelistEntry = WhitelistEntry(WhitelistEntryType.COORDINATE, value)
        fun path(value: String): WhitelistEntry = WhitelistEntry(WhitelistEntryType.PATH, value)
        fun source(value: String): WhitelistEntry = WhitelistEntry(WhitelistEntryType.SOURCE, value)
    }
}
