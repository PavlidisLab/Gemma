package ubic.gemma.web.services.rest.util;

import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class CORSFilter implements Filter {

    public CORSFilter() {
    }

    @Override
    public void init( FilterConfig config ) {
    }

    @Override
    public void doFilter( ServletRequest req, ServletResponse res, FilterChain chain )
            throws IOException, ServletException {
        final HttpServletResponse response = ( HttpServletResponse ) res;
        if ( "OPTIONS".equalsIgnoreCase( ( ( HttpServletRequest ) req ).getMethod() ) ) {
            response.setStatus( HttpServletResponse.SC_OK );
        } else {
            chain.doFilter( req, res );
        }
    }

    @Override
    public void destroy() {
    }
}