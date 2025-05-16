package ubic.gemma.rest.servlet;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.util.test.BaseTest;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@WebAppConfiguration
@ContextConfiguration
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class CorsFilterTest extends BaseTest {

    @Configuration
    @TestComponent
    static class CorsFilterTestContextConfiguration {

        @Bean
        public CorsFilter corsFilter() {
            return new CorsFilter();
        }
    }

    @Autowired
    private CorsFilter corsFilter;

    @Test
    public void testRequestWithOrigin() throws ServletException, IOException {
        corsFilter.setAllowedOrigins( "*" );
        corsFilter.setAllowedHeaders( "Authorization,Content-Type,X-Gemma-Client-ID" );
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
        corsFilter.setAllowedOrigins( "*" );
        corsFilter.setAllowedHeaders( "Authorization,Content-Type,X-Gemma-Client-ID" );
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
    public void testRequestWithCredentials() throws ServletException, IOException {
        corsFilter.setAllowedOrigins( "http://localhost" );
        corsFilter.setAllowCredentials( true );
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setMethod( "GET" );
        req.addHeader( "Origin", "http://localhost" );
        HttpServletResponse res = new MockHttpServletResponse();
        FilterChain filterChain = new MockFilterChain();
        corsFilter.doFilter( req, res, filterChain );
        assertEquals( HttpStatus.OK.value(), res.getStatus() );
        assertEquals( "http://localhost", res.getHeader( "Access-Control-Allow-Origin" ) );
        assertEquals( "true", res.getHeader( "Access-Control-Allow-Credentials" ) );
        assertEquals( "Origin", res.getHeader( "Vary" ) );
    }

    @Test
    public void testPreflightRequest() throws ServletException, IOException {
        corsFilter.setAllowedOrigins( "*" );
        corsFilter.setAllowedHeaders( "Authorization,Content-Type,X-Gemma-Client-ID" );
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setMethod( "OPTIONS" );
        req.addHeader( "Origin", "localhost" );
        HttpServletResponse res = new MockHttpServletResponse();
        FilterChain filterChain = new MockFilterChain();
        corsFilter.doFilter( req, res, filterChain );
        assertEquals( HttpStatus.NO_CONTENT.value(), res.getStatus() );
        assertEquals( "Authorization,Content-Type,X-Gemma-Client-ID", res.getHeader( "Access-Control-Allow-Headers" ) );
    }

    @Test
    public void testPreflightRequestWithCredentials() throws ServletException, IOException {
        corsFilter.setAllowedOrigins( "https://localhost" );
        corsFilter.setAllowedHeaders( "Authorization" );
        corsFilter.setAllowCredentials( true );
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setMethod( "OPTIONS" );
        req.addHeader( "Origin", "https://localhost" );
        HttpServletResponse res = new MockHttpServletResponse();
        FilterChain filterChain = new MockFilterChain();
        corsFilter.doFilter( req, res, filterChain );
        assertEquals( HttpStatus.NO_CONTENT.value(), res.getStatus() );
        assertEquals( "https://localhost", res.getHeader( "Access-Control-Allow-Origin" ) );
        assertEquals( "Authorization", res.getHeader( "Access-Control-Allow-Headers" ) );
        assertEquals( "true", res.getHeader( "Access-Control-Allow-Credentials" ) );
    }

    @Test
    public void testPreflightRequestWithCredentialsAndInvalidOrigin() throws ServletException, IOException {
        corsFilter.setAllowedOrigins( "https://localhost" );
        corsFilter.setAllowedHeaders( "Authorization" );
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setMethod( "OPTIONS" );
        req.addHeader( "Origin", "http://localhost2" );
        HttpServletResponse res = new MockHttpServletResponse();
        FilterChain filterChain = new MockFilterChain();
        corsFilter.doFilter( req, res, filterChain );
        assertEquals( HttpStatus.FORBIDDEN.value(), res.getStatus() );
        assertNull( res.getHeader( "Access-Control-Allow-Origin" ) );
        assertNull( res.getHeader( "Access-Control-Allow-Headers" ) );
        assertNull( res.getHeader( "Access-Control-Allow-Credentials" ) );
    }

    @Test
    public void testPreflightRequestWithMaxAge() throws ServletException, IOException {
        corsFilter.setAllowedOrigins( "*" );
        corsFilter.setAllowedHeaders( "Authorization,Content-Type,X-Gemma-Client-ID" );
        corsFilter.setMaxAge( 1200 );
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setMethod( "OPTIONS" );
        req.addHeader( "Origin", "localhost" );
        HttpServletResponse res = new MockHttpServletResponse();
        FilterChain filterChain = new MockFilterChain();
        corsFilter.doFilter( req, res, filterChain );
        assertEquals( HttpStatus.NO_CONTENT.value(), res.getStatus() );
        assertEquals( "Authorization,Content-Type,X-Gemma-Client-ID", res.getHeader( "Access-Control-Allow-Headers" ) );
        assertEquals( "1200", res.getHeader( "Access-Control-Max-Age" ) );
    }
}