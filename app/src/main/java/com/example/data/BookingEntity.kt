package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookings")
data class BookingEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  val pickup: String,
  val drop: String,
  val bhadaAmount: String,
  val notes: String,
  val date: String,
  val status: String = "Confirmed",
  val timestamp: Long = System.currentTimeMillis()
)
