package ubic.gemma.web.services.rest.util;

import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import java.io.IOException;

public class RestAuthEntryPoint extends BasicAuthenticationEntryPoint {

    private static final String MESSAGE_401 = "Provided authentication credentials are invalid.";

    @Override
    public void commence( final HttpServletRequest request, final HttpServletResponse response,
            final AuthenticationException authException ) {

        response.setStatus( HttpServletResponse.SC_UNAUTHORIZED );
        response.addHeader( "WWW-Authenticate", "Basic realm=" + getRealmName() + "" );
        response.setContentType( "application/json" );

        WellComposedErrorBody errorBody = new WellComposedErrorBody( Response.Status.UNAUTHORIZED, MESSAGE_401 );
        ResponseErrorObject errorObject = new ResponseErrorObject(errorBody);
        ObjectMapper mapperObj = new ObjectMapper();

        try {
            String jsonStr = mapperObj.writeValueAsString( errorObject );
            response.getWriter().println( jsonStr );
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }

    @Override
    public void afterPropertiesSet() {
        setRealmName("Gemma rest api");
    }
}