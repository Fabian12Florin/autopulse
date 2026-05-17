package com.autopulse.userservice.model.entity;

import com.autopulse.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import com.autopulse.userservice.model.entity.User;

@Entity
@Table(name = "dispatcher_profiles")
@Getter
@Setter
public class DispatcherProfile extends BaseEntity {

    @Valid
    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @NotBlank
    @Size(min = 2, max = 20)
    @Column(name = "region_code", nullable = false, length = 20)
    private String regionCode;
}
