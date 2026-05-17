package com.autopulse.geographyservice.model.entity;

import com.autopulse.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "localities",
        indexes = {
                @Index(name = "idx_localities_name", columnList = "name"),
                @Index(name = "idx_localities_region_id", columnList = "region_id")
        }
)
@Getter
@Setter
public class Locality extends BaseEntity {

    @NotBlank
    @Size(min = 2, max = 120)
    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @NotBlank
    @Size(min = 2, max = 20)
    @Column(name = "code", nullable = false, unique = true, length = 20)
    private String code;

    @Valid
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;
}