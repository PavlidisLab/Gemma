package ubic.gemma.persistence.util;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@ContextConfiguration(locations = "classpath*:ubic/gemma/nanoContext-*.xml")
public class NanoContextTest extends AbstractJUnit4SpringContextTests {

    @Value("${gemma.appdata.home}")
    private String gemmaAppdataHome;

    @Autowired
    private MessageSource messageSource;

    @Test
    public void test() {
        assertNotNull( gemmaAppdataHome );
        assertEquals( "Time", messageSource.getMessage( "MeasurementKind.TIME.label", null, Locale.ENGLISH ) );
    }
}