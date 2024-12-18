package ubic.gemma.core.loader.expression.geo.service;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

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
                return new URL( GEO_QUERY_URL + "/acc.cgi?acc=" + urlEncode( geoAccession ) + "&targ=all&form=" + form + "&view=brief" );
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
            String dir, ext;
            if ( format == GeoFormat.SOFT ) {
                dir = "soft";
                ext = ".soft.gz";
            } else if ( format == GeoFormat.MINIML ) {
                dir = "miniml";
                ext = ".xml.tgz";
            } else {
                throw new UnsupportedOperationException( "Unsupported GEO format: " + format + "." );
            }
            try {
                return new URL( baseUrl + formFtpDir( GeoRecordType.SERIES, geoAccession ) + "/" + dir + "/" + geoAccession + "_family" + ext );
            } catch ( MalformedURLException e ) {
                throw new RuntimeException( e );
            }
        } else {
            throw new UnsupportedOperationException( "Unsupported source for GEO data: " + source + "." );
        }
    }

    /**
     * Form the FTP directory name for a given GEO accession.
     * <p>
     * The path is relative to the GEO base FTP URL (i.e. <a href="https://ftp.ncbi.nlm.nih.gov/geo">https://ftp.ncbi.nlm.nih.gov/geo</a>).
     */
    public static String formFtpDir( GeoRecordType recordType, String geoAccession ) {
        String dir;
        if ( recordType == GeoRecordType.SERIES ) {
            dir = "/series";
        } else if ( recordType == GeoRecordType.PLATFORM ) {
            dir = "/platforms";
        } else if ( recordType == GeoRecordType.SAMPLE ) {
            dir = "/samples";
        } else if ( recordType == GeoRecordType.DATASET ) {
            dir = "/datasets";
        } else {
            throw new UnsupportedOperationException( "Unsupported record type for GEO data: " + recordType + "." );
        }
        return dir + "/" + geoAccession.substring( 0, Math.max( geoAccession.length() - 3, 3 ) ) + "nnn/" + urlEncode( geoAccession );
    }

    private static String urlEncode( String s ) {
        try {
            return URLEncoder.encode( s, StandardCharsets.UTF_8.name() );
        } catch ( UnsupportedEncodingException e ) {
            throw new RuntimeException( e );
        }
    }
}
