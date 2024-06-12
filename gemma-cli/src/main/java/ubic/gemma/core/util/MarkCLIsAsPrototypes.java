package ubic.gemma.core.util;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;

/**
 * Mark all CLI beans as prototype and singletons as lazy-init by default.
 * <p>
 * This essentially makes loading of beans very fast since only the beans referenced by the CLI will be initialized.
 * @author poirigui
 */
@CommonsLog
public class MarkCLIsAsPrototypes implements BeanFactoryPostProcessor {

    @Override
    public void postProcessBeanFactory( ConfigurableListableBeanFactory beanFactory ) throws BeansException {
        log.debug( "Marking all CLI beans as prototypes and singleton as lazy-init..." );
        for ( String beanName : beanFactory.getBeanDefinitionNames() ) {
            BeanDefinition def = beanFactory.getBeanDefinition( beanName );
            if ( isCli( def, beanFactory.getBeanClassLoader() ) ) {
                log.trace( "Marking " + beanName + " as prototype." );
                def.setScope( ConfigurableBeanFactory.SCOPE_PROTOTYPE );
            }
        }
    }

    private boolean isCli( BeanDefinition definition, ClassLoader classLoader ) {
        String className = definition.getBeanClassName();
        if ( className == null ) {
            return false;
        }
        Class<?> clazz;
        try {
            if ( definition instanceof AbstractBeanDefinition ) {
                clazz = ( ( AbstractBeanDefinition ) definition ).resolveBeanClass( classLoader );
            } else {
                clazz = Class.forName( className );
            }
        } catch ( ClassNotFoundException e ) {
            log.error( definition + " does not have a valid bean class name." );
            return false;
        }
        return CLI.class.isAssignableFrom( clazz );
    }
}
