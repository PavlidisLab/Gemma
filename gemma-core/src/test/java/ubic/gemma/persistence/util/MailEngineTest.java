package ubic.gemma.persistence.util;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.util.test.BaseSpringContextTest;

public class MailEngineTest extends BaseSpringContextTest {

    @Autowired
    private MailEngine mailEngine;

    @Test
    public void test() {
        mailEngine.sendAdminMessage( "test", "test subject" );
    }
}