package ubic.gemma.rest.servlet;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.mockito.Mockito.*;

public class RestapidocsIndexRewriteFilterTest {


    @Test
    public void test() throws ServletException, IOException {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setPathInfo( "/resources/restapidocs/" );
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain filterChain = mock( FilterChain.class );
        new RestapidocsIndexRewriteFilter().doFilter( req, res, filterChain );
        verifyNoInteractions( filterChain );
    }

    @Test
    public void testWithoutTrailingSlash() throws ServletException, IOException {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setPathInfo( "/resources/restapidocs" );
        HttpServletResponse res = mock( HttpServletResponse.class );
        FilterChain filterChain = mock( FilterChain.class );
        new RestapidocsIndexRewriteFilter().doFilter( req, res, filterChain );
        verifyNoInteractions( filterChain );
        verify( res ).sendRedirect( "/resources/restapidocs/" );
    }

    @Test
    public void testWithDifferentPath() throws ServletException, IOException {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setPathInfo( "/resources/docs/" );
        HttpServletResponse res = mock( HttpServletResponse.class );
        FilterChain filterChain = mock( FilterChain.class );
        new RestapidocsIndexRewriteFilter().doFilter( req, res, filterChain );
        verify( filterChain ).doFilter( req, res );
    }

    @Test
    public void testWithQuery() throws ServletException, IOException {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setPathInfo( "/resources/restapidocs/" );
        req.setQueryString( "a=b" );
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain filterChain = mock( FilterChain.class );
        new RestapidocsIndexRewriteFilter().doFilter( req, res, filterChain );
        verifyNoInteractions( filterChain );
    }
}