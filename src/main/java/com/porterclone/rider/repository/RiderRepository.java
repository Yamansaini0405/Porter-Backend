package com.porterclone.rider.repository;

import com.porterclone.rider.entity.Rider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RiderRepository extends JpaRepository<Rider, Long> {
    Optional<Rider> findByUserId(Long userId);
}
