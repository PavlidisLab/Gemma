package ubic.gemma.core.util;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.ConversionService;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import ubic.gemma.core.util.test.TestPropertyPlaceholderConfigurer;
import ubic.gemma.core.context.TestComponent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeTrue;

@ContextConfiguration
public class BuildInfoTest extends AbstractJUnit4SpringContextTests {

    @Import(BuildInfo.class)
    @Configuration
    @TestComponent
    static class BuildInfoContextConfiguration {

        @Bean
        public static TestPropertyPlaceholderConfigurer testPropertyPlaceholderConfigurer() {
            return new TestPropertyPlaceholderConfigurer( "gemma.version=1.0.0",
                    "gemma.build.timestamp=2023-11-09T00:26:02Z", "gemma.build.gitHash=1234" );
        }

        @Bean
        public ConversionService conversionService() {
            return new DefaultFormattingConversionService();
        }
    }

    @Autowired
    private BuildInfo buildInfo;

    @Test
    public void test() {
        assertEquals( "1.0.0", buildInfo.getVersion() );
        assertNotNull( buildInfo.getTimestamp() );
        assertEquals( "1234", buildInfo.getGitHash() );
    }

    @Test
    public void testFromClasspath() {
        assumeTrue( "Make sure that Gemma is build with Maven so that version.properties is generated.",
                getClass().getResource( "/ubic/gemma/version.properties" ) != null );
        buildInfo = BuildInfo.fromClasspath();
        assertNotNull( buildInfo.getVersion() );
        assertNotNull( buildInfo.getTimestamp() );
        assertNotNull( buildInfo.getGitHash() );
    }
}