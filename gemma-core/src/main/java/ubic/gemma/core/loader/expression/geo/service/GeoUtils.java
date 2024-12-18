package ubic.gemma.core.loader.expression.geo.service;

import java.net.MalformedURLException;
import java.net.URL;

public class GeoUtils {

    private static final String GEO_QUERY_URL = "https://www.ncbi.nlm.nih.gov/geo/query";
    private static final String GEO_FTP_VIA_HTTPS_BASE_URL = "https://ftp.ncbi.nlm.nih.gov/geo";
    private static final String GEO_FTP_BASE_URL = "ftp://ftp.ncbi.nlm.nih.gov/geo";

    /**
     * Obtain a URL for a series family file.
     */
    public static URL getUrlForSeriesFamily( String geoAccession, GeoSource source, GeoFormat format ) {
        if ( source == GeoSource.QUERY ) {
            String form;
            if ( format == GeoFormat.SOFT ) {
                form = "text";
            } else if ( format == GeoFormat.MINIML ) {
                form = "xml";
            } else {
                throw new UnsupportedOperationException( "" );
            }
            try {
                return new URL( GEO_QUERY_URL + "/acc.cgi?acc=" + geoAccession + "&targ=all&form=" + form + "&view=brief" );
            } catch ( MalformedURLException e ) {
                throw new RuntimeException( e );
            }
        } else if ( source == GeoSource.FTP || source == GeoSource.FTP_VIA_HTTPS ) {
            String baseUrl;
            if ( source == GeoSource.FTP ) {
                baseUrl = GEO_FTP_BASE_URL;
            } else {
                baseUrl = GEO_FTP_VIA_HTTPS_BASE_URL;
            }
            String formatDir, ext;
            if ( format == GeoFormat.SOFT ) {
                formatDir = "soft";
                ext = ".soft.gz";
            } else if ( format == GeoFormat.MINIML ) {
                formatDir = "miniml";
                ext = ".xml.tgz";
            } else {
                throw new UnsupportedOperationException( "Unsupported GEO format: " + format + "." );
            }
            try {
                return new URL( baseUrl + "/series/" + formShortenedFtpDirName( geoAccession ) + "/" + geoAccession + "/" + formatDir + "/" + geoAccession + "_family" + ext );
            } catch ( MalformedURLException e ) {
                throw new RuntimeException( e );
            }
        } else {
            throw new UnsupportedOperationException( "Unsupported source for GEO data: " + source + "." );
        }
    }

    /**
     * Form the shortened FTP directory name for a given GEO accession.
     */
    public static String formShortenedFtpDirName( String geoAccession ) {
        return geoAccession.substring( 0, Math.max( geoAccession.length() - 3, 3 ) ) + "nnn";
    }
}
