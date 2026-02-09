package com.cfforge.common.repository;

import com.cfforge.common.entity.CfTarget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CfTargetRepository extends JpaRepository<CfTarget, UUID> {
    List<CfTarget> findByUserIdOrderByCreatedAtDesc(UUID userId);
    Optional<CfTarget> findByUserIdAndIsDefaultTrue(UUID userId);
}
