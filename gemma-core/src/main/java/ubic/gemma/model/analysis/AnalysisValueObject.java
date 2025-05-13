package ubic.gemma.model.analysis;

import ubic.gemma.model.common.IdentifiableValueObject;
import ubic.gemma.model.util.ModelUtils;

public abstract class AnalysisValueObject<T extends Analysis> extends IdentifiableValueObject<T> {

    private String name;

    private ProtocolValueObject protocol;

    protected AnalysisValueObject() {
        super();
    }

    protected AnalysisValueObject( T analysis ) {
        super( analysis );
        this.name = analysis.getName();
        if ( analysis.getProtocol() != null && ModelUtils.isInitialized( analysis.getProtocol() ) ) {
            this.protocol = new ProtocolValueObject( analysis.getProtocol() );
        }
    }

    public String getName() {
        return name;
    }

    public void setName( String name ) {
        this.name = name;
    }

    public ProtocolValueObject getProtocol() {
        return protocol;
    }

    public void setProtocol( ProtocolValueObject protocol ) {
        this.protocol = protocol;
    }
}
