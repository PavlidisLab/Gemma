package ubic.gemma.model.analysis;

import ubic.gemma.model.IdentifiableValueObject;

public abstract class AnalysisValueObject<T extends Analysis> extends IdentifiableValueObject<T> {

    protected AnalysisValueObject() {
        super();
    }

    protected AnalysisValueObject( T analysis ) {
        super( analysis );
    }
}
