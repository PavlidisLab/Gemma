package ubic.gemma.core.loader.entrez;

import org.apache.commons.lang3.StringUtils;
import ubic.gemma.core.config.Settings;

import javax.annotation.Nullable;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Low-level utilities for generating Entrez URLs.
 * <p>
 * Read more about this in <a href="https://www.ncbi.nlm.nih.gov/books/NBK25499/">The E-utilities In-Depth: Parameters, Syntax and More</a>.
 * @author poirigui
 */
public class EntrezUtils {

    public static final String ESEARCH = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi";
    public static final String ESUMMARY = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi";
    public static final String EFETCH = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi";
    public static final String EQUERY = "https://www.ncbi.nlm.nih.gov/entrez/query.fcgi";

    private static final String TOOL = "gemma";
    private static final String EMAIL = Settings.getString( "gemma.support.email" );

    public static URL search( String db, String term, boolean useHistory, @Nullable String apiKey ) {
        return createUrl( ESEARCH
                + "?db=" + urlEncode( db )
                + "&term=" + urlEncode( term )
                + "&usehistory=" + ( useHistory ? "y" : "n" ), apiKey );
    }

    public static URL search( String db, String term, String retmode, String rettype, @Nullable String apiKey ) {
        return createUrl( ESEARCH
                + "?db=" + urlEncode( db )
                + "&term=" + urlEncode( term )
                + "&retmode=" + urlEncode( retmode )
                + "&rettype=" + urlEncode( rettype ), apiKey );
    }

    public static URL search( String db, String term, String retmode, String rettype, int retstart, int retmax, @Nullable String apiKey ) {
        return createUrl( ESEARCH
                + "?db=" + urlEncode( db )
                + "&term=" + urlEncode( term )
                + "&retstart=" + retstart
                + "&retmax=" + retmax
                + "&retmode=" + urlEncode( retmode )
                + "&rettype=" + urlEncode( rettype ), apiKey );
    }

    public static URL summary( String db, String queryKey, String retmode, int retstart, int retmax, String webEnv, @Nullable String apiKey ) {
        return createUrl( ESUMMARY
                + "?db=" + urlEncode( db )
                + "&query_key=" + urlEncode( queryKey )
                + "&retmode=" + urlEncode( retmode )
                + "&retstart=" + retstart
                + "&retmax=" + retmax
                + "&WebEnv=" + urlEncode( webEnv ), apiKey );
    }

    public static URL fetch( String db, String id, String retmode, String rettype, @Nullable String apiKey ) {
        return createUrl( EFETCH
                + "?db=" + urlEncode( db )
                + "&id=" + urlEncode( id )
                + "&retmode=" + urlEncode( retmode )
                + "&rettype=" + urlEncode( rettype ), apiKey );
    }

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
    public static URL query( String db, String query, String cmd, @Nullable String apiKey ) {
        return createUrl( EQUERY
                + "?db=" + urlEncode( db )
                + "&term=" + urlEncode( query )
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
