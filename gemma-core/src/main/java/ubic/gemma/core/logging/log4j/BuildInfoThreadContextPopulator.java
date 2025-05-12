package ubic.gemma.core.logging.log4j;


import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ubic.gemma.core.util.BuildInfo;

/**
 * Populate build information in the Log4j {@link ThreadContext}.
 * @author poirigui
 */
@Component
@Lazy(value = false)
public class BuildInfoThreadContextPopulator implements ThreadContextPopulator {

    /**
     * Context key used to store the build information.
     */
    public static final String BUILD_INFO_CONTEXT_KEY = KEY_PREFIX + ".buildInfo";

    private final BuildInfo buildInfo;

    @Autowired
    public BuildInfoThreadContextPopulator( BuildInfo buildInfo ) {
        this.buildInfo = buildInfo;
    }

    @Override
    public String getKey() {
        return BUILD_INFO_CONTEXT_KEY;
    }

    @Override
    public void populate() {
        ThreadContext.put( BUILD_INFO_CONTEXT_KEY, buildInfo.toString() );
    }
}
