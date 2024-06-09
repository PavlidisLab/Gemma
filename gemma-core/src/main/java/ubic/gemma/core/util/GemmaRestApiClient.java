package ubic.gemma.core.util;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.IOException;

/**
 * A minimalist client for accessing Gemma's RESTful API.
 * @author poirigui
 */
public interface GemmaRestApiClient {

    String getHostUrl();

    /**
     * Access a given REST endpoint.
     */
    Response perform( String endpoint ) throws IOException;

    /**
     * Access a given endpoint REST endpoint with parameters.
     * @throws IOException if anything goes wrong with I/O including issues with JSON deserialization
     */
    Response perform( String endpoint, MultiValueMap<String, Object> params ) throws IOException;

    Response perform( String endpoint, String firstParamName, Object firstParamValue, Object... otherParams ) throws IOException;

    /**
     * Set credentials used for authenticating API requests.
     */
    void setAuthentication( String username, String password );

    /**
     * Clear any credentials.
     */
    void clearAuthentication();

    /**
     * A response from the API, which is either a {@link DataResponse} or {@link ErrorResponse}.
     */
    interface Response {

    }

    interface DataResponse extends Response {

        Object getData();
    }

    /**
     * For endpoints that return no data (i.e. a 201 No Content reply code).
     */
    interface EmptyResponse extends Response {

    }

    /**
     * For endpoint that return a redirection.
     */
    interface Redirection extends Response {

        String getLocation();
    }

    interface ErrorResponse extends Response {

        Error getError();

        interface Error {

            int getCode();

            String getMessage();
        }
    }
}
