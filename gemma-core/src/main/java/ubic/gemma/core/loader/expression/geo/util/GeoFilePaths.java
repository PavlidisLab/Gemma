package ubic.gemma.core.loader.expression.geo.util;

import ubic.gemma.core.loader.expression.geo.GeoMetadataFormat;

/**
 * Utilities for accessing GEO data via their FTP server.
 * @author poirigui
 */
public class GeoFilePaths {

    public static String getSeriesFamilyFilePath( String accession, GeoMetadataFormat format ) {
        if ( format == GeoMetadataFormat.MINIML ) {
            return "geo/series/" + shortenAccession( accession ) + "/" + accession + "/miniml/" + accession + "_family.xml.tgz";
        } else if ( format == GeoMetadataFormat.SOFT ) {
            return "geo/series/" + shortenAccession( accession ) + "/" + accession + "/soft/" + accession + "_family.soft.gz";
        } else {
            throw new UnsupportedOperationException( "Unsupported GEO metadata format " + format );
        }
    }

    public static String getSeriesRawDataFilePath( String accession ) {
        return "geo/series/" + shortenAccession( accession ) + "/" + accession + "/suppl/" + accession + "_RAW.tar";
    }

    public static String getDatasetFilePath( String accession, GeoMetadataFormat format ) {
        if ( format == GeoMetadataFormat.SOFT ) {
            return "geo/datasets/" + shortenAccession( accession ) + "/" + accession + "/soft/" + accession + ".soft.gz";
        } else {
            throw new UnsupportedOperationException( "Unsupported GEO metadata format " + format );
        }
    }

    public static String getPlatformFamilyFilePath( String accession, GeoMetadataFormat format ) {
        if ( format == GeoMetadataFormat.SOFT ) {
            return "geo/platforms/" + shortenAccession( accession ) + "/" + accession + "/soft/" + accession + "_family.soft.gz";
        } else {
            throw new UnsupportedOperationException( "Unsupported GEO metadata format " + format );
        }
    }

    private static String shortenAccession( String accession ) {
        return accession.substring( 0, Math.max( accession.length() - 3, 3 ) ) + "nnn";
    }
}
