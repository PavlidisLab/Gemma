package ubic.gemma.persistence.persister;

import ubic.gemma.model.expression.experiment.FactorValue;

public interface FactorValuePersister extends Persister<FactorValue> {
    void fillInFactorValueAssociations( FactorValue factorValue );
}
