package ubic.gemma.web.services.rest.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import java.io.IOException;

public class ExceptionHandlerFilter extends OncePerRequestFilter {

    private static final String MESSAGE_401 = "Incorrect authentication credentials.";
    private static final String MESSAGE_500 = "Internal server error. Please report this to us if you can.";

    private static void PrintError( HttpServletResponse response, Response.Status status, String message,
            Exception e ) {
        WellComposedErrorBody errorBody = new WellComposedErrorBody( status, message );
        if ( e != null ) {
            WellComposedErrorBody.addExceptionFields( errorBody, e );
        }

        ResponseErrorObject errorObject = new ResponseErrorObject( errorBody );
        ObjectMapper mapperObj = new ObjectMapper();

        try {
            String jsonStr = mapperObj.writeValueAsString( errorObject );
            response.getWriter().println( jsonStr );
            response.flushBuffer();
        } catch ( IOException ex ) {
            PrintError( response, Response.Status.INTERNAL_SERVER_ERROR, MESSAGE_500, ex );
        }
    }

    @Override
    public void doFilterInternal( HttpServletRequest request, HttpServletResponse response, FilterChain filterChain )
            throws IOException {
        try {
            filterChain.doFilter( request, response );
        } catch ( ServletException e ) {
            PrintError( response, Response.Status.UNAUTHORIZED, MESSAGE_401, e );
        } catch ( RuntimeException e ) {
            PrintError( response, Response.Status.INTERNAL_SERVER_ERROR, MESSAGE_500, e );
        }
    }
}
