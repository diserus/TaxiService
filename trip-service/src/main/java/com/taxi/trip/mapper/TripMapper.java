package com.taxi.trip.mapper;

import com.taxi.trip.dto.TripResponse;
import com.taxi.trip.dto.TripStatus;
import com.taxi.trip.entity.Trip;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TripMapper {

    TripResponse toResponse(Trip trip);

    default TripStatus map(com.taxi.trip.entity.TripStatus s) {
        return s == null ? null : TripStatus.valueOf(s.name());
    }

    default com.taxi.trip.entity.TripStatus map(TripStatus s) {
        return s == null ? null : com.taxi.trip.entity.TripStatus.valueOf(s.name());
    }
}
