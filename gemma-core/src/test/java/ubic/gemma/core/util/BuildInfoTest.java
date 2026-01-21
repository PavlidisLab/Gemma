package ubic.gemma.core.util;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.ConversionService;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.util.test.BaseTest;
import ubic.gemma.core.util.test.TestPropertyPlaceholderConfigurer;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeTrue;

@ContextConfiguration
public class BuildInfoTest extends BaseTest {

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
        assertEquals( "1234", buildInfo.getGitHash() );
        Instant c = LocalDateTime.of( 2023, 11, 9, 0, 26, 2 )
                .atZone( ZoneId.of( "UTC" ) )
                .toInstant();
        assertEquals( Date.from( c ), buildInfo.getTimestamp() );
    }

    @Test
    @Ignore("The manifest is not available during the test phase.")
    public void testFromManifest() {
        BuildInfo buildInfo = BuildInfo.fromManifest();
        assertNotNull( buildInfo.getVersion() );
        assertNotNull( buildInfo.getTimestamp() );
        assertNotNull( buildInfo.getGitHash() );
    }

    @Test
    public void testParseMaven311BuildTimestamp() {
        BuildInfo buildInfo = new BuildInfo( "1.0.0", "20240910-1218", "1234" );
        assertEquals( "1.0.0", buildInfo.getVersion() );
        assertEquals( "1234", buildInfo.getGitHash() );
        Instant c = LocalDateTime.of( 2024, 9, 10, 12, 18 )
                .atZone( ZoneId.of( "America/Vancouver" ) )
                .toInstant();
        assertEquals( Date.from( c ), buildInfo.getTimestamp() );
    }
}