package gemma.gsec.acl;

import gemma.gsec.acl.ObjectTransientnessRetrievalStrategy;
import gemma.gsec.model.Securable;
import org.springframework.util.Assert;

public class ObjectTransientnessRetrievalStrategyImpl implements ObjectTransientnessRetrievalStrategy {

    @Override
    public boolean isObjectTransient( Object domainObject ) {
        Assert.isInstanceOf( Securable.class, domainObject, "The domain object must implement the Securable interface" );
        return ( ( Securable ) domainObject ).getId() == null;
    }
}
