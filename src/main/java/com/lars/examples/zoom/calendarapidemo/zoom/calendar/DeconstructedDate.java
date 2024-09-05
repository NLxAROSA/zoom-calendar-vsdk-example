package com.lars.examples.zoom.calendarapidemo.zoom.calendar;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DeconstructedDate(
    @JsonProperty("dateTime") String dateTime,
    @JsonProperty("timeZone") String timeZone ) {
}
