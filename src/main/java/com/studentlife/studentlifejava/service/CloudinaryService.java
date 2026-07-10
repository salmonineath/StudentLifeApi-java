package com.studentlife.studentlifejava.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import static com.studentlife.studentlifejava.exception.ErrorsExceptionFactory.badRequest;
import static com.studentlife.studentlifejava.exception.ErrorsExceptionFactory.internal;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;

    @SuppressWarnings("unchecked")
    public CloudinaryUploadResult upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw badRequest("File must not be empty.");
        }
        try {
            var result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "resource_type", "auto"
            ));
            return new CloudinaryUploadResult(
                    (String) result.get("secure_url"),
                    (String) result.get("public_id"),
                    (String) result.get("resource_type"));
        } catch (Exception e) {
            log.error("Cloudinary upload failed", e);
            throw internal("Failed to upload file.");
        }
    }

    public void delete(String publicId, String resourceType) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", resourceType));
        } catch (Exception e) {
            log.warn("Cloudinary delete failed for publicId={}, continuing anyway", publicId, e);
        }
    }
}
