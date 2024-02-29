package ubic.gemma.core.loader.util;

import org.junit.Test;
import ubic.gemma.core.loader.util.ftp.FTPClientFactory;
import ubic.gemma.core.loader.util.ftp.FTPClientFactoryImpl;

import java.net.URL;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class FTPClientFactoryImplTest {

    private final FTPClientFactory factory = new FTPClientFactoryImpl();

    @Test
    public void testGetFtpClientWithIncompleteUserInfo() {
        assertThatThrownBy( () -> factory.getFtpClient( new URL( "ftp://user@ftp.example.com" ) ) )
                .isInstanceOf( IllegalArgumentException.class )
                .hasMessageContaining( "':'" );
    }
}