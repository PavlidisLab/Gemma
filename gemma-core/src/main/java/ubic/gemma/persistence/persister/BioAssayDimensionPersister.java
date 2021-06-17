package ubic.gemma.persistence.persister;

import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.persistence.util.ArrayDesignsForExperimentCache;

public interface BioAssayDimensionPersister extends Persister<BioAssayDimension> {
    BioAssayDimension persistBioAssayDimension( BioAssayDimension bioAssayDimension,
            ArrayDesignsForExperimentCache c );
}
