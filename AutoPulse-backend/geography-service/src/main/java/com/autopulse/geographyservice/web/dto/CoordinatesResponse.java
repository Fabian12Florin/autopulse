package com.autopulse.geographyservice.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resolved geographic coordinates. X is longitude and Y is latitude.")
public record CoordinatesResponse(
        @Schema(description = "X coordinate. Equivalent to longitude.", example = "21.2306537")
        Double x,

        @Schema(description = "Y coordinate. Equivalent to latitude.", example = "45.7754583")
        Double y
) {
}
