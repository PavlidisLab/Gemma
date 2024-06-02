package ubic.gemma.core.context;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanFactory;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Utilities for working with bean factories.
 * @author poirigui
 */
public class BeanFactoryUtils {

    /**
     * Add a {@link BeanPostProcessor} at a specific index of a bean factory.
     * @see ConfigurableBeanFactory#addBeanPostProcessor(BeanPostProcessor)
     */
    public static void addBeanPostProcessor( ConfigurableBeanFactory beanFactory, int index, BeanPostProcessor beanPostProcessor ) {
        Assert.isInstanceOf( AbstractBeanFactory.class, beanFactory,
                "Initialization time monitoring is only supported for subclasses of AbstractBeanFactory." );
        // unfortunately, it's not possible to add a BeanPostProcessor before existing ones and using ordering does not
        // ensure that we are the first one
        Field f = ReflectionUtils.findField( AbstractBeanFactory.class, "beanPostProcessors" );
        ReflectionUtils.makeAccessible( f );
        List<BeanPostProcessor> beanPostProcessors = ( List<BeanPostProcessor> ) ReflectionUtils.getField( f, beanFactory );
        // needed to trigger some behaviours
        beanFactory.addBeanPostProcessor( beanPostProcessor );
        // reinsert at desired index
        beanPostProcessors.remove( beanPostProcessor );
        beanPostProcessors.add( index, beanPostProcessor );
    }
}
