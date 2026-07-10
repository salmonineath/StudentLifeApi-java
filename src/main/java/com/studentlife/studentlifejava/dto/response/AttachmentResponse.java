package com.studentlife.studentlifejava.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentResponse {

    private Long id;
    private Long taskId;
    private String name;

    @JsonProperty("size")
    private Long sizeBytes;

    private String url;
    private Instant uploadedAt;
}
