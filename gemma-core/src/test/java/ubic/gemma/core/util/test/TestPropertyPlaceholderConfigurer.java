package ubic.gemma.core.util.test;

import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Properties;

/**
 * Minimalistic placeholder configurer for usage in tests.
 * <p>
 * TODO: switch to <a href="https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/test/context/TestPropertySource.html">@TestPropertySource</a>
 * once we migrate to Spring 4.
 * @author poirigui
 */
public class TestPropertyPlaceholderConfigurer extends PropertyPlaceholderConfigurer {

    public TestPropertyPlaceholderConfigurer( String... properties ) {
        setProperties( createTestProperties( properties ) );
    }

    private static Properties createTestProperties( String... properties ) {
        Properties props = new Properties();
        try ( Reader reader = new StringReader( String.join( "\n", properties ) ) ) {
            props.load( reader );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
        return props;
    }
}
