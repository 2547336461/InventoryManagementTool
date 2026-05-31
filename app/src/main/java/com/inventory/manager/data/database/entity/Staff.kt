package com.inventory.manager.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "staff")
data class Staff(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val department: String = "",
    val phone: String = "",
    val notes: String = "",
    val isActive: Boolean = true
)
