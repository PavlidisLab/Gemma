package ubic.gemma.core.security.acl;

import ubic.gemma.core.security.acl.ObjectTransientnessRetrievalStrategy;
import ubic.gemma.core.security.model.Securable;
import org.springframework.util.Assert;

public class ObjectTransientnessRetrievalStrategyImpl implements ObjectTransientnessRetrievalStrategy {

    @Override
    public boolean isObjectTransient( Object domainObject ) {
        Assert.isInstanceOf( Securable.class, domainObject, "The domain object must implement the Securable interface" );
        return ( ( Securable ) domainObject ).getId() == null;
    }
}
