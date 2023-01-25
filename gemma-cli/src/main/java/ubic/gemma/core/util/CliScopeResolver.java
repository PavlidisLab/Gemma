package ubic.gemma.core.util;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ScopeMetadata;
import org.springframework.context.annotation.ScopeMetadataResolver;

/**
 * CLIs use the {@link BeanDefinition#SCOPE_PROTOTYPE} scope, always.
 * @author poirigui
 */
public class CliScopeResolver implements ScopeMetadataResolver {

    private static final ScopeMetadata CLI_SCOPE_METADATA;

    static {
        CLI_SCOPE_METADATA = new ScopeMetadata();
        CLI_SCOPE_METADATA.setScopeName( BeanDefinition.SCOPE_PROTOTYPE );
    }

    @Override
    public ScopeMetadata resolveScopeMetadata( BeanDefinition definition ) {
        return CLI_SCOPE_METADATA;
    }
}
