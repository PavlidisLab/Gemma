package ubic.gemma.core.util;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import ubic.gemma.core.util.test.TestPropertyPlaceholderConfigurer;
import ubic.gemma.persistence.util.TestComponent;

import static org.junit.Assert.assertNull;

@ContextConfiguration
public class MissingBuildInfoTest extends AbstractJUnit4SpringContextTests {

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
