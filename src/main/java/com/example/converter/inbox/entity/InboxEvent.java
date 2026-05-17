package com.example.converter.inbox.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "inbox_events")
@Getter
@Setter
@NoArgsConstructor
public class InboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_id", nullable = false, unique = true)
    private String eventId;

    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InboxEventStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    public static InboxEvent pending(String eventId, String payload) {
        InboxEvent event = new InboxEvent();
        event.eventId = eventId;
        event.payload = payload;
        event.status = InboxEventStatus.PENDING;
        event.createdAt = LocalDateTime.now();
        return event;
    }
}
