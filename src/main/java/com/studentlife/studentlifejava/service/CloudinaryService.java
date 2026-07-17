package com.studentlife.studentlifejava.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.studentlife.studentlifejava.entity.Attachment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collection;
import java.util.List;

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
            // "auto" lets Cloudinary classify image/video/raw itself. Whatever it
            // picks is stored on the Attachment and must be passed back unchanged
            // to delete() below - a mismatched resource_type makes the delete
            // call silently fail (see delete()'s comment).
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
            // Deliberately swallowed: callers (attachment delete, and the upload
            // rollback path) must still remove/proceed even if Cloudinary is down.
            // Trade-off: a failed delete here is only visible in logs, so storage
            // can quietly accumulate orphaned assets if Cloudinary errors go
            // unnoticed - worth an alert on this log line if that becomes a problem.
            log.warn("Cloudinary delete failed for publicId={}, continuing anyway", publicId, e);
        }
    }

    // Removes the remote files only once the surrounding DB transaction has
    // committed. Deleting them inline would destroy the files first and leave
    // rows pointing at dead URLs if the transaction later rolls back; the
    // inverse ordering (rows gone, files briefly lingering until this fires)
    // is the recoverable side of that trade.
    public void deleteAllAfterCommit(Collection<Attachment> attachments) {
        if (attachments.isEmpty()) {
            return;
        }
        // Snapshot the identifiers now - the entities are deleted (and possibly
        // detached/unloadable) by the time the callback runs.
        List<String[]> refs = attachments.stream()
                .map(a -> new String[]{a.getPublicId(), a.getResourceType()})
                .toList();

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    refs.forEach(ref -> delete(ref[0], ref[1]));
                }
            });
        } else {
            refs.forEach(ref -> delete(ref[0], ref[1]));
        }
    }
}
