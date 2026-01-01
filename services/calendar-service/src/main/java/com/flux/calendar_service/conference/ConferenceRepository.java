package com.flux.calendar_service.conference;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConferenceRepository extends JpaRepository<Conference, String> {
    Optional<Conference> findByGoogleConferenceId(String googleConferenceId);
    List<Conference> findBySyncStatus(Conference.SyncStatus syncStatus);
    List<Conference> findByType(Conference.ConferenceType type);
}
