package ubic.gemma.persistence.persister;

import ubic.gemma.model.expression.experiment.ExperimentalFactor;

public interface ExperimentalFactorPersister extends Persister<ExperimentalFactor> {
    void fillInExperimentalFactorAssociations( ExperimentalFactor experimentalFactor );
}
