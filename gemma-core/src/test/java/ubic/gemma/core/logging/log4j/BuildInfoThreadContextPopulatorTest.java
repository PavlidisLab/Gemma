package ubic.gemma.core.logging.log4j;

import org.apache.logging.log4j.ThreadContext;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.util.BuildInfo;
import ubic.gemma.core.util.test.BaseTest;

import static org.assertj.core.api.Assertions.assertThat;

@ContextConfiguration
@TestExecutionListeners(WithSecurityContextTestExecutionListener.class)
public class BuildInfoThreadContextPopulatorTest extends BaseTest {

    @Configuration
    @TestComponent
    static class CC {

        @Bean
        public BuildInfoThreadContextPopulator threadContextConfigurer( ApplicationContext applicationContext ) {
            BuildInfo buildInfo = new BuildInfo( "1.0.0", null, null );
            return new BuildInfoThreadContextPopulator( buildInfo );
        }
    }

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    @WithMockUser("bob")
    public void test() {
        assertThat( ThreadContext.get( BuildInfoThreadContextPopulator.BUILD_INFO_CONTEXT_KEY ) )
                .isEqualTo( "1.0.0" );
    }
}