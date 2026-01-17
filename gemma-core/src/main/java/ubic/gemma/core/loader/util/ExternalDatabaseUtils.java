package ubic.gemma.core.loader.util;

import ubic.gemma.core.loader.entrez.pubmed.PubMedUtils;
import ubic.gemma.core.loader.expression.arrayExpress.ArrayExpressUtils;
import ubic.gemma.core.loader.expression.cellxgene.CellXGeneUtils;
import ubic.gemma.core.loader.expression.geo.service.*;
import ubic.gemma.core.loader.expression.sra.SraUtils;
import ubic.gemma.model.common.description.DatabaseEntry;
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
        if ( ExternalDatabases.GEO.equalsIgnoreCase( accession.getExternalDatabase().getName() ) ) {
            return GeoUtils.getUrl( accession.getAccession(), GeoSource.DIRECT, GeoFormat.HTML, GeoScope.SELF, GeoAmount.BRIEF );
        } else if ( ExternalDatabases.SRA.equalsIgnoreCase( accession.getExternalDatabase().getName() ) ) {
            return SraUtils.getUrl( accession.getAccession() );
        } else if ( ExternalDatabases.ARRAY_EXPRESS.equalsIgnoreCase( accession.getExternalDatabase().getName() ) ) {
            return ArrayExpressUtils.getUrl( accession.getAccession() );
        } else if ( ExternalDatabases.CELLXGENE.equalsIgnoreCase( accession.getExternalDatabase().getName() ) ) {
            return CellXGeneUtils.getDatasetUrl( accession.getAccession() );
        } else if ( ExternalDatabases.PUBMED.equalsIgnoreCase( accession.getExternalDatabase().getName() ) ) {
            return PubMedUtils.getUrl( accession.getAccession() );
        } else if ( ExternalDatabases.GO.equalsIgnoreCase( accession.getExternalDatabase().getName() ) ) {
            try {
                return new URL( "https://amigo.geneontology.org/amigo/term/" + urlEncode( accession.getAccession() ) );
            } catch ( MalformedURLException e ) {
                throw new RuntimeException( e );
            }
        } else {
            return null;
        }
    }
}
