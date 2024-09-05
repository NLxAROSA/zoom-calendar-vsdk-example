package com.lars.examples.zoom.calendarapidemo.web;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.lars.examples.zoom.calendarapidemo.ZoomApiConfiguration;
import com.lars.examples.zoom.calendarapidemo.repo.ScheduledSession;
import com.lars.examples.zoom.calendarapidemo.repo.ScheduledSessionRepo;
import com.lars.examples.zoom.calendarapidemo.zoom.AccessTokenResponse;
import com.lars.examples.zoom.calendarapidemo.zoom.ZoomApiUtil;
import com.lars.examples.zoom.calendarapidemo.zoom.calendar.Attendee;
import com.lars.examples.zoom.calendarapidemo.zoom.calendar.CreateCalendarEventRequest;
import com.lars.examples.zoom.calendarapidemo.zoom.calendar.CreateCalendarEventResponse;
import com.lars.examples.zoom.calendarapidemo.zoom.calendar.DeconstructedDate;

@Controller
public class SessionScheduleController {

    private final ZoomApiConfiguration zac;
    private final ScheduledSessionRepo ssr;

    private final SecureRandom randomNumberGenerator;
    private static final char[] ALLOWED_PASSCODE_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ01234567890".toCharArray();
    private static final int PASSCODE_MAX_LENGTH = 8;

    private static final int SESSION_DURATION_MINUTES = 60;
    private static final int SESSION_JOIN_BEFORE_START_MINUTES = 15;
    private static final int SESSION_JOIN_AFTER_END_MINUTES = 15;

    private static final String LOCAL_TIMEZONE = "Europe/Amsterdam";
    private static final String CALENDAR_EVENT_DESCRIPTION_PREFIX = "Your session join link is: \n";
    private static final String CALENDAR_EVENT_LOCATION = "Zoom Video SDK session";
    private static final String CALENDAR_EVENT_STATUS = "confirmed";
    private static final String CALENDAR_EVENT_SUMMARY = "Meet with our expert: Lars";


    private static final String SHA1PRNG_ALGO = "SHA1PRNG";

