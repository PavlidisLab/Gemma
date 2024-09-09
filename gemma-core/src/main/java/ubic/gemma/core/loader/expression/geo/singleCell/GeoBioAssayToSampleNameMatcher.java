package ubic.gemma.core.loader.expression.geo.singleCell;

import ubic.gemma.core.loader.expression.singleCell.BioAssayToSampleNameMatcher;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabases;
import ubic.gemma.model.expression.bioAssay.BioAssay;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static org.apache.commons.lang3.StringUtils.normalizeSpace;

/**
 * Strategy to match a {@link BioAssay} against a sample name provided by GEO.
 * <p>
 * Ideally, the provided sample name is a GSM accession, but in some cases it might originate from a supplementary file,
 * so this has some fallback on other metadata.
 * @author poirigui
 */
public class GeoBioAssayToSampleNameMatcher implements BioAssayToSampleNameMatcher {

    @Override
    public Set<BioAssay> match( Collection<BioAssay> bas, String n ) {
        Set<BioAssay> results = new HashSet<>( 1 ); // ideally, should only match 1 element

        // BioAssay GEO accession (canonical)
        bas.stream().filter( ba -> matchGeoAccession( ba.getAccession(), n ) ).forEach( results::add );

        // BioAssay name
        if ( results.isEmpty() ) {
            bas.stream().filter( ba -> matchName( ba.getName(), n ) ).forEach( results::add );
        }

        // BioAssay name (case-insensitive)
        if ( results.isEmpty() ) {
            bas.stream().filter( ba -> matchNameIgnoreCase( ba.getName(), n ) ).forEach( results::add );
        }

        // BioMaterial GEO accession
        if ( results.isEmpty() ) {
            bas.stream().filter( ba -> matchGeoAccession( ba.getSampleUsed().getExternalAccession(), n ) ).forEach( results::add );
        }

        // BioMaterial name
        if ( results.isEmpty() ) {
            bas.stream().filter( ba -> matchName( ba.getSampleUsed().getName(), n ) ).forEach( results::add );
        }

        // BioMaterial name (case-insensitive)
        if ( results.isEmpty() ) {
            bas.stream().filter( ba -> matchNameIgnoreCase( ba.getSampleUsed().getName(), n ) ).forEach( results::add );
        }

        return results;
    }

    private boolean matchName( String a, String b ) {
        return normalizeSpace( a ).equals( normalizeSpace( b ) );
    }

    private boolean matchNameIgnoreCase( String a, String b ) {
        return normalizeSpace( a ).equalsIgnoreCase( normalizeSpace( b ) );
    }

    private boolean matchGeoAccession( @Nullable DatabaseEntry accession, String n ) {
        return accession != null && accession.getExternalDatabase().getName().equals( ExternalDatabases.GEO )
                && accession.getAccession().equals( n );
    }
}
