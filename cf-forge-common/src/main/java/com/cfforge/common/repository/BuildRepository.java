package com.cfforge.common.repository;

import com.cfforge.common.entity.Build;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BuildRepository extends JpaRepository<Build, UUID> {
    List<Build> findByProjectIdOrderByCreatedAtDesc(UUID projectId);
}
