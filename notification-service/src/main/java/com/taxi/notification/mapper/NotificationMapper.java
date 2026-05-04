package com.taxi.notification.mapper;

import com.taxi.notification.dto.NotificationResponse;
import com.taxi.notification.entity.NotificationTask;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    NotificationResponse toResponse(NotificationTask entity);

    default com.taxi.notification.dto.NotificationStatus mapStatus(com.taxi.notification.entity.NotificationStatus s) {
        return s == null ? null : com.taxi.notification.dto.NotificationStatus.valueOf(s.name());
    }

    default com.taxi.notification.entity.NotificationStatus mapStatus(com.taxi.notification.dto.NotificationStatus s) {
        return s == null ? null : com.taxi.notification.entity.NotificationStatus.valueOf(s.name());
    }

    default com.taxi.notification.dto.RecipientType mapRecipient(com.taxi.notification.entity.RecipientType r) {
        return r == null ? null : com.taxi.notification.dto.RecipientType.valueOf(r.name());
    }

    default com.taxi.notification.entity.RecipientType mapRecipient(com.taxi.notification.dto.RecipientType r) {
        return r == null ? null : com.taxi.notification.entity.RecipientType.valueOf(r.name());
    }
}
