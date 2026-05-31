package com.inventory.manager.data.database

import androidx.room.TypeConverter
import com.inventory.manager.data.database.entity.DeviceCondition
import com.inventory.manager.data.database.entity.DeviceStatus
import com.inventory.manager.data.database.entity.RecordType

class Converters {
    @TypeConverter fun fromDeviceStatus(v: DeviceStatus): String = v.name
    @TypeConverter fun toDeviceStatus(v: String): DeviceStatus = DeviceStatus.valueOf(v)

    @TypeConverter fun fromRecordType(v: RecordType): String = v.name
    @TypeConverter fun toRecordType(v: String): RecordType = RecordType.valueOf(v)

    @TypeConverter fun fromDeviceCondition(v: DeviceCondition?): String? = v?.name
    @TypeConverter fun toDeviceCondition(v: String?): DeviceCondition? = v?.let { DeviceCondition.valueOf(it) }
}
