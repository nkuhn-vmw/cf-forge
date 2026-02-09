package com.cfforge.common.repository;

import com.cfforge.common.entity.Deployment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DeploymentRepository extends JpaRepository<Deployment, UUID> {
    List<Deployment> findByProjectIdOrderByCreatedAtDesc(UUID projectId);
}
