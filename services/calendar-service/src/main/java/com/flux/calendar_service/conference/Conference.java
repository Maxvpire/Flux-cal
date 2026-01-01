package com.flux.calendar_service.conference;

import com.flux.calendar_service.event.Event;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "conferences")
@EntityListeners(AuditingEntityListener.class)
public class Conference {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private ConferenceType type;

    // Google Meet specific fields
    @Column(name = "google_conference_id")
    private String googleConferenceId;  // e.g., "abc123xyz"

    @Column(name = "meet_link", length = 1000)
    private String meetLink;  // https://meet.google.com/abc-defg-hij

    @Column(name = "meeting_code")
    private String meetingCode;  // abc-defg-hij

    @Column(name = "phone_number")
    private String phoneNumber;  // Dial-in number

    @Column(name = "pin")
    private String pin;  // PIN for phone dial-in

    // For other platforms (Zoom, Teams, etc.)
    @Column(name = "conference_link", length = 1000)
    private String conferenceLink;

    @Column(name = "conference_password")
    private String conferencePassword;

    @Column(name = "platform_name")
    private String platformName;

    // Sync tracking
    @Column(name = "sync_status")
    @Enumerated(EnumType.STRING)
    private SyncStatus syncStatus;

    @Column(name = "last_synced")
    private LocalDateTime lastSynced;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Relationship with Event (One-to-One)
    @OneToOne(mappedBy = "conference")
    private Event event;

    public enum ConferenceType {
        GOOGLE_MEET,
        ZOOM,
    }

    public enum SyncStatus {
        SYNCED,
        PENDING_UPLOAD,
        PENDING_UPDATE,
        PENDING_DELETE
    }
}