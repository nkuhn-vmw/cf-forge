package com.cfforge.common.repository;

import com.cfforge.common.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {
    List<Conversation> findByProjectIdOrderByCreatedAtDesc(UUID projectId);
    List<Conversation> findByUserIdOrderByUpdatedAtDesc(UUID userId);
}
