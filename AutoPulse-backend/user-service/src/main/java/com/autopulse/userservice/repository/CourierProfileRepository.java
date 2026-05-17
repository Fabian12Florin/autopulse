package com.autopulse.userservice.repository;

import com.autopulse.userservice.model.entity.CourierProfile;
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
public interface CourierProfileRepository extends JpaRepository<CourierProfile, UUID>, JpaSpecificationExecutor<CourierProfile> {

    @Override
    @EntityGraph(attributePaths = "user")
    Optional<CourierProfile> findById(UUID id);

    @Override
    @EntityGraph(attributePaths = "user")
    Page<CourierProfile> findAll(Specification<CourierProfile> specification, Pageable pageable);

    @EntityGraph(attributePaths = "user")
    Optional<CourierProfile> findByUserId(UUID userId);

    @EntityGraph(attributePaths = "user")
    Optional<CourierProfile> findByUserKeycloakUserId(UUID keycloakUserId);

    boolean existsByUserId(UUID userId);
}
