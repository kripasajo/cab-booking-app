package com.rideflow.booking.repository;

import com.rideflow.booking.model.Booking;
import com.rideflow.booking.model.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findTop50ByRiderIdOrderByCreatedAtDesc(String riderId);

    List<Booking> findTop50ByDriverIdOrderByCreatedAtDesc(String driverId);

    Optional<Booking> findFirstByRiderIdAndStatusInOrderByCreatedAtDesc(String riderId, Collection<BookingStatus> statuses);

    Optional<Booking> findFirstByDriverIdAndStatusInOrderByCreatedAtDesc(String driverId, Collection<BookingStatus> statuses);
}
