package com.studentlife.studentlifejava.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChecklistItemResponse {

    private Long id;
    private Long taskId;
    private String text;
    private Boolean done;
}
