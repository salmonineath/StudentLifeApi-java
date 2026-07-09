package com.studentlife.studentlifejava.exception;

public class NotificationNotFoundException extends ApiException {

    public NotificationNotFoundException(Long id) {
        super(404, "Notification not found: " + id);
    }
}
