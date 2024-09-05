package com.lars.examples.zoom.calendarapidemo.repo;

import java.time.LocalDateTime;

public record ScheduledSession(String sessionName, String passCode, LocalDateTime startDate) {
    
}
