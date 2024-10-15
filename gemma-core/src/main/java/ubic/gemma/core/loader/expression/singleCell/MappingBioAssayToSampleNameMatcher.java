package ubic.gemma.core.loader.expression.singleCell;

import org.springframework.util.Assert;
import ubic.gemma.model.expression.bioAssay.BioAssay;

import java.util.*;

/**
 * A BioAssay-to-sample-name matcher that first maps sample names before matching them.
 * @author poirigui
 */
public class MappingBioAssayToSampleNameMatcher implements BioAssayToSampleNameMatcher {

    private final BioAssayToSampleNameMatcher delegate;
    private final Map<String, String> sampleNameToBioAssayName;

    /**
     * @param delegate      a matcher that performs the underlying comparison of BA IDs
     * @param bioAssayNames the BioAssay identifiers to use
     * @param sampleNames   the corresponding sample identifiers to use
     */
    public MappingBioAssayToSampleNameMatcher( BioAssayToSampleNameMatcher delegate, String[] bioAssayNames, String[] sampleNames ) {
        Assert.isTrue( bioAssayNames.length == sampleNames.length );
        this.delegate = delegate;
        this.sampleNameToBioAssayName = new HashMap<>();
        for ( int i = 0; i < bioAssayNames.length; i++ ) {
            this.sampleNameToBioAssayName.put( sampleNames[i], bioAssayNames[i] );
        }
    }

    @Override
    public Set<BioAssay> match( Collection<BioAssay> bioAssays, String sampleNameFromData ) {
        if ( sampleNameToBioAssayName.containsKey( sampleNameFromData ) ) {
            return delegate.match( bioAssays, sampleNameToBioAssayName.get( sampleNameFromData ) );
        } else {
            return Collections.emptySet();
        }
    }
}
