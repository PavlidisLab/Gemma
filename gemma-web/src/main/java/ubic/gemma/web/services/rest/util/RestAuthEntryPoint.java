package ubic.gemma.web.services.rest.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import java.io.IOException;

public class RestAuthEntryPoint extends BasicAuthenticationEntryPoint {

    private static Log log = LogFactory.getLog( RestAuthEntryPoint.class );

    private static final String MESSAGE_401 = "Provided authentication credentials are invalid.";

    @Override
    public void afterPropertiesSet() {
        setRealmName( "Gemma rest api" );
    }

    @Override
    public void commence( final HttpServletRequest request, final HttpServletResponse response,
            final AuthenticationException authException ) {

        response.setStatus( HttpServletResponse.SC_UNAUTHORIZED );
        // using 'xBasic' instead of 'basic' to prevent default browser login popup
        response.addHeader( "WWW-Authenticate", "xBasic realm=" + getRealmName() + "" );
        response.addHeader( "Access-Control-Allow-Headers", "**Authorization**,authorization" ); // necessary for vue gembrow access
        response.setContentType( "application/json" );

        WellComposedErrorBody errorBody = new WellComposedErrorBody( Response.Status.UNAUTHORIZED, MESSAGE_401 );
        ResponseErrorObject errorObject = new ResponseErrorObject( errorBody );
        ObjectMapper mapperObj = new ObjectMapper();

        try {
            String jsonStr = mapperObj.writeValueAsString( errorObject );
            response.getWriter().println( jsonStr );
        } catch ( IOException e ) {
            log.error( e );
        }
    }
}