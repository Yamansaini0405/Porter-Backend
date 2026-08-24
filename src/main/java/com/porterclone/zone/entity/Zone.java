package com.porterclone.zone.entity;

import com.porterclone.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "zones",
        uniqueConstraints = @UniqueConstraint(name = "uk_zone_code", columnNames = "code"),
        indexes = {
                @Index(name = "idx_zone_status_serviceable", columnList = "status,is_serviceable"),
                @Index(name = "idx_zone_priority", columnList = "priority")
        })
@Getter @Setter @NoArgsConstructor
public class Zone extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 120) private String name;
    @Column(nullable = false, length = 50) private String code;
    @Column(length = 1000) private String description;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private ZoneStatus status = ZoneStatus.ACTIVE;
    @Column(name = "is_serviceable", nullable = false) private boolean serviceable = true;
    @Column(nullable = false) private Integer priority = 0;
    @Enumerated(EnumType.STRING) @Column(name = "boundary_type", nullable = false, length = 20)
    private BoundaryType boundaryType = BoundaryType.POLYGON;
    @Column(name = "boundary_coordinates", nullable = false, columnDefinition = "JSON")
    private String boundaryCoordinates;
    @Column(name = "deleted_at") private java.time.LocalDateTime deletedAt;
}
