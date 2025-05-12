package ubic.gemma.core.logging.log4j;

import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.InitializingBean;

/**
 * Interface for populating the Log4j {@link ThreadContext} with custom information.
 * <p>
 * Each populator implementation is responsible for populating a key. It may be done immediately in {@link #populate()}
 * or at a later time.
 * @author poirigui
 */
public interface ThreadContextPopulator extends InitializingBean {

    /**
     * Recommended prefix to use to organize {@link ThreadContext} keys,
     */
    String KEY_PREFIX = "ubic.gemma.core.logging.log4j.ThreadContextKeys";

    /**
     * Key in the {@link org.apache.logging.log4j.ThreadContext} this populator is responsible for.
     */
    String getKey();

    /**
     * Populate the {@link ThreadContext}.
     * <p>
     * This is called when the bean is initialized as per {@link InitializingBean#afterPropertiesSet()}.
     */
    void populate();

    @Override
    default void afterPropertiesSet() throws Exception {
        populate();
    }
}
