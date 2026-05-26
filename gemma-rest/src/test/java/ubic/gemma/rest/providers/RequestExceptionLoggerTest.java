package ubic.gemma.rest.providers;

import org.glassfish.jersey.server.internal.process.MappableException;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The /me firehose regression: every 403 from an anonymous probe was producing a
 * full stack trace at ERROR level because the security exception comes wrapped in
 * {@link MappableException}. Confirm the unwrap walks through the wrapper.
 */
class RequestExceptionLoggerTest {

    @Test
    void unwrapsMappableExceptionAroundAccessDenied() throws Exception {
        AccessDeniedException root = new AccessDeniedException( "Access is denied" );
        MappableException wrapped = new MappableException( root );

        Throwable found = invokeFindCause( wrapped, AccessDeniedException.class, AuthenticationException.class );

        assertThat( found ).isSameAs( root );
    }

    @Test
    void unwrapsMappableExceptionAroundAuthenticationException() throws Exception {
        AuthenticationException root = new BadCredentialsException( "bad creds" );
        MappableException wrapped = new MappableException( root );

        Throwable found = invokeFindCause( wrapped, AccessDeniedException.class, AuthenticationException.class );

        assertThat( found ).isSameAs( root );
    }

    @Test
    void returnsNullWhenChainHasNoSecurityException() throws Exception {
        Throwable wrapped = new MappableException( new RuntimeException( "something else" ) );

        Throwable found = invokeFindCause( wrapped, AccessDeniedException.class, AuthenticationException.class );

        assertThat( found ).isNull();
    }

    @Test
    void handlesSelfReferentialCauseChain() throws Exception {
        // Pathological case: an exception whose cause is itself. Must not spin.
        RuntimeException self = new RuntimeException( "spin" ) {
            @Override
            public Throwable getCause() {
                return this;
            }
        };
        Throwable found = invokeFindCause( self, AccessDeniedException.class );
        assertThat( found ).isNull();
    }

    @SuppressWarnings("unchecked")
    private static Throwable invokeFindCause( Throwable t, Class<? extends Throwable>... targets ) throws Exception {
        Method m = RequestExceptionLogger.class.getDeclaredMethod( "findCause", Throwable.class, Class[].class );
        m.setAccessible( true );
        return ( Throwable ) m.invoke( null, t, targets );
    }
}
