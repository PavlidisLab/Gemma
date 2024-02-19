package ubic.gemma.model.analysis;

import ubic.gemma.model.IdentifiableValueObject;

public abstract class AnalysisValueObject<T extends Analysis> extends IdentifiableValueObject<T> {

    private ProtocolValueObject protocol;

    protected AnalysisValueObject() {
        super();
    }

    protected AnalysisValueObject( T analysis ) {
        super( analysis );
        if ( analysis.getProtocol() != null ) {
            this.protocol = new ProtocolValueObject( analysis.getProtocol() );
        }
    }

    public ProtocolValueObject getProtocol() {
        return protocol;
    }

    public void setProtocol( ProtocolValueObject protocol ) {
        this.protocol = protocol;
    }
}
