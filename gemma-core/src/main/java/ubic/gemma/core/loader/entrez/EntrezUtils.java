package ubic.gemma.core.loader.entrez;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import ubic.gemma.core.config.Settings;
import ubic.gemma.core.util.SimpleRetryCallable;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;

/**
 * Low-level utilities for generating Entrez URLs.
 * <p>
 * Read more about this in <a href="https://www.ncbi.nlm.nih.gov/books/NBK25500/">The E-utilities In-Depth: Parameters, Syntax and More</a>.
 * @author poirigui
 */
@CommonsLog
public class EntrezUtils {

    public static final String ESEARCH = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi";
    public static final String ESUMMARY = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi";
    public static final String EFETCH = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi";
    public static final String EQUERY = "https://www.ncbi.nlm.nih.gov/entrez/query.fcgi";

    private static final String TOOL = "gemma";
    private static final String EMAIL = Settings.getString( "gemma.support.email" );

    private static final long TIMEOUT_AUTHENTICATED_MS = 100;
    private static final long TIMEOUT_ANONYMOUS_MS = 333;

    private static long lastCall = 0L;

    @FunctionalInterface
    public interface EntrezCall<T> extends Callable<T> {

        @Override
        T call() throws IOException;
    }

    /**
     * Coordinate calls to the Entrez API so that we always respect the recommended usage.
     * <p>
     * Refer to <a href="https://www.ncbi.nlm.nih.gov/books/NBK25497/">A General Introduction to the E-utilities</a> for
     * more information about usage policies.
     */
    public synchronized static <T> T doNicely( EntrezCall<T> task, @Nullable String apiKey ) throws IOException {
        long timeoutMs = StringUtils.isNotBlank( apiKey ) ? TIMEOUT_AUTHENTICATED_MS : TIMEOUT_ANONYMOUS_MS;
        long diff = System.currentTimeMillis() - lastCall;
        if ( diff < timeoutMs ) {
            try {
                log.debug( "Last Entrez API call occurred " + diff + " ms ago, waiting " + ( timeoutMs - diff ) + " ms..." );
                Thread.sleep( timeoutMs - diff );
            } catch ( InterruptedException e ) {
                Thread.currentThread().interrupt();
                throw new RuntimeException( e );
            }
        }
        try {
            return task.call();
        } finally {
            lastCall = System.currentTimeMillis();
        }
    }

    /**
     * Wrap a {@link SimpleRetryCallable} such that it will respect the recommended usage of the Entrez API.
     * @see #doNicely(EntrezCall, String)
     */
    public static <T> SimpleRetryCallable<T, IOException> retryNicely( SimpleRetryCallable<T, IOException> callable, @Nullable String apiKey ) {
        return ( ctx ) -> EntrezUtils.doNicely( () -> callable.call( ctx ), apiKey );
    }

    /**
     * Perform a search on an Entrez database.
     * <p>
     * Results must be subsequently retrieved with {@link #fetch(String, String, String, int, int, String, String)} or
     * {@link #summary(String, String, String, int, int, String, String)}. The query key and WebEnv values must be
     * extracted from the payload.
     */
    public static URL search( String db, String term, String retmode, @Nullable String apiKey ) {
        return createUrl( ESEARCH
                + "?db=" + urlEncode( db )
                + "&term=" + urlEncode( term )
                + "&retmode=" + urlEncode( retmode )
                + "&usehistory=y", apiKey );
    }

    /**
     * Summarize a previous {@link #search(String, String, String, String)} query.
     */
    public static URL summary( String db, String queryKey, String retmode, int retstart, int retmax, String webEnv, @Nullable String apiKey ) {
        return createUrl( ESUMMARY
                + "?db=" + urlEncode( db )
                + "&query_key=" + urlEncode( queryKey )
                + "&retmode=" + urlEncode( retmode )
                + "&retstart=" + retstart
                + "&retmax=" + retmax
                + "&WebEnv=" + urlEncode( webEnv ), apiKey );
    }

    /**
     * Retrieve a record from an Entrez database by ID.
     */
    public static URL fetchById( String db, String id, String retmode, String rettype, @Nullable String apiKey ) {
        return createUrl( EFETCH
                + "?db=" + urlEncode( db )
                + "&id=" + urlEncode( id )
                + "&retmode=" + urlEncode( retmode )
                + "&rettype=" + urlEncode( rettype ), apiKey );
    }

    /**
     * Retrieve the results of a previous {@link #search(String, String, String, String)} query.
     */
    public static URL fetch( String db, String queryKey, String retmode, int retstart, int retmax, String webEnv, @Nullable String apiKey ) {
        return createUrl( EFETCH
                + "?db=" + urlEncode( db )
                + "&query_key=" + urlEncode( queryKey )
                + "&retmode=" + urlEncode( retmode )
                + "&retstart=" + retstart
                + "&retmax=" + retmax
                + "&WebEnv=" + urlEncode( webEnv ), apiKey );
    }

    /**
     * @deprecated this is not a documented Entrez API and should not be used.
     */
    @Deprecated
    public static URL query( String db, String term, String cmd, @Nullable String apiKey ) {
        return createUrl( EQUERY
                + "?db=" + urlEncode( db )
                + "&term=" + urlEncode( term )
                + "&cmd=" + cmd, apiKey );
    }

    private static URL createUrl( String url, @Nullable String apiKey ) {
        try {
            return new URL( url
                    + "&tool=" + urlEncode( TOOL )
                    + "&email=" + urlEncode( EMAIL )
                    + ( StringUtils.isNotBlank( apiKey ) ? "&api_key=" + urlEncode( apiKey ) : "" ) );
        } catch ( MalformedURLException e ) {
            throw new RuntimeException( e );
        }
    }

    private static String urlEncode( String s ) {
        try {
            return URLEncoder.encode( s, StandardCharsets.UTF_8.name() );
        } catch ( UnsupportedEncodingException e ) {
            throw new RuntimeException( e );
        }
    }
}
