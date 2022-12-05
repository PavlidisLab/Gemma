package ubic.gemma.web.util;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.json.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;

/**
 * Utilities for writing JSON payloads to {@link HttpServletResponse}.
 */
public final class JsonUtil {

    public static void writeErrorToResponse( AuthenticationException e, HttpServletResponse response ) throws IOException {
        JSONObject json = new JSONObject();
        json.put( "success", false );
        json.put( "message", ExceptionUtils.getRootCauseMessage( e ) );
        response.setContentType( MediaType.APPLICATION_JSON_VALUE );
        response.sendError( HttpServletResponse.SC_UNAUTHORIZED, json.toString() );
    }

    public static void writeErrorToResponse( Exception e, HttpServletResponse response ) throws IOException {
        JSONObject json = new JSONObject();
        json.put( "success", false );
        json.put( "message", ExceptionUtils.getRootCauseMessage( e ) );
        response.setContentType( MediaType.APPLICATION_JSON_VALUE );
        response.sendError( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, json.toString() );
    }

    public static void writeToResponse( JSONObject json, HttpServletResponse response ) throws IOException {
        String jsonText = json.toString();
        response.setContentType( MediaType.APPLICATION_JSON_VALUE );
        response.setContentLength( jsonText.length() );
        try ( Writer out = response.getWriter() ) {
            out.write( jsonText );
        }
    }
}
