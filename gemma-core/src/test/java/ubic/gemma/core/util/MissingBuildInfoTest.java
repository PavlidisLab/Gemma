package ubic.gemma.core.util;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.util.test.BaseTest5;
import ubic.gemma.core.util.test.TestPropertyPlaceholderConfigurer;
import ubic.gemma.core.context.TestComponent;

import static org.junit.jupiter.api.Assertions.assertNull;

@ContextConfiguration
public class MissingBuildInfoTest extends BaseTest5 {

    @Import(BuildInfo.class)
    @Configuration
    @TestComponent
    static class BuildInfoContextConfiguration {

        @Bean
        public static TestPropertyPlaceholderConfigurer testPropertyPlaceholderConfigurer() {
            return new TestPropertyPlaceholderConfigurer();
        }
    }

    @Autowired
    private BuildInfo buildInfo;

    @Test
    public void test() {
        assertNull( buildInfo.getVersion() );
        assertNull( buildInfo.getTimestamp() );
        assertNull( buildInfo.getGitHash() );
    }
}
