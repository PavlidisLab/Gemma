package ubic.gemma.model.analysis;

import lombok.Getter;
import lombok.Setter;
import ubic.gemma.model.common.IdentifiableValueObject;
import ubic.gemma.model.util.ModelUtils;

@Getter
@Setter
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
}
