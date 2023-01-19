package ubic.gemma.web.util;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import ubic.gemma.core.util.test.BaseSpringContextTest;

import java.util.Locale;

import static org.junit.Assert.assertEquals;

public class MessageSourceTest extends BaseSpringContextTest {

    @Autowired
    private MessageSource messageSource;

    @Test
    public void test() {
        assertEquals( "Bad News", messageSource.getMessage( "errorPage.title", null, Locale.ENGLISH ) );
        assertEquals( "Une erreur est apparue", messageSource.getMessage( "errorPage.title", null, Locale.FRENCH ) );
    }
}
