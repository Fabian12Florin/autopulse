package com.autopulse.userservice.repository.specification;

import com.autopulse.userservice.model.entity.CourierProfile;
import com.autopulse.userservice.model.enums.AvailabilityStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class CourierProfileSpecifications {

    private CourierProfileSpecifications() {
    }

    public static Specification<CourierProfile> withFilters(UUID depotId,
                                                            String regionCode,
                                                            AvailabilityStatus availabilityStatus,
                                                            Boolean active) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (depotId != null) {
                predicates.add(cb.equal(root.get("depotId"), depotId));
            }
            if (regionCode != null && !regionCode.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("regionCode")), regionCode.trim().toLowerCase()));
            }
            if (availabilityStatus != null) {
                predicates.add(cb.equal(root.get("availabilityStatus"), availabilityStatus));
            }
            if (active != null) {
                var userJoin = root.join("user");
                predicates.add(cb.equal(userJoin.get("active"), active));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    public static Specification<CourierProfile> availableForDepot(UUID depotId, String regionCode) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("depotId"), depotId));
            predicates.add(cb.equal(root.get("availabilityStatus"), AvailabilityStatus.AVAILABLE));
            var userJoin = root.join("user");
            predicates.add(cb.isTrue(userJoin.get("active")));

            if (regionCode != null && !regionCode.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("regionCode")), regionCode.trim().toLowerCase()));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
