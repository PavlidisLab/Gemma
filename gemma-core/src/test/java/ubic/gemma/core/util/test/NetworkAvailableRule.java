package ubic.gemma.core.util.test;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.*;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assume.assumeNoException;
import static org.junit.Assume.assumeTrue;

/**
 * Enables the usage of {@link NetworkAvailable} test annotation.
 *
 * @author poirigui
 */
public class NetworkAvailableRule implements TestRule {

    private static final Map<String, Outcome> testedUrls = new ConcurrentHashMap<>();

    @Override
    public Statement apply( Statement base, Description description ) {
        boolean isAnnotationPresent = false;
        LinkedHashSet<String> urls = new LinkedHashSet<>();
        int maxTimeoutMillis = 0;
        for ( NetworkAvailable annotation : description.getTestClass().getAnnotationsByType( NetworkAvailable.class ) ) {
            isAnnotationPresent = true;
            urls.addAll( Arrays.asList( annotation.url() ) );
            maxTimeoutMillis = Math.max( maxTimeoutMillis, annotation.timeoutMillis() );
        }
        for ( Annotation annotation : description.getAnnotations() ) {
            if ( annotation.annotationType().equals( NetworkAvailable.class ) ) {
                isAnnotationPresent = true;
                urls.addAll( Arrays.asList( ( ( NetworkAvailable ) annotation ).url() ) );
                maxTimeoutMillis = Math.max( maxTimeoutMillis, ( ( NetworkAvailable ) annotation ).timeoutMillis() );
            }
        }
        int timeoutMillis = maxTimeoutMillis;
        if ( isAnnotationPresent ) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    for ( String url : urls ) {
                        assumeThatResourceIsAvailable( url, timeoutMillis );
                    }
                    try {
                        base.evaluate();
                    } catch ( Exception e ) {
                        assumeThatExceptionIsDueToNetworkIssue( e );
                        throw e;
                    }
                }
            };
        } else {
            return base;
        }
    }

    private void assumeThatResourceIsAvailable( String url, int timeoutMillis ) throws Exception {
        Outcome outcome = testedUrls.computeIfAbsent( url, ignored -> {
            try {
                assumeThatResourceIsAvailableInternal( url, timeoutMillis );
                return new Success();
            } catch ( Exception e ) {
                return new ErrorOutcome( e );
            }
        } );
        if ( outcome instanceof Success ) {
            // all good!
        } else if ( outcome instanceof ErrorOutcome ) {
            throw ( ( ErrorOutcome ) outcome ).exception;
        } else {
            throw new UnsupportedOperationException( "Unsupported outcome type: " + outcome.getClass().getName() );
        }
    }

    private void assumeThatResourceIsAvailableInternal( String url, int timeoutMillis ) throws IOException {
        URLConnection con = null;
        try {
            con = new URL( url ).openConnection();
            // set a very strict connection timeout
            con.setConnectTimeout( timeoutMillis );
            // needed to explicitly attempt a connection since we're not consuming the payload
            con.connect();
            if ( con instanceof HttpURLConnection ) {
                HttpURLConnection httpCon = ( HttpURLConnection ) con;
                assumeTrue( String.format( "The resource at %s responded with a %d %s HTTP status code.",
                                url, httpCon.getResponseCode(), httpCon.getResponseMessage() ),
                        httpCon.getResponseCode() < 400 );
            }
        } catch ( UnknownHostException | ConnectException | SocketTimeoutException e ) {
            assumeNoException( String.format( "The resource at %s is not available.", url ), e );
        } catch ( SSLException e ) {
            assumeNoException( String.format( "SSL issue attempting to connect to %s.", url ), e );
        } finally {
            if ( con instanceof HttpURLConnection ) {
                ( ( HttpURLConnection ) con ).disconnect();
            }
        }
    }

    private void assumeThatExceptionIsDueToNetworkIssue( Exception e ) {
        if ( e instanceof IOException ) {
            checkIOException( ( IOException ) e );
        }
        if ( e.getCause() instanceof IOException ) {
            assumeThatExceptionIsDueToNetworkIssue( ( IOException ) e.getCause() );
        }
    }

    private void checkIOException( IOException e ) {
        if ( e instanceof ConnectException ) {
            assumeNoException( "Test skipped due to connection exception", e );
        } else if ( e instanceof UnknownHostException ) {
            assumeNoException( "Test skipped due to unknown host exception", e );
        } else if ( e instanceof SSLException ) {
            assumeNoException( "SSL issue attempting to connect.", e );
        } else if ( e instanceof SocketTimeoutException ) {
            assumeNoException( "Test skipped due to a socket timeout.", e );
        } else if ( e.getMessage() != null && e.getMessage().contains( "504" ) ) {
            assumeNoException( "Test skipped due to a 504 error", e );
        } else if ( e.getMessage() != null && e.getMessage().contains( "503" ) ) {
            assumeNoException( "Test skipped due to a 503 error", e );
        } else if ( e.getMessage() != null && e.getMessage().contains( "502" ) ) {
            assumeNoException( "Test skipped due to a 502 error", e );
        } else if ( e.getMessage() != null && e.getMessage().contains( "500" ) ) {
            assumeNoException( "Test skipped due to a 500 error", e );
        }
    }

    private static abstract class Outcome {

    }

    private static class Success extends Outcome {
        private Success() {
        }
    }

    private static class ErrorOutcome extends Outcome {

        private final Exception exception;

        private ErrorOutcome( Exception exception ) {
            this.exception = exception;
        }
    }
}
