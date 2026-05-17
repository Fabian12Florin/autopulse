package com.autopulse.userservice.repository;

import com.autopulse.userservice.model.entity.CourierProfile;
import com.autopulse.userservice.model.entity.DispatcherProfile;
import com.autopulse.userservice.model.entity.User;
import com.autopulse.userservice.model.enums.AvailabilityStatus;
import com.autopulse.userservice.repository.specification.CourierProfileSpecifications;
import com.autopulse.userservice.repository.specification.DispatcherProfileSpecifications;
import com.autopulse.userservice.repository.specification.UserSpecifications;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryIntegrationTest {

    private final AtomicInteger phoneSequence = new AtomicInteger(100000);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DispatcherProfileRepository dispatcherProfileRepository;

    @Autowired
    private CourierProfileRepository courierProfileRepository;

    @Test
    void persistsUsersAndFindsByNaturalKeys() {
        User user = saveUser("jane@example.com", "Jane", "Driver", "+40722123456", true);

        assertThat(userRepository.findByKeycloakUserId(user.getKeycloakUserId())).contains(user);
        assertThat(userRepository.findByEmailIgnoreCase("JANE@example.com")).contains(user);
        assertThat(userRepository.existsByEmailIgnoreCase("jane@example.com")).isTrue();
        assertThat(userRepository.existsByEmailIgnoreCaseAndIdNot("jane@example.com", UUID.randomUUID())).isTrue();
        assertThat(userRepository.existsByEmailIgnoreCaseAndIdNot("jane@example.com", user.getId())).isFalse();
    }

    @Test
    void userSpecificationAppliesEveryOptionalFilterAndEmptyFilter() {
        saveUser("jane.driver@example.com", "Jane", "Driver", "+40722123456", true);
        saveUser("john.manager@example.com", "John", "Manager", "+40722123457", false);

        assertThat(userRepository.findAll(UserSpecifications.withFilters("driver", "ja", "dri", "722123456", true), PageRequest.of(0, 10)))
                .hasSize(1)
                .first()
                .extracting(User::getEmail)
                .isEqualTo("jane.driver@example.com");

        assertThat(userRepository.findAll(UserSpecifications.withFilters(" ", null, "", null, null), PageRequest.of(0, 10)))
                .hasSize(2);
    }

    @Test
    void dispatcherRepositoryAndSpecificationUseUserJoinForActiveFilter() {
        DispatcherProfile active = saveDispatcher("dispatcher@example.com", "RO-B", true);
        saveDispatcher("inactive-dispatcher@example.com", "RO-C", false);

        assertThat(dispatcherProfileRepository.findById(active.getId())).contains(active);
        assertThat(dispatcherProfileRepository.findByUserId(active.getUser().getId())).contains(active);
        assertThat(dispatcherProfileRepository.findByUserKeycloakUserId(active.getUser().getKeycloakUserId())).contains(active);
        assertThat(dispatcherProfileRepository.existsByUserId(active.getUser().getId())).isTrue();
        assertThat(dispatcherProfileRepository.findAll(DispatcherProfileSpecifications.withFilters("ro-b", true), PageRequest.of(0, 10)))
                .hasSize(1);
        assertThat(dispatcherProfileRepository.findAll(DispatcherProfileSpecifications.withFilters(null, null), PageRequest.of(0, 10)))
                .hasSize(2);
    }

    @Test
    void courierRepositoryAndSpecificationsCoverOperationalFilters() {
        UUID depot = UUID.randomUUID();
        UUID otherDepot = UUID.randomUUID();
        CourierProfile available = saveCourier("available@example.com", depot, "RO-B", AvailabilityStatus.AVAILABLE, true);
        saveCourier("unavailable@example.com", depot, "RO-B", AvailabilityStatus.OFF_DUTY, true);
        saveCourier("inactive@example.com", depot, "RO-B", AvailabilityStatus.AVAILABLE, false);
        saveCourier("other-region@example.com", depot, "RO-C", AvailabilityStatus.AVAILABLE, true);
        saveCourier("other-depot@example.com", otherDepot, "RO-B", AvailabilityStatus.AVAILABLE, true);

        assertThat(courierProfileRepository.findById(available.getId())).contains(available);
        assertThat(courierProfileRepository.findByUserId(available.getUser().getId())).contains(available);
        assertThat(courierProfileRepository.findByUserKeycloakUserId(available.getUser().getKeycloakUserId())).contains(available);
        assertThat(courierProfileRepository.existsByUserId(available.getUser().getId())).isTrue();

        assertThat(courierProfileRepository.findAll(
                CourierProfileSpecifications.withFilters(depot, "ro-b", AvailabilityStatus.AVAILABLE, true),
                PageRequest.of(0, 10)
        )).hasSize(1);

        assertThat(courierProfileRepository.findAll(
                CourierProfileSpecifications.availableForDepot(depot, "ro-b"),
                PageRequest.of(0, 10)
        )).hasSize(1);

        assertThat(courierProfileRepository.findAll(
                CourierProfileSpecifications.withFilters(null, null, null, null),
                PageRequest.of(0, 10)
        )).hasSize(5);
    }

    private DispatcherProfile saveDispatcher(String email, String regionCode, boolean active) {
        User user = saveUser(email, "Dispatch", "Person", phone(), active);
        DispatcherProfile profile = new DispatcherProfile();
        profile.setUser(user);
        profile.setRegionCode(regionCode);
        return dispatcherProfileRepository.saveAndFlush(profile);
    }

    private CourierProfile saveCourier(String email, UUID depotId, String regionCode, AvailabilityStatus status, boolean active) {
        User user = saveUser(email, "Courier", "Person", phone(), active);
        CourierProfile profile = new CourierProfile();
        profile.setUser(user);
        profile.setDepotId(depotId);
        profile.setRegionCode(regionCode);
        profile.setAvailabilityStatus(status);
        return courierProfileRepository.saveAndFlush(profile);
    }

    private User saveUser(String email, String firstName, String lastName, String phoneNumber, boolean active) {
        User user = new User();
        user.setKeycloakUserId(UUID.randomUUID());
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPhoneNumber(phoneNumber);
        user.setActive(active);
        return userRepository.saveAndFlush(user);
    }

    private String phone() {
        return "+4072" + phoneSequence.getAndIncrement();
    }
}
