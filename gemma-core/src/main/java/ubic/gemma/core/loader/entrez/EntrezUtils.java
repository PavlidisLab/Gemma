package ubic.gemma.core.loader.entrez;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;
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
    public static final String ELINK = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/elink.fcgi";

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
     * Results must be subsequently retrieved with {@link #fetch(String, EntrezQuery, EntrezRetmode, String, int, int, String)} or
     * {@link #summary(String, EntrezQuery, EntrezRetmode, int, int, String)}. The query key and WebEnv values must be
     * extracted from the payload.
     */
    public static URL search( String db, String term, EntrezRetmode retmode, @Nullable String apiKey ) {
        return createUrl( ESEARCH
                + "?db=" + urlEncode( db )
                + "&term=" + urlEncode( term )
                + "&retmode=" + urlEncode( retmode.getValue() )
                + "&usehistory=y", apiKey );
    }

    /**
     * Replay a previous {@link #search(String, String, EntrezRetmode, String)} query.
     */
    public static URL search( String db, EntrezQuery query, EntrezRetmode retmode, int retstart, int retmax, @Nullable String apiKey ) {
        Assert.isTrue( retstart >= 0 );
        Assert.isTrue( retmax > 0 );
        if ( retstart >= query.getTotalRecords() ) {
            throw new IndexOutOfBoundsException();
        }
        return createUrl( ESEARCH
                + "?db=" + urlEncode( db )
                + "&query_key=" + urlEncode( query.getQueryId() )
                + "&retmode=" + urlEncode( retmode.getValue() )
                + "&retstart=" + retstart
                + "&retmax=" + retmax
                + "&WebEnv=" + urlEncode( query.getCookie() )
                + "&usehistory=y", apiKey );
    }

    /**
     * Summarize a previous {@link #search(String, String, EntrezRetmode, String)} query.
     */
    public static URL summary( String db, EntrezQuery query, EntrezRetmode retmode, int retstart, int retmax, @Nullable String apiKey ) {
        Assert.isTrue( retstart >= 0 );
        Assert.isTrue( retmax > 0 );
        if ( retstart >= query.getTotalRecords() ) {
            throw new IndexOutOfBoundsException();
        }
        return createUrl( ESUMMARY
                + "?db=" + urlEncode( db )
                + "&query_key=" + urlEncode( query.getQueryId() )
                + "&retmode=" + urlEncode( retmode.getValue() )
                + "&retstart=" + retstart
                + "&retmax=" + retmax
                + "&WebEnv=" + urlEncode( query.getCookie() ), apiKey );
    }

    /**
     * Retrieve a record from an Entrez database by ID.
     */
    public static URL fetchById( String db, String id, EntrezRetmode retmode, String rettype, @Nullable String apiKey ) {
        return createUrl( EFETCH
                + "?db=" + urlEncode( db )
                + "&id=" + urlEncode( id )
                + "&retmode=" + urlEncode( retmode.getValue() )
                + "&rettype=" + urlEncode( rettype ), apiKey );
    }

    /**
     * Retrieve the results of a previous {@link #search(String, String, EntrezRetmode, String)} query.
     */
    public static URL fetch( String db, EntrezQuery query, EntrezRetmode retmode, String rettype, int retstart, int retmax, @Nullable String apiKey ) {
        Assert.isTrue( retstart >= 0 );
        Assert.isTrue( retmax > 0 );
        return createUrl( EFETCH
                + "?db=" + urlEncode( db )
                + "&query_key=" + urlEncode( query.getQueryId() )
                + "&retmode=" + urlEncode( retmode.getValue() )
                + "&rettype=" + urlEncode( rettype )
                + "&retstart=" + retstart
                + "&retmax=" + retmax
                + "&WebEnv=" + urlEncode( query.getCookie() ), apiKey );
    }

    /**
     * Retrieve related records from an Entrez database by ID.
     * @param db     target database
     * @param dbfrom source database, the ID must come from this database
     * @param id     the ID to link from
     * @param cmd    the command to use, e.g. "neighbor"
     */
    public static URL linkById( String db, String dbfrom, String id, String cmd, EntrezRetmode retmode, @Nullable String apiKey ) {
        return createUrl( ELINK + "?db=" + urlEncode( db )
                + "&dbfrom=" + urlEncode( dbfrom )
                + "&id=" + urlEncode( id )
                + "&cmd=" + urlEncode( cmd )
                + "&retmode=" + urlEncode( retmode.getValue() ), apiKey );
    }

    /**
     * Retrieve related records from an Entrez database from a previous {@link #search(String, String, EntrezRetmode, String)} query.
     */
    public static URL link( String db, String dbfrom, EntrezQuery query, String cmd, EntrezRetmode retmode, @Nullable String apiKey ) {
        return createUrl( ELINK + "?db=" + urlEncode( db )
                + "&dbfrom=" + urlEncode( dbfrom )
                + "&query_key=" + urlEncode( query.getQueryId() )
                + "&retmode=" + urlEncode( retmode.getValue() )
                + "&cmd=" + urlEncode( cmd )
                + "&WebEnv=" + urlEncode( query.getCookie() ), apiKey );
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
