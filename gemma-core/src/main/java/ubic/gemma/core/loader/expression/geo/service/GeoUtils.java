package ubic.gemma.core.loader.expression.geo.service;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class GeoUtils {

    private static final String GEO_QUERY_URL = "https://www.ncbi.nlm.nih.gov/geo/query";
    private static final String GEO_DOWNLOAD_URL = "https://www.ncbi.nlm.nih.gov/geo/download";
    private static final String GEO_BROWSE_URL = "https://www.ncbi.nlm.nih.gov/geo/browse/";
    private static final String GEO_FTP_VIA_HTTPS_BASE_URL = "https://ftp.ncbi.nlm.nih.gov/geo";
    private static final String GEO_FTP_BASE_URL = "ftp://ftp.ncbi.nlm.nih.gov/geo";

    /**
     * Obtain a URL for GEO entry.
     */
    public static URL getUrl( String geoAccession, GeoSource source, GeoFormat format, GeoScope scope, GeoAmount amount ) {
        if ( source == GeoSource.DIRECT ) {
            String form;
            if ( format == GeoFormat.SOFT ) {
                form = "text";
            } else if ( format == GeoFormat.MINIML ) {
                form = "xml";
            } else if ( format == GeoFormat.HTML ) {
                form = null;
            } else {
                throw new UnsupportedOperationException( "Unsupported GEO source: " + source + " for the direct GEO source." );
            }
            String targ;
            switch ( scope ) {
                case SELF:
                    // in the HTML view, the default is self
                    targ = format == GeoFormat.HTML ? null : "self";
                    break;
                case SAMPLES:
                    targ = "samples";
                    break;
                case PLATFORM:
                    targ = "platform";
                    break;
                case SERIES:
                    targ = "series";
                    break;
                case FAMILY:
                    targ = "all";
                    break;
                default:
                    throw new UnsupportedOperationException( "Unsupported GEO scope: " + scope + " for the direct GEO source." );
            }
            String view;
            if ( format == GeoFormat.HTML ) {
                if ( amount == GeoAmount.BRIEF ) {
                    // in the HTML view, the default is brief
                    view = null;
                } else if ( amount == GeoAmount.QUICK ) {
                    view = "quick";
                } else {
                    throw new UnsupportedOperationException( "Unsupported GEO amount: " + amount + " for the direct GEO source with HTML format." );
                }
            } else {
                switch ( amount ) {
                    case BRIEF:
                        view = "brief";
                        break;
                    case QUICK:
                        view = "quick";
                        break;
                    case FULL:
                        view = "full";
                        break;
                    case DATA:
                        view = "data";
                        break;
                    default:
                        throw new UnsupportedOperationException( "Unsupported GEO amount: " + amount + " for the direct GEO source." );
                }
            }
            try {
                return new URL( GEO_QUERY_URL + "/acc.cgi?acc=" + geoAccession
                        + ( targ != null ? "&targ=" + targ : "" )
                        + ( form != null ? "&form=" + form : "" )
                        + ( view != null ? "&view=" + view : "" ) );
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
                throw new UnsupportedOperationException( "Unsupported GEO format: " + format + " for the FTP GEO source." );
            }
            if ( scope != GeoScope.FAMILY ) {
                throw new UnsupportedOperationException( "Only family scope is supported for the FTP GEO source." );
            }
            // we support all amount because the FTP files are complete
            try {
                return new URL( baseUrl + "/series/" + formShortenedFtpDirName( geoAccession ) + "/" + geoAccession + "/" + formatDir + "/" + geoAccession + "_family" + ext );
            } catch ( MalformedURLException e ) {
                throw new RuntimeException( e );
            }
        } else {
            throw new UnsupportedOperationException( "Unsupported source for GEO data: " + source + "." );
        }
    }

    public static URL getUrlForBrowsing( GeoRecordType recordType, int start, int pageSize, GeoFormat format ) {
        String recordTypeS;
        switch ( recordType ) {
            case SERIES:
                recordTypeS = "series";
                break;
            case SAMPLE:
                recordTypeS = "samples";
                break;
            case PLATFORM:
                recordTypeS = "platforms";
                break;
            default:
                throw new UnsupportedOperationException( "Unsupported record type for browsing: " + recordType + "." );
        }
        String formatS;
        switch ( format ) {
            case TSV:
                formatS = "tsv";
                break;
            case CSV:
                formatS = "csv";
                break;
            default:
                throw new UnsupportedOperationException( "Unsupported format for browsing: " + format + "." );
        }
        try {
            return new URL( GEO_BROWSE_URL + "?view=" + recordTypeS + "&zsort=date&mode=" + formatS + "&page=" + start + "&display=" + pageSize );
        } catch ( MalformedURLException e ) {
            throw new RuntimeException( e );
        }
    }

    public static URL getUrlForSupplementaryMaterial( GeoRecordType recordType, String geoAccession, String filename, GeoSource source ) {
        if ( source == GeoSource.DIRECT ) {
            try {
                return new URL( GEO_DOWNLOAD_URL + "/?acc=" + geoAccession + "&format=file&file=" + urlEncode( filename ) );
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
            try {
                return new URL( baseUrl + "/" + formFtpDirName( recordType ) + "/" + formShortenedFtpDirName( geoAccession ) + "/" + geoAccession + "/suppl/" + urlEncode( filename ) );
            } catch ( MalformedURLException e ) {
                throw new RuntimeException( e );
            }
        } else {
            throw new UnsupportedOperationException( "Unsupported GEO source: " + source + "." );
        }
    }

    /**
     * Form the FTP directory name for the given GEO record type.
     */
    public static String formFtpDirName( GeoRecordType recordType ) {
        switch ( recordType ) {
            case SERIES:
                return "series";
            case SAMPLE:
                return "samples";
            case PLATFORM:
                return "platforms";
            case DATASET:
                return "datasets";
            default:
                throw new UnsupportedOperationException( "Unsupported record type for supplementary material: " + recordType + "." );
        }
    }

    /**
     * Form the shortened FTP directory name for a given GEO accession.
     */
    public static String formShortenedFtpDirName( String geoAccession ) {
        return geoAccession.substring( 0, Math.max( geoAccession.length() - 3, 3 ) ) + "nnn";
    }

    private static String urlEncode( String s ) {
        try {
            return URLEncoder.encode( s, StandardCharsets.UTF_8.name() );
        } catch ( UnsupportedEncodingException e ) {
            throw new RuntimeException( e );
        }
    }
}
