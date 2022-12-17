package ubic.gemma.persistence.util;

import org.springframework.beans.factory.FactoryBean;

import java.util.concurrent.Future;

/**
 * Async extension for {@link FactoryBean}.
 */
public interface AsyncFactoryBean<T> extends FactoryBean<T> {

    Future<T> getObjectAsync();
}
