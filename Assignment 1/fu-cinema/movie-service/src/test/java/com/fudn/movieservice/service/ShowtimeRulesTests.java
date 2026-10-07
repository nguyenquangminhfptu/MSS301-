package com.fudn.movieservice.service;

import com.fudn.movieservice.config.DataSeeder;
import com.fudn.movieservice.dto.ShowtimeRequest;
import com.fudn.movieservice.exception.ApiException;
import com.fudn.movieservice.model.*;
import com.fudn.movieservice.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ShowtimeRulesTests {
    ShowtimeRepository showtimes=mock(ShowtimeRepository.class);
    MovieRepository movies=mock(MovieRepository.class);
    RoomRepository rooms=mock(RoomRepository.class);
    MovieService movieService=mock(MovieService.class);
    RoomService roomService=mock(RoomService.class);
    ShowtimeService service=new ShowtimeService(showtimes,movies,rooms,movieService,roomService);
    Movie movie;CinemaRoom room;LocalDateTime start=LocalDateTime.now().plusDays(7);
    @BeforeEach void setUp() {
        movie=new Movie("66f200000000000000000001","Galaxy Rangers","","Director",125,"English",AgeRating.T13,LocalDate.of(2026,9,20),"66f000000000000000000005",MovieStatus.NOW_SHOWING);
        room=new CinemaRoom("66f100000000000000000001","Room 01",RoomType.STANDARD,8,10,RoomStatus.ACTIVE);
        when(movieService.find(movie.getMovieId())).thenReturn(movie);when(roomService.find(room.getRoomId())).thenReturn(room);
    }
    ShowtimeRequest request() {return new ShowtimeRequest(movie.getMovieId(),room.getRoomId(),start,new BigDecimal("95000"));}
    void error(int status,org.junit.jupiter.api.function.Executable action) {assertEquals(status,assertThrows(ApiException.class,action).getStatus().value());}
    @Test void computesEndAndQueriesStrictOverlapBoundaries() {
        when(showtimes.save(any())).thenAnswer(x->x.getArgument(0));
        var result=service.create(request());assertEquals(start.plusMinutes(125),result.endTime());
        verify(showtimes).countByRoomIdAndShowtimeStatusAndStartTimeLessThanAndEndTimeGreaterThanAndShowtimeIdNot(room.getRoomId(),ShowtimeStatus.SCHEDULED,start.plusMinutes(125),start,"");
    }
    @Test void refusesOverlappingScheduledShowtime() {
        when(showtimes.countByRoomIdAndShowtimeStatusAndStartTimeLessThanAndEndTimeGreaterThanAndShowtimeIdNot(anyString(),any(),any(),any(),anyString())).thenReturn(1L);
        error(409,()->service.create(request()));verify(showtimes,never()).save(any());
    }
    @Test void refusesEndedMovie() {movie.setMovieStatus(MovieStatus.ENDED);error(400,()->service.create(request()));}
    @Test void refusesMaintenanceRoom() {room.setRoomStatus(RoomStatus.MAINTENANCE);error(400,()->service.create(request()));}
    @Test void refusesPastStart() {start=LocalDateTime.now().minusMinutes(1);error(400,()->service.create(request()));}
    @Test void refusesMissingMovieReference() {
        when(movieService.find(movie.getMovieId())).thenThrow(ApiException.notFound("Movie missing"));error(404,()->service.create(request()));
    }
    @Test void seederRepairsEmptyCollectionsWithoutDuplicatingPopulatedOnes() {
        GenreRepository genres=mock(GenreRepository.class);when(genres.count()).thenReturn(5L);
        new DataSeeder(genres,rooms,movies,showtimes).run();
        verify(genres,never()).saveAll(anyList());verify(rooms).saveAll(anyList());verify(movies).saveAll(anyList());verify(showtimes).saveAll(anyList());
    }
    @Test void seederSkipsAllPopulatedCollections() {
        GenreRepository genres=mock(GenreRepository.class);when(genres.count()).thenReturn(5L);when(rooms.count()).thenReturn(4L);when(movies.count()).thenReturn(4L);when(showtimes.count()).thenReturn(5L);
        new DataSeeder(genres,rooms,movies,showtimes).run();
        verify(genres,never()).saveAll(anyList());verify(rooms,never()).saveAll(anyList());verify(movies,never()).saveAll(anyList());verify(showtimes,never()).saveAll(anyList());
    }
}
