package ubic.gemma.web.util;

import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.ExternalDatabaseValueObject;
import ubic.gemma.model.common.description.ExternalDatabases;

import org.springframework.lang.Nullable;

/**
 * Extends {@link ubic.gemma.core.loader.util.ExternalDatabaseUtils} with web-specific utilities.
 *
 * @author poirigui
 */
public class ExternalDatabaseWebUtils {

    /**
     * Obtain the path to the logo image for a given external database.
     */
    @Nullable
    public static String getLogo( ExternalDatabase externalDatabase ) {
        return getLogo( externalDatabase.getName() );
    }

    /**
     * Obtain the path to the logo image for a given external database.
     */
    @Nullable
    public static String getLogo( ExternalDatabaseValueObject externalDatabase ) {
        return getLogo( externalDatabase.getName() );
    }

    @Nullable
    private static String getLogo( String databaseName ) {
        if ( ExternalDatabases.GEO.equalsIgnoreCase( databaseName ) ) {
            return "/images/logo/geo-logo.png";
        } else if ( ExternalDatabases.ARRAY_EXPRESS.equalsIgnoreCase( databaseName ) ) {
            return "/images/logo/arrayexpress-logo.png";
        } else if ( ExternalDatabases.BIO_STUDIES.equalsIgnoreCase( databaseName ) ) {
            return "/images/logo/biostudies-logo.png";
        } else if ( ExternalDatabases.SRA.equalsIgnoreCase( databaseName ) ) {
            return "/images/logo/sra-logo.png";
        } else if ( ExternalDatabases.CELLXGENE.equalsIgnoreCase( databaseName ) ) {
            return "/images/logo/cellxgene-logo-inverted.png";
        } else if ( ExternalDatabases.PUBMED.equalsIgnoreCase( databaseName ) ) {
            return "/images/logo/pubmed-logo-blue.png";
        } else if ( ExternalDatabases.ARXIV.equalsIgnoreCase( databaseName ) ) {
            return "/images/logo/arxiv-logo.png";
        } else if ( ExternalDatabases.BIORXIV.equalsIgnoreCase( databaseName ) ) {
            return "/images/logo/biorxiv-logo.png";
        } else if ( ExternalDatabases.SYNAPSE.equalsIgnoreCase( databaseName ) ) {
            return "/images/logo/synapse-logo.png";
        } else if ( ExternalDatabases.ZENODO.equalsIgnoreCase( databaseName ) ) {
            return "/images/logo/biorxiv-logo.png";
        } else {
            return null;
        }
    }
}
