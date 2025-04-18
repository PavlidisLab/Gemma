package ubic.gemma.core.context;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.Ordered;
import org.springframework.core.type.MethodMetadata;

import java.util.Set;
import java.util.TreeSet;

/**
 * Mark beans as lazy-init by default.
 * <p>
 * Beans annotated with {@link Lazy} or annotated with the {@code ubic.gemma.core.context.LazyInitByDefaultPostProcessor.ignore}
 * attribute are excluded.
 * <p>
 * Beans part of the infrastructure role are excluded.
 * <p>
 * This is a necessary workaround because Spring 3 does not support lazy-by-default for annotated components.
 * <p>
 * Spring Boot provides a similar functionality with <a href="https://docs.spring.io/spring-boot/api/java/org/springframework/boot/LazyInitializationBeanFactoryPostProcessor.html">LazyInitializationBeanFactoryPostProcessor</a>.
 * @author poirigui
 */
@CommonsLog
public class LazyInitByDefaultPostProcessor implements BeanFactoryPostProcessor, Ordered {

    private static final String IGNORE_ATTRIBUTE = "ubic.gemma.core.context.LazyInitByDefaultPostProcessor.ignore";

    private static final String LAZY_ANNOTATION_CLASS = "org.springframework.context.annotation.Lazy";

    @Override
    public void postProcessBeanFactory( ConfigurableListableBeanFactory beanFactory ) throws BeansException {
        boolean isTraceEnabled = log.isTraceEnabled();
        log.debug( "Marking beans as lazy-init by default..." );
        Set<String> markedAsLazyInitBeans;
        int markedAsLazyInitBeansSize = 0;
        if ( isTraceEnabled ) {
            markedAsLazyInitBeans = new TreeSet<>();
        } else {
            markedAsLazyInitBeans = null;
        }
        for ( String beanName : beanFactory.getBeanDefinitionNames() ) {
            BeanDefinition def = beanFactory.getBeanDefinition( beanName );
            if ( def.hasAttribute( IGNORE_ATTRIBUTE ) && Boolean.parseBoolean( ( String ) def.getAttribute( IGNORE_ATTRIBUTE ) ) ) {
                if ( isTraceEnabled ) {
                    log.trace( "Ignoring " + formatBeanDefinition( beanName, def ) + " marked as ignored with " + IGNORE_ATTRIBUTE + "." );
                }
                continue;
            }
            if ( def.getRole() == BeanDefinition.ROLE_INFRASTRUCTURE ) {
                if ( isTraceEnabled ) {
                    log.trace( "Ignoring infrastructure bean " + formatBeanDefinition( beanName, def ) );
                }
                continue;
            }
            if ( def.isLazyInit() ) {
                if ( isTraceEnabled ) {
                    log.trace( "Ignoring " + formatBeanDefinition( beanName, def ) + " already lazy-init." );
                }
                continue;
            }
            if ( def instanceof AnnotatedBeanDefinition ) {
                AnnotatedBeanDefinition abd = ( AnnotatedBeanDefinition ) def;
                if ( abd.getMetadata().isAnnotated( LAZY_ANNOTATION_CLASS ) ) {
                    if ( isTraceEnabled ) {
                        log.trace( "Ignoring " + formatBeanDefinition( beanName, def ) + " with explicit @Lazy annotation." );
                    }
                    continue;
                }
                if ( def.getFactoryMethodName() != null ) {
                    // FIXME: is this really the best way?
                    if ( abd.getMetadata().getAnnotatedMethods( LAZY_ANNOTATION_CLASS ).stream()
                            .map( MethodMetadata::getMethodName ).anyMatch( def.getFactoryMethodName()::equals ) ) {
                        if ( isTraceEnabled ) {
                            log.trace( "Ignoring " + formatBeanDefinition( beanName, def ) + " with explicit @Lazy annotation." );
                        }
                        continue;
                    }
                }
            }
            if ( isTraceEnabled ) {
                log.trace( "Marking " + formatBeanDefinition( beanName, def ) + " as lazy-init." );
            }
            def.setLazyInit( true );
            if ( isTraceEnabled ) {
                markedAsLazyInitBeans.add( beanName );
            }
            markedAsLazyInitBeansSize++;
        }
        if ( isTraceEnabled ) {
            log.trace( "Marked the following beans as lazy-init: " + String.join( ", ", markedAsLazyInitBeans ) );
        } else {
            log.debug( "Marked " + markedAsLazyInitBeansSize + " beans as lazy-init." );
        }
    }

    @Override
    public int getOrder() {
        return HIGHEST_PRECEDENCE;
    }

    private String formatBeanDefinition( String beanName, BeanDefinition def ) {
        return beanName + ( def.getBeanClassName() != null ? " [" + def.getBeanClassName() + ( def.getFactoryMethodName() != null ? "#" + def.getFactoryMethodName() + "()" : "" ) + "]" : "" );
    }
}
