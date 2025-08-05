package ubic.gemma.model.analysis;

import lombok.Getter;
import lombok.Setter;
import ubic.gemma.model.common.DescribableValueObject;
import ubic.gemma.model.util.ModelUtils;

import javax.annotation.Nullable;

@Getter
@Setter
public abstract class AnalysisValueObject<T extends Analysis> extends DescribableValueObject<T> {

    private ProtocolValueObject protocol;

    protected AnalysisValueObject() {
        super();
    }

    protected AnalysisValueObject( T analysis ) {
        super( analysis );
        if ( analysis.getProtocol() != null && ModelUtils.isInitialized( analysis.getProtocol() ) ) {
            this.protocol = new ProtocolValueObject( analysis.getProtocol() );
        }
    }

    @Nullable
    @Override
    public String getName() {
        return super.getName();
    }
}
