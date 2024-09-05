package com.lars.examples.zoom.calendarapidemo.zoom;

import java.util.Base64;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

public class ZoomApiUtil {

    private static final String AUTHORIZATION_HEADER_NAME = "Authorization";

    private static final String BASIC_AUTH_PREFIX = "Basic ";
    private static final String BEARER_TOKEN_PREFIX = "Bearer ";

    private static final String ZOOM_API_BASE_URI = "https://api.zoom.us/v2";
    private static final String ZOOM_ACCESS_TOKEN_URI = "https://zoom.us/oauth/token?grant_type={1}&account_id={2}";

    private static final RestClient REST_CLIENT = RestClient.create();

    /**
     * Executes an HTTP GET request against the Zoom API.
     * @param <T> Reference to the response type
     * @param apiPath Path of the API to be called
     * @param accessToken A valid JWT
     * @param responseType The desired response type
     * @return A response of the specified response type
     */
    public static <T> T zoomApiGetRequest(String apiPath, String accessToken, Class<T> responseType) {

        ResponseEntity<T> response = REST_CLIENT.get()
                .uri(ZOOM_API_BASE_URI + apiPath)
                .header(AUTHORIZATION_HEADER_NAME, toBearerToken(accessToken))
                .retrieve()
                .toEntity(responseType);

        return response.getBody();
    }

    /**
     * Executes an HTTP POST request against the Zoom API.
     * @param <T> Reference to the response type
     * @param request A request object
     * @param apiPath Path of the API to be called
     * @param accessToken A valid JWT
     * @param responseType The desired response type
     * @return A response of the specified response type
     */
    public static <T> T zoomApiPostRequest(Object request, String apiPath, String accessToken, Class<T> responseType) {

        ResponseEntity<T> response = REST_CLIENT.post()
                .uri(ZOOM_API_BASE_URI + apiPath)
                .contentType(MediaType.APPLICATION_JSON)
                .header(AUTHORIZATION_HEADER_NAME, toBearerToken(accessToken))
                .body(request)
                .retrieve()
                .toEntity(responseType);

        return response.getBody();
    }

    /**
     * Executes an HTTP DELETE request against the Zoom API. No response in case successful, exception otherwise
     * @param apiPath Path of the API to be called
     * @param accessToken A valid JWT
     */
    public static void zoomApiDeleteRequest(String apiPath, String accessToken) {
        REST_CLIENT.delete()
                .uri(ZOOM_API_BASE_URI + apiPath)
                .header(AUTHORIZATION_HEADER_NAME, toBearerToken(accessToken))
                .retrieve()
                .toBodilessEntity();
    }

    /**
     * Retrieves a JWT based on the specified parameters
     * @param grantType type of grant requested (default is 'account_credentials')
     * @param accountId the account id, as listed in the server-to-server app
     * @param clientId the client id, as listed in the server-to-server app
     * @param clientSecret the client secret, as listed in the server-to-server app
     * @return Response containing the JWT
     */
    public static AccessTokenResponse accessToken(String grantType, String accountId, String clientId,
            String clientSecret) {
        ResponseEntity<AccessTokenResponse> response = REST_CLIENT.post()
                .uri(ZOOM_ACCESS_TOKEN_URI, grantType, accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .header(AUTHORIZATION_HEADER_NAME, toBasicAuthenticationToken(clientId, clientSecret))
                .retrieve()
                .toEntity(AccessTokenResponse.class);

        return response.getBody();
    }

    /**
     * Takes a client id and secret and turns it into a Basic Authentication header value
     * @param clientId the client id, as listed in the server-to-server app
     * @param clientSecret the client secret, as listed in the server-to-server app
     * @return A String that can be directly used as the value for an Authorization header
     */
    public static String toBasicAuthenticationToken(String clientId, String clientSecret) {
        String credentials = clientId + ":" + clientSecret;
        byte[] plainCredsBytes = credentials.getBytes();
        byte[] base64CredsBytes = Base64.getEncoder().encode(plainCredsBytes);
        String base64Creds = new String(base64CredsBytes);
        return BASIC_AUTH_PREFIX + base64Creds;
    }

    /**
     * Takes a valid JWT and turns it into a Bearer Token header value
     * @param accessToken the JWT
     * @return A String that can be directly used as the value for an Authorization header
     */
    public static String toBearerToken(String accessToken) {
        return BEARER_TOKEN_PREFIX + accessToken;
    }

}
