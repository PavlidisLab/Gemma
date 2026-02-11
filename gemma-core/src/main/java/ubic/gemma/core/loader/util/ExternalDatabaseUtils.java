package ubic.gemma.core.loader.util;

import io.micrometer.common.util.StringUtils;
import ubic.gemma.core.loader.entrez.pubmed.PubMedUtils;
import ubic.gemma.core.loader.expression.arrayExpress.ArrayExpressUtils;
import ubic.gemma.core.loader.expression.cellxgene.CellXGeneUtils;
import ubic.gemma.core.loader.expression.geo.service.*;
import ubic.gemma.core.loader.expression.sra.SraUtils;
import ubic.gemma.core.loader.expression.synapse.SynapseUtils;
import ubic.gemma.core.loader.expression.ucsc.cellbrowser.UcscCellBrowserUtils;
import ubic.gemma.core.loader.expression.zenodo.ZenodoUtils;
import ubic.gemma.core.loader.genome.gene.ncbi.NcbiGeneUtils;
import ubic.gemma.core.ontology.providers.GeneOntologyUtils;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.DatabaseEntryValueObject;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.ExternalDatabases;

import javax.annotation.Nullable;

/**
 * Utilities for working with various external databases.
 * <p>
 * This delegates to specific utilities for each supported database.
 *
 * @author poirigui
 * @see GeoUtils
 * @see SraUtils
 * @see ArrayExpressUtils
 * @see CellXGeneUtils
 */
public class ExternalDatabaseUtils {

    /**
     * Obtain a URI for a given database entry.
     * <p>
     * The returned value is not necessarily an HTTP/HTTPS URL.
     */
    @Nullable
    public static String getUri( DatabaseEntry accession ) {
        if ( StringUtils.isNotBlank( accession.getUri() ) ) {
            return accession.getUri();
        }
        return getUri( accession.getAccession(), accession.getExternalDatabase().getName() );
    }

    /**
     * Obtain a URI for a given database entry.
     * <p>
     * The returned value is not necessarily an HTTP/HTTPS URL.
     */
    @Nullable
    public static String getUri( DatabaseEntryValueObject accession ) {
        if ( StringUtils.isNotBlank( accession.getUri() ) ) {
            return accession.getUri();
        }
        return getUri( accession.getAccession(), accession.getExternalDatabase().getName() );
    }

    /**
     * Obtain a URI for a given database entry.
     * <p>
     * The returned value is not necessarily an HTTP/HTTPS URL.
     */
    @Nullable
    private static String getUri( String accession, String databaseName ) {
        if ( ExternalDatabases.GEO.equalsIgnoreCase( databaseName ) ) {
            return GeoUtils.getUri( accession, GeoSource.DIRECT, GeoFormat.HTML, GeoScope.SELF, GeoAmount.BRIEF );
        } else if ( ExternalDatabases.SRA.equalsIgnoreCase( databaseName ) ) {
            return SraUtils.getUri( accession );
        } else if ( ExternalDatabases.ARRAY_EXPRESS.equalsIgnoreCase( databaseName ) ) {
            return ArrayExpressUtils.getUri( accession );
        } else if ( ExternalDatabases.CELLXGENE.equalsIgnoreCase( databaseName ) ) {
            return CellXGeneUtils.getDatasetUri( accession );
        } else if ( ExternalDatabases.PUBMED.equalsIgnoreCase( databaseName ) ) {
            return PubMedUtils.getUri( accession );
        } else if ( ExternalDatabases.GO.equalsIgnoreCase( databaseName ) ) {
            return GeneOntologyUtils.getUri( accession );
        } else if ( ExternalDatabases.UCSC_CELL_BROWSER.equalsIgnoreCase( databaseName ) ) {
            return UcscCellBrowserUtils.getDatasetUri( accession );
        } else if ( ExternalDatabases.SYNAPSE.equalsIgnoreCase( databaseName ) ) {
            return SynapseUtils.getUri( accession );
        } else if ( ExternalDatabases.ZENODO.equalsIgnoreCase( databaseName ) ) {
            return ZenodoUtils.getUri( accession );
        } else if ( ExternalDatabases.GENE.equalsIgnoreCase( databaseName ) ) {
            return NcbiGeneUtils.getUri( accession );
        } else {
            return null;
        }
    }

    /**
     * Obtain a URI for a given external database.
     * <p>
     * The returned value is not necessarily an HTTP/HTTPS URL.
     */
    @Nullable
    public static String getUri( ExternalDatabase externalDatabase ) {
        return externalDatabase.getWebUri();
    }

    /**
     * Obtain a label for a given database entry.
     * <p>
     * This is usually the accession, but it may be {@code null} if the database accession is not meaningful to users
     * (e.g. CELLxGENE dataset IDs are UUIDs).
     */
    @Nullable
    public static String getLabel( DatabaseEntry de ) {
        if ( ExternalDatabases.CELLXGENE.equalsIgnoreCase( de.getExternalDatabase().getName() ) ) {
            return null;
        } else {
            return de.getAccession();
        }
    }
}
