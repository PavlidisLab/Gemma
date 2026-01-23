package ubic.gemma.core.loader.util;

import ubic.gemma.core.loader.entrez.pubmed.PubMedUtils;
import ubic.gemma.core.loader.expression.arrayExpress.ArrayExpressUtils;
import ubic.gemma.core.loader.expression.cellxgene.CellXGeneUtils;
import ubic.gemma.core.loader.expression.geo.service.*;
import ubic.gemma.core.loader.expression.sra.SraUtils;
import ubic.gemma.core.loader.expression.ucsc.cellbrowser.UcscCellBrowserUtils;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.DatabaseEntryValueObject;
import ubic.gemma.model.common.description.ExternalDatabases;

import javax.annotation.Nullable;
import java.net.MalformedURLException;
import java.net.URL;

import static ubic.gemma.core.util.StringUtils.urlEncode;

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
     * Obtain a URL for a given database entry.
     */
    @Nullable
    public static URL getUrl( DatabaseEntry accession ) {
        if ( accession.getUri() != null ) {
            try {
                return new URL( accession.getAccession() );
            } catch ( MalformedURLException e ) {
                // ignore?
            }
        }
        return getUrl( accession.getAccession(), accession.getExternalDatabase().getName() );
    }

    @Nullable
    public static URL getUrl( DatabaseEntryValueObject accession ) {
        return getUrl( accession.getAccession(), accession.getExternalDatabase().getName() );
    }

    @Nullable
    private static URL getUrl( String accession, String databaseName ) {
        if ( ExternalDatabases.GEO.equalsIgnoreCase( databaseName ) ) {
            return GeoUtils.getUrl( accession, GeoSource.DIRECT, GeoFormat.HTML, GeoScope.SELF, GeoAmount.BRIEF );
        } else if ( ExternalDatabases.SRA.equalsIgnoreCase( databaseName ) ) {
            return SraUtils.getUrl( accession );
        } else if ( ExternalDatabases.ARRAY_EXPRESS.equalsIgnoreCase( databaseName ) ) {
            return ArrayExpressUtils.getUrl( accession );
        } else if ( ExternalDatabases.CELLXGENE.equalsIgnoreCase( databaseName ) ) {
            return CellXGeneUtils.getDatasetUrl( accession );
        } else if ( ExternalDatabases.PUBMED.equalsIgnoreCase( databaseName ) ) {
            return PubMedUtils.getUrl( accession );
        } else if ( ExternalDatabases.GO.equalsIgnoreCase( databaseName ) ) {
            try {
                return new URL( "https://amigo.geneontology.org/amigo/term/" + urlEncode( accession ) );
            } catch ( MalformedURLException e ) {
                throw new RuntimeException( e );
            }
        } else if ( ExternalDatabases.UCSC_CELL_BROWSER.equalsIgnoreCase( databaseName ) ) {
            try {
                return new URL( UcscCellBrowserUtils.getDatasetUrl( accession ) );
            } catch ( MalformedURLException e ) {
                throw new RuntimeException( e );
            }
        } else {
            return null;
        }
    }
}
