package ubic.gemma.core.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.CredentialsContainer;
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
     * Issue a {@code DELETE} against a given REST endpoint.
     *
     * <p>Exists for the admin cache surface, which is only reachable by {@code DELETE}. A CLI that
     * rebuilds a table can evict its own JVM's Hibernate regions, but gemma-rest is a different
     * process and learns nothing from that — so the only way for a rebuild to stop the server
     * serving what it cached beforehand is a call like this one.</p>
     *
     * @return an {@link EmptyResponse} on a 204, which is what the cache endpoints return on
     *         success, or an {@link ErrorResponse} the caller is expected to inspect
     */
    Response delete( String endpoint ) throws IOException;

    /**
     * Set credentials used for authenticating API requests.
     */
    void setAuthentication( Authentication authentication );

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
     * For endpoints that may return an error.
     */
    interface ErrorResponse extends Response {

        Error getError();

        interface Error {

            int getCode();

            String getMessage();
        }
    }
}
