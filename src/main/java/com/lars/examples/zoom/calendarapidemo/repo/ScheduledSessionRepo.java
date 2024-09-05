package com.lars.examples.zoom.calendarapidemo.repo;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

@Service
public class ScheduledSessionRepo {

    private static final String INSERT_SQL = "insert into scheduled_session (start_date, session_name, passcode, host_email, attendee_email) values (:startDate, :sessionName, :passcode, :hostEmail, :attendeeEmail) ";
    private static final String SELECT_SQL = "select session_name, start_date, passcode from scheduled_session where session_name = :sessionName";

    private final JdbcClient jdbcClient;

    private RowMapper<ScheduledSession> rowMapper = (rs, rowNum) -> new ScheduledSession(
            rs.getString("session_name"),
            rs.getString("passcode"), rs.getTimestamp("start_date").toLocalDateTime());

    /**
     * Constuctor, sets up database connectivity
     * @param jdbcClient the JDBC client
     */
    public ScheduledSessionRepo(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    /**
     * Inserts a new scheduled session entry into the database
     * @param startDate the start date of the session
     * @param sessionName session name
     * @param passCode passcode for the session
     * @param hostEmail the hosts email address
     * @param attendeeEmail the attendees email address
     * @return
     */
    public int insertScheduledSession(LocalDateTime startDate, String sessionName, String passCode, String hostEmail,
            String attendeeEmail) {
        return jdbcClient
                .sql(INSERT_SQL)
                .param("startDate", startDate)
                .param("sessionName", sessionName)
                .param("passcode", passCode)
                .param("hostEmail", hostEmail)
                .param("attendeeEmail", attendeeEmail)
                .update();
    }

    /**
     * Retrieves a scheduled session by its name, optional in case it does not exist
     * @param sessionName the name of the session to retrieve
     * @return Optional with the scheduled session, if any
     */
    public Optional<ScheduledSession> getScheduledSession(String sessionName) {
        return jdbcClient
                .sql(SELECT_SQL)
                .param("sessionName", sessionName)
                .query(rowMapper).optional();
    }
}
