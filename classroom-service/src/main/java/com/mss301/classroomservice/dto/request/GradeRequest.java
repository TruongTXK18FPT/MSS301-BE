package com.mss301.classroomservice.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradeRequest {

    @NotNull(message = "Points cannot be null")
    @Min(value = 0, message = "Points must be greater than or equal to 0")
    private Integer points;

    private String feedback;
}
