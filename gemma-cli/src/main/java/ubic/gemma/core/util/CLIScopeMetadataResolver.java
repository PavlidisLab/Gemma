package ubic.gemma.core.util;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.AnnotationScopeMetadataResolver;
import org.springframework.context.annotation.ScopeMetadata;
import org.springframework.context.annotation.ScopeMetadataResolver;

/**
 * Resolve scope metadata for {@link CLI}.
 * @author poirigui
 */
@CommonsLog
public class CLIScopeMetadataResolver implements ScopeMetadataResolver {

    private final ScopeMetadataResolver delegate;

    @SuppressWarnings("unused")
    public CLIScopeMetadataResolver() {
        this( new AnnotationScopeMetadataResolver() );
    }

    public CLIScopeMetadataResolver( ScopeMetadataResolver delegate ) {
        this.delegate = delegate;
    }

    @Override
    public ScopeMetadata resolveScopeMetadata( BeanDefinition definition ) {
        ScopeMetadata metadata = delegate.resolveScopeMetadata( definition );
        if ( isCli( definition ) ) {
            log.info( "Marking " + definition + " as prototype." );
            metadata.setScopeName( ConfigurableBeanFactory.SCOPE_PROTOTYPE );
        }
        return metadata;
    }

    private boolean isCli( BeanDefinition definition ) {
        String className = definition.getBeanClassName();
        if ( className == null ) {
            return false;
        }
        try {
            return CLI.class.isAssignableFrom( Class.forName( className ) );
        } catch ( ClassNotFoundException e ) {
            log.error( definition + " does not have a valid bean class name." );
            return false;
        }
    }
}
