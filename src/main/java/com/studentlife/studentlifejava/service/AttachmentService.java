package com.studentlife.studentlifejava.service;

import com.studentlife.studentlifejava.dto.response.AttachmentResponse;
import com.studentlife.studentlifejava.entity.Users;
import org.springframework.web.multipart.MultipartFile;

public interface AttachmentService {

    AttachmentResponse upload(Long taskId, MultipartFile file, Users currentUser);

    void delete(Long attachmentId, Users currentUser);
}
