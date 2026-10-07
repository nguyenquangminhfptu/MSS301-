package com.fudn.bookingservice;

import com.fudn.bookingservice.client.MovieClient;
import com.fudn.bookingservice.dto.*;
import com.fudn.bookingservice.exception.ApiException;
import com.fudn.bookingservice.model.*;
import com.fudn.bookingservice.repository.*;
import com.fudn.bookingservice.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BookingRulesTests {
    BookingRepository bookings=mock(BookingRepository.class);
    BookingDetailRepository details=mock(BookingDetailRepository.class);
    MovieClient movies=mock(MovieClient.class);
    JdbcTemplate jdbc=mock(JdbcTemplate.class);
    BookingService service=new BookingService(bookings,details,movies,jdbc);
    ShowtimeResponse show;
    @BeforeEach void setUp() {
        show=new ShowtimeResponse("66f300000000000000000001","66f200000000000000000001","Test Movie","66f100000000000000000001","Room 01",5,8,LocalDateTime.now().plusDays(7),LocalDateTime.now().plusDays(7).plusHours(2),new BigDecimal("95000"),"SCHEDULED");
    }
    CreateBookingRequest request(String... seats) {
        return new CreateBookingRequest(Arrays.stream(seats).map(s->new BookingItemRequest(show.showtimeId(),s)).toList());
    }
    Booking ownedBooking(LocalDateTime start) {
        Booking b=new Booking();b.setBookingId(10L);b.setCustomerId(1L);b.setBookingDate(LocalDateTime.now());b.setBookingStatus(BookingStatus.CONFIRMED);b.setTotalPrice(new BigDecimal("95000"));
        BookingDetail d=new BookingDetail();d.setShowtimeStart(start);d.setPrice(new BigDecimal("95000"));d.setMovieId("movie1");d.setMovieTitle("Movie");b.addDetail(d);
        when(bookings.findById(10L)).thenReturn(Optional.of(b));return b;
    }
    void error(int status,org.junit.jupiter.api.function.Executable action) {
        assertEquals(status,assertThrows(ApiException.class,action).getStatus().value());
    }
    @Test void computesPriceAndFetchesEachShowtimeOnce() {
        when(movies.getShowtime(show.showtimeId())).thenReturn(show);
        when(bookings.save(any())).thenAnswer(x->{Booking b=x.getArgument(0);b.setBookingId(1L);return b;});
        var result=service.create(1L,request("E5","E6"));
        assertEquals(0,new BigDecimal("190000").compareTo(result.totalPrice()));
        assertEquals("Test Movie",result.details().getFirst().movieTitle());
        verify(movies,times(1)).getShowtime(show.showtimeId());
    }
    @Test void rejectsDuplicateSeatInOneRequest() {
        when(movies.getShowtime(show.showtimeId())).thenReturn(show);
        error(400,()->service.create(1L,request("A1","A1")));verify(bookings,never()).save(any());
    }
    @Test void rejectsSeatOutsideRoomLayout() {
        when(movies.getShowtime(show.showtimeId())).thenReturn(show);
        error(400,()->service.create(1L,request("F1")));error(400,()->service.create(1L,request("E9")));
    }
    @Test void rejectsAlreadyBookedSeat() {
        when(movies.getShowtime(show.showtimeId())).thenReturn(show);
        when(details.findSeatCodesByShowtime(show.showtimeId(),BookingStatus.CONFIRMED)).thenReturn(List.of("A1"));
        error(409,()->service.create(1L,request("A1")));
    }
    @Test void rejectsCancelledShowtime() {
        when(movies.getShowtime(show.showtimeId())).thenReturn(new ShowtimeResponse(show.showtimeId(),show.movieId(),show.movieTitle(),show.roomId(),show.roomName(),5,8,show.startTime(),show.endTime(),show.ticketPrice(),"CANCELLED"));
        error(400,()->service.create(1L,request("A1")));
    }
    @Test void deniesOtherCustomersDetailsAndCancellation() {
        ownedBooking(LocalDateTime.now().plusDays(1));
        error(403,()->service.getById(10L,2L,"CUSTOMER"));error(403,()->service.cancel(10L,2L,"CUSTOMER"));
    }
    @Test void customerCannotCancelInsideTwoHours() {
        ownedBooking(LocalDateTime.now().plusMinutes(119));error(400,()->service.cancel(10L,1L,"CUSTOMER"));
        verifyNoInteractions(jdbc);
    }
    @Test void adminCanCancelAfterShowtimeAndReleaseSeats() {
        var b=ownedBooking(LocalDateTime.now().minusHours(1));when(bookings.save(b)).thenReturn(b);
        assertEquals(BookingStatus.CANCELLED,service.cancel(10L,0L,"ADMIN").bookingStatus());
        verify(jdbc).update("DELETE FROM seat_reservation WHERE booking_id = ?",10L);
    }
    @Test void cannotCancelTwice() {
        var b=ownedBooking(LocalDateTime.now().plusDays(1));b.setBookingStatus(BookingStatus.CANCELLED);
        error(400,()->service.cancel(10L,1L,"CUSTOMER"));
    }
    @Test void rejectsReversedReportDates() {
        error(400,()->service.report(LocalDate.of(2026,10,8),LocalDate.of(2026,10,7)));
    }
    @Test void reportUsesExclusiveNextDayAndServerTotals() {
        LocalDate day=LocalDate.of(2026,10,7);
        var b=ownedBooking(day.plusDays(1).atTime(19,0));
        when(bookings.findForReport(BookingStatus.CONFIRMED,day.atStartOfDay(),day.plusDays(1).atStartOfDay())).thenReturn(List.of(b));
        var result=service.report(day,day);assertEquals(1,result.totalBookings());assertEquals(1,result.totalTickets());assertEquals(0,result.totalRevenue().compareTo(new BigDecimal("95000")));
    }
}
