package com.lars.examples.zoom.calendarapidemo.zoom.calendar;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CreateCalendarEventRequest(
        @JsonProperty("start") DeconstructedDate start,
        @JsonProperty("end") DeconstructedDate end,
        @JsonProperty("attendees") Attendee[] attendees,
        @JsonProperty("location") String location,
        @JsonProperty("summary") String summary,
        @JsonProperty("description") String description,
        @JsonProperty("status") String status) {
                
}
