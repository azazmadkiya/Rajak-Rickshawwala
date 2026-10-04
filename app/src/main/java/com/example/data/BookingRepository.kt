package com.example.data

import kotlinx.coroutines.flow.Flow

class BookingRepository(private val bookingDao: BookingDao) {
  val allBookings: Flow<List<BookingEntity>> = bookingDao.getAllBookings()

  suspend fun insertBooking(booking: BookingEntity): Long {
    return bookingDao.insertBooking(booking)
  }

  suspend fun deleteBooking(id: Long) {
    bookingDao.deleteBooking(id)
  }
}
