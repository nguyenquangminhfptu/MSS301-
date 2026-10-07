-- Only confirmed bookings own reservations. Cancellation releases them atomically.
CREATE TABLE seat_reservation (
    showtime_id VARCHAR(24) NOT NULL,
    seat_code VARCHAR(5) NOT NULL,
    booking_id BIGINT NOT NULL,
    PRIMARY KEY (showtime_id, seat_code),
    CONSTRAINT fk_reservation_booking FOREIGN KEY (booking_id) REFERENCES booking(booking_id),
    INDEX idx_reservation_booking (booking_id)
);

INSERT INTO seat_reservation (showtime_id, seat_code, booking_id)
SELECT d.showtime_id, d.seat_code, d.booking_id
FROM booking_detail d JOIN booking b ON b.booking_id = d.booking_id
WHERE b.booking_status = 'CONFIRMED';
