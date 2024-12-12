package ubic.gemma.web.controller.common.auditAndSecurity.recaptcha;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ReCaptchaTest {

    @Test
    public void test() {
        ReCaptcha reCaptcha = new ReCaptcha( "test" );
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter( "g-recaptcha-response", "test" );
        ReCaptchaResponse response = reCaptcha.validateRequest( request );
        assertFalse( response.isValid() );
        assertEquals( "invalid-input-response", response.getErrorMessage() );
    }
}