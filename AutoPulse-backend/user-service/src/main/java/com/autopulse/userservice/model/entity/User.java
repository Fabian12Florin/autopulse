package com.autopulse.userservice.model.entity;

import com.autopulse.common.model.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User extends BaseEntity {

    @NotNull
    @Column(name = "keycloak_user_id", nullable = false, unique = true, updatable = false)
    private UUID keycloakUserId;

    @NotBlank
    @Email
    @Size(max = 255)
    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @NotBlank
    @Size(min = 2, max = 100)
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @NotBlank
    @Size(min = 2, max = 100)
    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @NotBlank
    @Pattern(regexp = "^\\+?[1-9]\\d{7,14}$", message = "Phone number must be in E.164-like format")
    @Size(max = 20)
    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @NotNull
    @Column(name = "active", nullable = false)
    private Boolean active = Boolean.TRUE;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private DispatcherProfile dispatcherProfile;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private CourierProfile courierProfile;
}
