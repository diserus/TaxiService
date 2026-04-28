package com.taxi.user.mapper;

import com.taxi.user.dto.DriverResponse;
import com.taxi.user.dto.DriverStatus;
import com.taxi.user.dto.PassengerResponse;
import com.taxi.user.entity.Driver;
import com.taxi.user.entity.Passenger;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    PassengerResponse toResponse(Passenger passenger);

    @Mapping(target = "status", source = "status")
    DriverResponse toResponse(Driver driver);

    default DriverStatus map(com.taxi.user.entity.DriverStatus status) {
        return status == null ? null : DriverStatus.valueOf(status.name());
    }

    default com.taxi.user.entity.DriverStatus map(DriverStatus status) {
        return status == null ? null : com.taxi.user.entity.DriverStatus.valueOf(status.name());
    }
}
