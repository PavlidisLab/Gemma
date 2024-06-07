package ubic.gemma.core.context;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.type.MethodMetadata;

import java.util.Set;
import java.util.TreeSet;

/**
 * Mark beans as lazy-init by default.
 * <p>
 * Beans annotated with {@link Lazy} or annotated with the {@code ubic.gemma.core.context.LazyInitByDefaultPostProcessor.ignore}
 * attribute are excluded.
 * <p>
 * This is a necessary workaround because Spring 3 does not support lazy-by-default for annotated components.
 * @author poirigui
 */
@CommonsLog
public class LazyInitByDefaultPostProcessor implements BeanFactoryPostProcessor {

    private static final String IGNORE_ATTRIBUTE = "ubic.gemma.core.context.LazyInitByDefaultPostProcessor.ignore";

    private static final String LAZY_ANNOTATION_CLASS = "org.springframework.context.annotation.Lazy";

    @Override
    public void postProcessBeanFactory( ConfigurableListableBeanFactory beanFactory ) throws BeansException {
        log.debug( "Marking beans as lazy-init by default..." );
        Set<String> markedAsLazyInitBeans = new TreeSet<>();
        for ( String beanName : beanFactory.getBeanDefinitionNames() ) {
            BeanDefinition def = beanFactory.getBeanDefinition( beanName );
            if ( def.hasAttribute( IGNORE_ATTRIBUTE ) && Boolean.parseBoolean( ( String ) def.getAttribute( IGNORE_ATTRIBUTE ) ) ) {
                log.debug( "Ignoring " + formatBeanDefinition( beanName, def ) + " marked as ignored with " + IGNORE_ATTRIBUTE + "." );
                continue;
            }
            if ( def.isLazyInit() ) {
                log.debug( "Ignoring " + formatBeanDefinition( beanName, def ) + " already lazy-init." );
                continue;
            }
            if ( def instanceof AnnotatedBeanDefinition ) {
                AnnotatedBeanDefinition abd = ( AnnotatedBeanDefinition ) def;
                if ( abd.getMetadata().isAnnotated( LAZY_ANNOTATION_CLASS ) ) {
                    log.debug( "Ignoring " + formatBeanDefinition( beanName, def ) + " with explicit @Lazy annotation." );
                    continue;
                }
                if ( def.getFactoryMethodName() != null ) {
                    // FIXME: is this really the best way?
                    if ( abd.getMetadata().getAnnotatedMethods( LAZY_ANNOTATION_CLASS ).stream()
                            .map( MethodMetadata::getMethodName ).anyMatch( def.getFactoryMethodName()::equals ) ) {
                        log.debug( "Ignoring " + formatBeanDefinition( beanName, def ) + " with explicit @Lazy annotation." );
                        continue;
                    }
                }
            }
            if ( log.isTraceEnabled() ) {
                log.trace( "Marking " + formatBeanDefinition( beanName, def ) + " as lazy-init." );
            }
            def.setLazyInit( true );
            markedAsLazyInitBeans.add( beanName );
        }
        log.debug( "Marked the following beans as lazy-init: " + String.join( ", ", markedAsLazyInitBeans ) );
    }

    private String formatBeanDefinition( String beanName, BeanDefinition def ) {
        return beanName + ( def.getBeanClassName() != null ? " [" + def.getBeanClassName() + ( def.getFactoryMethodName() != null ? "#" + def.getFactoryMethodName() + "()" : "" ) + "]" : "" );
    }
}
