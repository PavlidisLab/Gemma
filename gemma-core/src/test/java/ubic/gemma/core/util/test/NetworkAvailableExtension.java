package ubic.gemma.core.util.test;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.junit.platform.commons.support.AnnotationSupport;
import org.opentest4j.TestAbortedException;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Jupiter equivalent of {@link NetworkAvailableRule}. Enables the {@link NetworkAvailable} annotation on JUnit 5 tests.
 * <p>
 * Two-phase behaviour, mirroring the JUnit 4 rule:
 * <ol>
 *  <li>Before each test, for every URL declared via {@code @NetworkAvailable(url=...)} on the test class or method,
 *      attempt a short-timeout connection. If the resource is unreachable, abort the test with a {@link TestAbortedException}
 *      (which Jupiter treats as a skip, not a failure).</li>
 *  <li>If the test itself throws an exception that looks like a network failure ({@link ConnectException},
 *      {@link UnknownHostException}, {@link SocketTimeoutException}, {@link SSLException}, or HTTP 5xx wrapped in
 *      {@link IOException}), convert it into a {@link TestAbortedException} so the test is skipped rather than failed.</li>
 * </ol>
 * Per-URL outcomes are cached statically so the connection check runs at most once per URL per JVM.
 *
 * @author poirigui
 * @see NetworkAvailable
 * @see NetworkAvailableRule
 */
public class NetworkAvailableExtension implements BeforeEachCallback, TestExecutionExceptionHandler {

    private static final Map<String, Outcome> testedUrls = new ConcurrentHashMap<>();

    @Override
    public void beforeEach( ExtensionContext ctx ) {
        Resolved r = resolve( ctx );
        if ( !r.present ) {
            return;
        }
        for ( String url : r.urls ) {
            assumeThatResourceIsAvailable( url, r.timeoutMillis );
        }
    }

    @Override
    public void handleTestExecutionException( ExtensionContext ctx, Throwable t ) throws Throwable {
        Resolved r = resolve( ctx );
        if ( !r.present ) {
            throw t;
        }
        if ( t instanceof Exception ) {
            assumeThatExceptionIsDueToNetworkIssue( ( Exception ) t );
        }
        throw t;
    }

    private Resolved resolve( ExtensionContext ctx ) {
        LinkedHashSet<String> urls = new LinkedHashSet<>();
        int maxTimeoutMillis = 0;
        boolean present = false;
        Optional<NetworkAvailable> onClass = ctx.getTestClass()
                .flatMap( c -> AnnotationSupport.findAnnotation( c, NetworkAvailable.class ) );
        if ( onClass.isPresent() ) {
            present = true;
            urls.addAll( Arrays.asList( onClass.get().url() ) );
            maxTimeoutMillis = Math.max( maxTimeoutMillis, onClass.get().timeoutMillis() );
        }
        Optional<NetworkAvailable> onMethod = ctx.getTestMethod()
                .flatMap( m -> AnnotationSupport.findAnnotation( m, NetworkAvailable.class ) );
        if ( onMethod.isPresent() ) {
            present = true;
            urls.addAll( Arrays.asList( onMethod.get().url() ) );
            maxTimeoutMillis = Math.max( maxTimeoutMillis, onMethod.get().timeoutMillis() );
        }
        return new Resolved( present, urls, maxTimeoutMillis );
    }

    private static void assumeThatResourceIsAvailable( String url, int timeoutMillis ) {
        Outcome outcome = testedUrls.computeIfAbsent( url, ignored -> {
            try {
                checkResource( url, timeoutMillis );
                return Outcome.SUCCESS;
            } catch ( TestAbortedException e ) {
                return new Aborted( e );
            } catch ( Exception e ) {
                return new Failed( e );
            }
        } );
        if ( outcome instanceof Aborted ) {
            throw new TestAbortedException( ( ( Aborted ) outcome ).cause.getMessage(), ( ( Aborted ) outcome ).cause );
        } else if ( outcome instanceof Failed ) {
            // Re-raise as an abort: at the rule layer JUnit 4's assumeNoException also produces a skip.
            throw new TestAbortedException( "The resource at " + url + " is not available.", ( ( Failed ) outcome ).cause );
        }
    }

    private static void checkResource( String url, int timeoutMillis ) throws IOException {
        URLConnection con = null;
        try {
            con = new URL( url ).openConnection();
            con.setConnectTimeout( timeoutMillis );
            con.connect();
            if ( con instanceof HttpURLConnection ) {
                HttpURLConnection httpCon = ( HttpURLConnection ) con;
                if ( httpCon.getResponseCode() >= 400 ) {
                    throw new TestAbortedException( String.format( "The resource at %s responded with a %d %s HTTP status code.",
                            url, httpCon.getResponseCode(), httpCon.getResponseMessage() ) );
                }
            }
        } catch ( UnknownHostException | ConnectException | SocketTimeoutException e ) {
            throw new TestAbortedException( String.format( "The resource at %s is not available.", url ), e );
        } catch ( SSLException e ) {
            throw new TestAbortedException( String.format( "SSL issue attempting to connect to %s.", url ), e );
        } finally {
            if ( con instanceof HttpURLConnection ) {
                ( ( HttpURLConnection ) con ).disconnect();
            }
        }
    }

    private static void assumeThatExceptionIsDueToNetworkIssue( Exception e ) {
        if ( e instanceof IOException ) {
            checkIOException( ( IOException ) e );
        }
        if ( e.getCause() instanceof Exception ) {
            assumeThatExceptionIsDueToNetworkIssue( ( Exception ) e.getCause() );
        }
    }

    private static void checkIOException( IOException e ) {
        if ( e instanceof ConnectException ) {
            throw new TestAbortedException( "Test skipped due to connection exception", e );
        } else if ( e instanceof UnknownHostException ) {
            throw new TestAbortedException( "Test skipped due to unknown host exception", e );
        } else if ( e instanceof SSLException ) {
            throw new TestAbortedException( "SSL issue attempting to connect.", e );
        } else if ( e instanceof SocketTimeoutException ) {
            throw new TestAbortedException( "Test skipped due to a socket timeout.", e );
        } else if ( e.getMessage() != null && ( e.getMessage().contains( "504" ) || e.getMessage().contains( "503" )
                || e.getMessage().contains( "502" ) || e.getMessage().contains( "500" ) ) ) {
            throw new TestAbortedException( "Test skipped due to a server error response.", e );
        }
    }

    private static final class Resolved {
        final boolean present;
        final LinkedHashSet<String> urls;
        final int timeoutMillis;

        Resolved( boolean present, LinkedHashSet<String> urls, int timeoutMillis ) {
            this.present = present;
            this.urls = urls;
            this.timeoutMillis = timeoutMillis;
        }
    }

    private static abstract class Outcome {
        static final Outcome SUCCESS = new Outcome() {};
    }

    private static final class Aborted extends Outcome {
        final TestAbortedException cause;

        Aborted( TestAbortedException cause ) {
            this.cause = cause;
        }
    }

    private static final class Failed extends Outcome {
        final Exception cause;

        Failed( Exception cause ) {
            this.cause = cause;
        }
    }
}
