package ubic.gemma.core.loader.util.mapper;

import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.experiment.BioAssaySet;

/**
 * Strategy used for comparing {@link BioAssay} to sample names from the data.
 * @author poirigui
 */
public interface BioAssayMapper extends EntityMapper<BioAssay> {

    default StatefulEntityMapper<BioAssay> forCandidates( BioAssaySet bioAssaySet ) {
        return forCandidates( bioAssaySet.getBioAssays() );
    }
}
