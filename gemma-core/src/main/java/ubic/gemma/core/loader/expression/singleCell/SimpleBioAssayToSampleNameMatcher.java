package ubic.gemma.core.loader.expression.singleCell;

import ubic.gemma.model.expression.bioAssay.BioAssay;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static org.apache.commons.lang3.StringUtils.normalizeSpace;

/**
 * A simple strategy for matching BioAssay to sample name.
 * @author poirigui
 */
public class SimpleBioAssayToSampleNameMatcher implements BioAssayToSampleNameMatcher {

    @Override
    public Set<BioAssay> match( Collection<BioAssay> bas, String n ) {
        Set<BioAssay> results = new HashSet<>( 1 ); // ideally, should only match 1 element

        // BioAssay name
        bas.stream().filter( ba -> matchName( ba.getName(), n ) ).forEach( results::add );

        // BioAssay name (case-insensitive)
        if ( results.isEmpty() ) {
            bas.stream().filter( ba -> matchNameIgnoreCase( ba.getName(), n ) ).forEach( results::add );
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
}
