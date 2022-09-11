package ubic.gemma.rest.servlet;

import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class CorsFilterTest {

    Filter corsFilter = new CorsFilter();

    @Test
    public void testRequestWithOrigin() throws ServletException, IOException {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setMethod( "GET" );
        req.addHeader( "Origin", "localhost" );
        HttpServletResponse res = new MockHttpServletResponse();
        FilterChain filterChain = new MockFilterChain();
        corsFilter.doFilter( req, res, filterChain );
        assertEquals( HttpStatus.OK.value(), res.getStatus() );
        assertEquals( "*", res.getHeader( "Access-Control-Allow-Origin" ) );
        assertNull( res.getHeader( "Access-Control-Allow-Headers" ) );
    }

    @Test
    public void testRequestWithoutOrigin() throws ServletException, IOException {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setMethod( "GET" );
        HttpServletResponse res = new MockHttpServletResponse();
        FilterChain filterChain = new MockFilterChain();
        corsFilter.doFilter( req, res, filterChain );
        assertEquals( HttpStatus.OK.value(), res.getStatus() );
        assertNull( res.getHeader( "Access-Control-Allow-Origin" ) );
        assertNull( res.getHeader( "Access-Control-Allow-Headers" ) );
    }

    @Test
    public void testPreflightRequest() throws ServletException, IOException {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setMethod( "OPTIONS" );
        req.addHeader( "Origin", "localhost" );
        HttpServletResponse res = new MockHttpServletResponse();
        FilterChain filterChain = new MockFilterChain();
        corsFilter.doFilter( req, res, filterChain );
        assertEquals( HttpStatus.NO_CONTENT.value(), res.getStatus() );
        assertEquals( "Authorization,Content-Type", res.getHeader( "Access-Control-Allow-Headers" ) );
    }

}