    /**
     * Constructor
     * @param scheduledSessionRepo Database repository
     * @param zoomApiConfiguration API and other configuration
     * @throws NoSuchAlgorithmException in case SHA1PRNG is not supported
     * @throws UnsupportedEncodingException if UTF-8 is not supported for some reason
     */
    public SessionScheduleController(ScheduledSessionRepo scheduledSessionRepo,
            ZoomApiConfiguration zoomApiConfiguration) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        this.ssr = scheduledSessionRepo;
        this.zac = zoomApiConfiguration;
        this.randomNumberGenerator = SecureRandom.getInstance(SHA1PRNG_ALGO); // RNG for generating passcodes
    }

    /**
     * Renders the index page
     * @param model the Model to put the session data in
     * @param scheduledSession model object for user to enter their data
     * @return the index page
     */
    @GetMapping("/")
    public String index(Model model, @ModelAttribute("scheduledSession") SessionScheduleRequest scheduledSession) {
        return "index.html";
    }

    /**
     * Schedules a session based on the users input
     * - Validates input, generates session name, passcode and joinlink and creates a Zoom Calendar event
     * @param model the Model that holds the data the user entered
     * @param scheduledSession the data the user entered
     * @return Success page, or error page if something went wrong
     * @throws UnsupportedEncodingException in case UTF-8 is not supported for some reason
     */
    @PostMapping(value = "/scheduled")
    public String scheduleSession(Model model,
            @ModelAttribute("scheduledSession") SessionScheduleRequest scheduledSession)
            throws UnsupportedEncodingException {

        // Retrieve and validate user input
        String attendeeEmail = scheduledSession.attendeeEmail();
        LocalDateTime sessionDate = scheduledSession.sessionDate();

        // Generate a (unique) session name and passcode
        String sessionName = this.generateSessionName();
        String passCode = this.generatePassCode();

        // Generate a session join link
        String joinLink = this.generateJoinLink(sessionName, passCode);

        // Create a calendar entry with the (shared) session details
        this.createNewCalendarEvent(attendeeEmail, sessionDate, sessionName, passCode, joinLink);

        // Create a database entry with the (private) session details
        this.storeScheduledSession(attendeeEmail, sessionDate, sessionName, passCode);

        return "scheduled.html";
    }

    /**
     * Validates and starts a Video SDK session. Will validate the incoming details and relay the request to the Video SDK app
     * @param sessionName the name of the session as provided in the URL
     * @param passcode the passcode of the session as provided in the URL
     * @param model The model object in which to store an error object in case a validation error occurs
     * @return The session page if all data was valid, error page if not
     */
    @GetMapping("/session")
    public String session(@RequestParam(required = true) String sessionName,
            @RequestParam(required = true) String passcode, Model model) {
        // Retrieve private session details
        Optional<ScheduledSession> scheduledSession = ssr.getScheduledSession(sessionName);
        if (!scheduledSession.isPresent()) {
            // No session was found matching the supplied session name
            model.addAttribute("errorMessage", new ErrorMessage("No valid session could be found"));
            return "caught-error.html";
        } else {
            // A session was found, check if it can be joined this time
            Optional<String> validationError = validateScheduledSession(scheduledSession.get(), passcode);
            if (validationError.isPresent()) {
                model.addAttribute("errorMessage", new ErrorMessage(validationError.get()));
                return "caught-error.html";
            } else {
                return "session.html";
            }
        }
    }

    /**
     * Validates the session details and passcode
     * - Does the provided passcode match the stored passcode
     * - Can this session be started/joined according to the scheduled start/end date/time
     * @param scheduledSession The scheduled session
     * @param passcode the passcode
     * @return Optional of String with any error that may occur
     */
    private Optional<String> validateScheduledSession(ScheduledSession scheduledSession, String passcode) {
        String validationError = null;
        if (!scheduledSession.passCode().equals(passcode)) {
            // Supplied passcode does not match stored passcode
            validationError = "You are not allowed to access this session";
        }

        if (LocalDateTime.now().isBefore(scheduledSession.startDate().minusMinutes(SESSION_JOIN_BEFORE_START_MINUTES))) {
            // Session can't be joined more than 15 minutes early
            validationError = "This session has not started yet, please try again later, but no earlier than 15 minutes before start";
        }

        if (LocalDateTime.now()
                .isAfter(scheduledSession.startDate().plusMinutes(SESSION_DURATION_MINUTES + SESSION_JOIN_AFTER_END_MINUTES))) {
            // Session can't be joined more than 15 minutes after it ended
            validationError = "This session has already ended and can no longer be joined";
        }

        return Optional.ofNullable(validationError);
    }

    /**
     * Stores the scheduled session in the database
     * @param attendeeEmail the email of the attendee
     * @param sessionDate the date of the session in LocalDateTime format
     * @param sessionName the name of the session
     * @param passCode the passcode for the session
     * @throws UnsupportedEncodingException if UTF-8 is not supported for some reason
     */
    private void storeScheduledSession(String attendeeEmail, LocalDateTime sessionDate, String sessionName,
            String passCode)
            throws UnsupportedEncodingException {
        ssr.insertScheduledSession(sessionDate, sessionName, passCode, zac.getCalendarId(), attendeeEmail);
    }

    /**
     * Creates a new Zoom Calendar Event
     * @param attendeeEmail the email address of the attendee
     * @param sessionStart the start of the session in LocalDateTime format
     * @param sessionName the name of the session
     * @param passCode the passcode for the session
     * @param joinLink the link to join/start the session
     * @return A String with the ID of the created event
     */
    private String createNewCalendarEvent(String attendeeEmail, LocalDateTime sessionStart, String sessionName,
            String passCode, String joinLink) {

        // Get an access token
        AccessTokenResponse accessTokenResponse = ZoomApiUtil.accessToken(zac.getGrantType(),
                zac.getAccountId(), zac.getClientId(),
                zac.getClientSecret());

        // Create the required event data
        Attendee attendee = new Attendee(attendeeEmail);
        Attendee[] attendees = new Attendee[1];
        attendees[0] = attendee;
        String description = CALENDAR_EVENT_DESCRIPTION_PREFIX + joinLink;

        // Determine end time based on start time + 60 minutes and format them
        String sessionStartString = sessionStart.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        LocalDateTime sessionEnd = sessionStart.plusMinutes(SESSION_DURATION_MINUTES);
        String sessionEndString = sessionEnd.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        CreateCalendarEventRequest request = new CreateCalendarEventRequest(
                new DeconstructedDate(sessionStartString, LOCAL_TIMEZONE),
                new DeconstructedDate(sessionEndString, LOCAL_TIMEZONE),
                attendees,
                CALENDAR_EVENT_LOCATION,
                CALENDAR_EVENT_SUMMARY,
                description,
                CALENDAR_EVENT_STATUS);

        CreateCalendarEventResponse response = ZoomApiUtil.zoomApiPostRequest(request,
                "/calendars/" + zac.getCalendarId() + "/events", accessTokenResponse.accessToken(),
                CreateCalendarEventResponse.class);

        return response.id();
    }

    /**
     * Generates a session name, given a configured default and adding in a session number to make it more random
     * @return A name for the session
     * @throws UnsupportedEncodingException if UTF-8 is not supported for some reason
     */
    private String generateSessionName() throws UnsupportedEncodingException {

        Long identifier = 1000000 + randomNumberGenerator.nextLong(9999999);

        String sessionName = CALENDAR_EVENT_SUMMARY + " (" + identifier + ")";
        return Base64.getEncoder().encodeToString(String.valueOf(sessionName).getBytes("UTF-8"));
    }

    /**
     * Generates a passcode, based on a configured length and set of allowed characters 
     * @return A generate passcode
     * @throws UnsupportedEncodingException if UTF-8 is not supported for some reason
     */
    private String generatePassCode() throws UnsupportedEncodingException {

        char[] passCodeCharacters = new char[PASSCODE_MAX_LENGTH];

        for (int i = 0; i < PASSCODE_MAX_LENGTH - 1; i++) {
            passCodeCharacters[i] = ALLOWED_PASSCODE_CHARACTERS[randomNumberGenerator
                    .nextInt(ALLOWED_PASSCODE_CHARACTERS.length)];
        }

        return Base64.getEncoder().encodeToString(String.valueOf(passCodeCharacters).getBytes("UTF-8"));
    }

    /**
     * Generates a Zoom Video SDK join link for the given session name and passcode
     * @param sessionName the name of the session to join
     * @param passCode the passcode of the session to join
     * @return A URL pointing back to this application for the given session name and passcode
     */
    private String generateJoinLink(String sessionName, String passCode) {
        return "http://localhost:8080/session?sessionName=" + sessionName + "&passcode=" + passCode;
    }

}
