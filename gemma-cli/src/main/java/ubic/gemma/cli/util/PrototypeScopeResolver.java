package ubic.gemma.cli.util;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.ScopeMetadata;
import org.springframework.context.annotation.ScopeMetadataResolver;

public class PrototypeScopeResolver implements ScopeMetadataResolver {

    private static final ScopeMetadata PROTOTYPE_SCOPE_METADATA;

    static {
        PROTOTYPE_SCOPE_METADATA = new ScopeMetadata();
        PROTOTYPE_SCOPE_METADATA.setScopeName( ConfigurableBeanFactory.SCOPE_PROTOTYPE );
    }

    @Override
    public ScopeMetadata resolveScopeMetadata( BeanDefinition beanDefinition ) {
        return PROTOTYPE_SCOPE_METADATA;
    }
}
