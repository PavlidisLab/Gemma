package ubic.gemma.persistence.util;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.Future;

@Component
public class AsyncBeanFactoryImpl implements AsyncBeanFactory {

    @Autowired
    private BeanFactory beanFactory;

    @Override
    public <T> Future<T> getBeanAsync( Class<? extends AsyncFactoryBean<T>> clazz ) {
        return beanFactory.getBean( clazz ).getObjectAsync();
    }
}
