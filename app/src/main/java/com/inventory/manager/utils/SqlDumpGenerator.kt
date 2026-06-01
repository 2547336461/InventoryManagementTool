package com.inventory.manager.utils

import java.text.SimpleDateFormat
import java.util.*

object SqlDumpGenerator {
    fun generateHeader(): String {
        val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return """
            -- 库存管理工具备份
            -- 备份时间: ${timeFormat.format(Date())}
            -- App版本: 1.0
            -- 数据库版本: 2

            BEGIN TRANSACTION;
        """.trimIndent()
    }

    fun generateFooter(maxDeviceId: Int, maxStaffId: Int, maxRecordId: Int): String {
        return """

            INSERT OR IGNORE INTO sqlite_sequence VALUES ('devices', $maxDeviceId);
            INSERT OR IGNORE INTO sqlite_sequence VALUES ('staff', $maxStaffId);
            INSERT OR IGNORE INTO sqlite_sequence VALUES ('stock_records', $maxRecordId);
            INSERT OR IGNORE INTO sqlite_sequence VALUES ('categories', 8);

            COMMIT;
        """.trimIndent()
    }

    fun escapeString(value: String?): String {
        if (value == null) return "NULL"
        return "'${value.replace("'", "''")}'"
    }

    fun escapeLong(value: Long?): String {
        return value?.toString() ?: "NULL"
    }

    fun escapeDouble(value: Double?): String {
        return value?.toString() ?: "NULL"
    }

    fun escapeInt(value: Int?): String {
        return value?.toString() ?: "NULL"
    }

    fun escapeBoolean(value: Boolean): String {
        return if (value) "1" else "0"
    }
}
