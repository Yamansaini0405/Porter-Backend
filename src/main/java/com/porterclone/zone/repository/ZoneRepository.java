package com.porterclone.zone.repository;

import com.porterclone.zone.entity.Zone;
import com.porterclone.zone.entity.ZoneStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ZoneRepository extends JpaRepository<Zone, Long> {
    Optional<Zone> findByCode(String code);
    boolean existsByCode(String code);
    boolean existsByCodeAndIdNot(String code, Long id);
    List<Zone> findAllByDeletedAtIsNullOrderByPriorityDescNameAsc();

    @Query("""
        select z from Zone z
        where z.deletedAt is null
          and z.status = :status
          and z.serviceable = true
        order by z.priority desc, z.id asc
        """)
    List<Zone> findServiceableZones(@Param("status") ZoneStatus status);
}
