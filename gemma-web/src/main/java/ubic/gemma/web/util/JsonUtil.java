package ubic.gemma.web.util;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.json.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.util.Assert;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Utilities for writing JSON payloads to {@link HttpServletResponse}.
 */
public final class JsonUtil {

    public static void writeErrorToResponse( AuthenticationException e, HttpServletResponse response ) throws IOException {
        writeErrorToResponse( HttpServletResponse.SC_UNAUTHORIZED, ExceptionUtils.getRootCauseMessage( e ), response );
    }

    public static void writeErrorToResponse( Exception e, HttpServletResponse response ) throws IOException {
        writeErrorToResponse( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ExceptionUtils.getRootCauseMessage( e ), response );
    }

    public static void writeErrorToResponse( int status, String message, HttpServletResponse response ) throws IOException {
        Assert.isTrue( status >= 400 && status < 600, "Status code must be in 4xx or 5xx ranges." );
        JSONObject json = new JSONObject();
        json.put( "success", false );
        json.put( "message", message );
        response.setStatus( status );
        writeToResponse( json, response );
    }

    public static void writeToResponse( JSONObject json, HttpServletResponse response ) throws IOException {
        response.setContentType( MediaType.APPLICATION_JSON_VALUE );
        response.setCharacterEncoding( StandardCharsets.UTF_8.name() );
        json.write( response.getWriter() );
        response.flushBuffer();
    }
}
