package com.example.converter.inbox.repository;

import com.example.converter.inbox.entity.InboxEvent;
import com.example.converter.inbox.entity.InboxEventStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InboxEventRepository extends JpaRepository<InboxEvent, UUID> {

    boolean existsByEventId(String eventId);

    List<InboxEvent> findAllByStatus(InboxEventStatus status);
}
