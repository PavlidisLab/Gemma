package ubic.gemma.core.loader.expression.singleCell;

import ubic.gemma.model.expression.bioAssay.BioAssay;

import java.util.Collection;
import java.util.Set;

/**
 * Strategy used for comparing {@link BioAssay} to sample names from the data.
 * @author poirigui
 */
@FunctionalInterface
public
interface BioAssayToSampleNameMatcher {

    Set<BioAssay> match( Collection<BioAssay> bioAssays, String sampleNameFromData );
}
