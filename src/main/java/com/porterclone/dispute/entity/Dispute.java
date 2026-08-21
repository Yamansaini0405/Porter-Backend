package com.porterclone.dispute.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "disputes")
@Getter
@Setter
@NoArgsConstructor
public class Dispute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trip_id", nullable = false)
    private Long tripId;

    @Column(name = "raised_by", nullable = false, length = 20)
    private String raisedBy;

    private String category;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DisputeStatus status = DisputeStatus.OPEN;

    @Column(name = "resolved_by_admin_id")
    private Long resolvedByAdminId;

    @Column(name = "resolution_notes", length = 1000)
    private String resolutionNotes;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
}
