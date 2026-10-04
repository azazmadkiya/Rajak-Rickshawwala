package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BookingDao {
  @Query("SELECT * FROM bookings ORDER BY timestamp DESC")
  fun getAllBookings(): Flow<List<BookingEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertBooking(booking: BookingEntity): Long

  @Query("DELETE FROM bookings WHERE id = :id")
  suspend fun deleteBooking(id: Long)
}
