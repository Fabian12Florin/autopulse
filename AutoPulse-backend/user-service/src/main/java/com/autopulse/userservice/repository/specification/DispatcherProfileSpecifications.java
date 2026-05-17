package com.autopulse.userservice.repository.specification;

import com.autopulse.userservice.model.entity.DispatcherProfile;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;

import java.util.ArrayList;
import java.util.List;

public final class DispatcherProfileSpecifications {

    private DispatcherProfileSpecifications() {
    }

    public static Specification<DispatcherProfile> withFilters(String regionCode,
                                                               Boolean active) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (regionCode != null && !regionCode.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("regionCode")), regionCode.trim().toLowerCase()));
            }
            if (active != null) {
                var userJoin = root.join("user");
                predicates.add(cb.equal(userJoin.get("active"), active));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
