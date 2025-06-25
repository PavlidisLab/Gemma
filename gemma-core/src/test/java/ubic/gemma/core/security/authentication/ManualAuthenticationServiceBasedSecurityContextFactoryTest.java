package ubic.gemma.core.security.authentication;

import gemma.gsec.authentication.ManualAuthenticationService;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

public class ManualAuthenticationServiceBasedSecurityContextFactoryTest {

    @Test
    public void test() throws Exception {
        ManualAuthenticationService s = mock();
        ManualAuthenticationServiceBasedSecurityContextFactory factory = new ManualAuthenticationServiceBasedSecurityContextFactory();
        factory.setManualAuthenticationService( s );
        factory.setUserName( "groupAgent" );
        factory.setPassword( "1234" );
        assertThat( factory.isSingleton() ).isTrue();
        factory.afterPropertiesSet();
        verify( s ).authenticate( "groupAgent", "1234" );
        assertThatThrownBy( factory::createInstance )
                .isInstanceOf( IllegalArgumentException.class );
        verifyNoMoreInteractions( s );
    }
}