package com.autopulse.geographyservice.model.entity;

import com.autopulse.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "regions",
        indexes = {
                @Index(name = "idx_regions_name", columnList = "name")
        }
)
@Getter
@Setter
public class Region extends BaseEntity {

    @NotBlank
    @Size(min = 2, max = 100)
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @NotBlank
    @Size(min = 2, max = 20)
    @Column(name = "code", nullable = false, unique = true, length = 20)
    private String code;

    @Valid
    @OneToMany(mappedBy = "region")
    private List<Locality> localities = new ArrayList<>();
}