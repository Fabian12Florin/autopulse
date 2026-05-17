package com.autopulse.userservice.repository.specification;

import com.autopulse.userservice.model.entity.User;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;

import java.util.ArrayList;
import java.util.List;

public final class UserSpecifications {

    private UserSpecifications() {
    }

    public static Specification<User> withFilters(String email,
                                                  String firstName,
                                                  String lastName,
                                                  String phoneNumber,
                                                  Boolean active) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (email != null && !email.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("email")), contains(email)));
            }
            if (firstName != null && !firstName.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("firstName")), contains(firstName)));
            }
            if (lastName != null && !lastName.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("lastName")), contains(lastName)));
            }
            if (phoneNumber != null && !phoneNumber.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("phoneNumber")), contains(phoneNumber)));
            }
            if (active != null) {
                predicates.add(cb.equal(root.get("active"), active));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static String contains(String value) {
        return "%" + value.trim().toLowerCase() + "%";
    }
}
