package ubic.gemma.web.util;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.json.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Utilities for writing JSON payloads to {@link HttpServletResponse}.
 */
public final class JsonUtil {

    public static void writeErrorToResponse( AuthenticationException e, HttpServletResponse response ) throws IOException {
        JSONObject json = new JSONObject();
        json.put( "success", false );
        json.put( "message", ExceptionUtils.getRootCauseMessage( e ) );
        response.setStatus( HttpServletResponse.SC_UNAUTHORIZED );
        writeToResponse( json, response );
    }

    public static void writeErrorToResponse( Exception e, HttpServletResponse response ) throws IOException {
        JSONObject json = new JSONObject();
        json.put( "success", false );
        json.put( "message", ExceptionUtils.getRootCauseMessage( e ) );
        response.setStatus( HttpServletResponse.SC_INTERNAL_SERVER_ERROR );
        writeToResponse( json, response );
    }

    public static void writeToResponse( JSONObject json, HttpServletResponse response ) throws IOException {
        response.setContentType( MediaType.APPLICATION_JSON_VALUE );
        response.setCharacterEncoding( StandardCharsets.UTF_8.name() );
        json.write( response.getWriter() );
        response.flushBuffer();
    }
}
