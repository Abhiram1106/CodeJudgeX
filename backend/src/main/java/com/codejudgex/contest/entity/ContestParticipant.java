package com.codejudgex.contest.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "contest_participants")
@Getter
@Setter
@NoArgsConstructor
public class ContestParticipant {

    @EmbeddedId
    private ContestParticipantId id = new ContestParticipantId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("contestId")
    @JoinColumn(name = "contest_id")
    private Contest contest;

    @Column(name = "user_id", insertable = false, updatable = false)
    private UUID userId;

    @Column(name = "registered_at")
    private Instant registeredAt;

    @PrePersist
    protected void onCreate() {
        registeredAt = Instant.now();
    }

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    public static class ContestParticipantId implements Serializable {
        @Column(name = "contest_id")
        private UUID contestId;

        @Column(name = "user_id")
        private UUID userId;
    }
}
