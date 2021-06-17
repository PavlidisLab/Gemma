package ubic.gemma.persistence.persister;

import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.persistence.util.ArrayDesignsForExperimentCache;

public interface BioAssayPersister extends Persister<BioAssay> {
    <S extends BioAssay> S persistBioAssay( S assay, ArrayDesignsForExperimentCache c );

    void fillInBioAssayAssociations( BioAssay bioAssay, ArrayDesignsForExperimentCache c );
}
