package ubic.gemma.rest.util;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.security.Principal;

import static org.assertj.core.api.Assertions.assertThat;

public class ServletUtilsTest {

    @Test
    public void testSummarizeRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest( "GET", "/users" );
        assertThat( ServletUtils.summarizeRequest( request ) ).isEqualTo( "GET /users" );
    }

    @Test
    public void testSummarizeRequestWithQueryString() {
        MockHttpServletRequest request = new MockHttpServletRequest( "GET", "/users" );
        request.setQueryString( "a=b" );
        assertThat( ServletUtils.summarizeRequest( request ) ).isEqualTo( "GET /users?a=b" );
    }

    @Test
    public void testSummarizeRequestWithPrincipal() {
        MockHttpServletRequest request = new MockHttpServletRequest( "GET", "/users" );
        request.setQueryString( "a=b" );
        Principal principal = new UsernamePasswordAuthenticationToken( "foo", "1234" );
        request.setUserPrincipal( principal );
        assertThat( ServletUtils.summarizeRequest( request ) ).isEqualTo( "GET foo@/users?a=b" );
    }
}