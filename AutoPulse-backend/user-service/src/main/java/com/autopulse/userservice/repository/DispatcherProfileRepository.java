package com.autopulse.userservice.repository;

import com.autopulse.userservice.model.entity.DispatcherProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DispatcherProfileRepository extends JpaRepository<DispatcherProfile, UUID>, JpaSpecificationExecutor<DispatcherProfile> {

    @Override
    @EntityGraph(attributePaths = "user")
    Optional<DispatcherProfile> findById(UUID id);

    @Override
    @EntityGraph(attributePaths = "user")
    Page<DispatcherProfile> findAll(Specification<DispatcherProfile> specification, Pageable pageable);

    @EntityGraph(attributePaths = "user")
    Optional<DispatcherProfile> findByUserId(UUID userId);

    @EntityGraph(attributePaths = "user")
    Optional<DispatcherProfile> findByUserKeycloakUserId(UUID keycloakUserId);

    boolean existsByUserId(UUID userId);
}
