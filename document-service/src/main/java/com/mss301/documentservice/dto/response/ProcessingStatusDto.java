package com.mss301.documentservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessingStatusDto {
    private DocumentResponseDto.ProcessingJobDto job;
    private boolean hasJob;

    public static ProcessingStatusDto withJob(DocumentResponseDto.ProcessingJobDto job) {
        return ProcessingStatusDto.builder().job(job).hasJob(true).build();
    }

    public static ProcessingStatusDto noJob() {
        return ProcessingStatusDto.builder().hasJob(false).build();
    }
}
