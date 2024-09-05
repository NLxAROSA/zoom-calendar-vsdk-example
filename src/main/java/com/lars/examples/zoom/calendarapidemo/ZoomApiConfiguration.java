package com.lars.examples.zoom.calendarapidemo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ZoomApiConfiguration {
    @Value("${zoom.oauth.token.granttype}")
	private String grantType;
	@Value("${zoom.oauth.token.accountid}")
	private String accountId;
	@Value("${zoom.oauth.token.clientid}")
	private String clientId;
	@Value("${zoom.oauth.token.clientsecret}")
	private String clientSecret;
	@Value("${zoom.calendar.id}")
	private String calendarId;

    
    public String getCalendarId() {
        return calendarId;
    }
    public void setCalendarId(String calendarId) {
        this.calendarId = calendarId;
    }
    public String getGrantType() {
        return grantType;
    }
    public void setGrantType(String grantType) {
        this.grantType = grantType;
    }
    public String getAccountId() {
        return accountId;
    }
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }
    public String getClientId() {
        return clientId;
    }
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
    public String getClientSecret() {
        return clientSecret;
    }
    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    
}
