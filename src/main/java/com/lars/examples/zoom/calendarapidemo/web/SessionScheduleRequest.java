package com.lars.examples.zoom.calendarapidemo.web;

import java.time.LocalDateTime;

public record SessionScheduleRequest(LocalDateTime sessionDate, String attendeeEmail) {

}
