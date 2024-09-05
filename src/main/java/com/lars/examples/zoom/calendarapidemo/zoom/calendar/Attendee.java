package com.lars.examples.zoom.calendarapidemo.zoom.calendar;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Attendee(@JsonProperty("email") String email) {
    
}
