package ubic.gemma.core.apps;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.loader.util.ftp.FTPConfig;
import ubic.gemma.core.util.test.BaseCliTest;

import static ubic.gemma.core.util.test.Assertions.assertThat;

@ContextConfiguration
public class SingleCellDataDownloaderCliTest extends BaseCliTest {

    @Configuration
    @TestComponent
    @Import(FTPConfig.class)
    static class CC {
        @Bean
        public SingleCellDataDownloaderCli singleCellDataDownloaderCli() {
            return new SingleCellDataDownloaderCli();
        }
    }

    @Autowired
    private SingleCellDataDownloaderCli singleCellDataDownloaderCli;

    @Test
    public void test() {
        assertThat( singleCellDataDownloaderCli )
                .withArguments( "-e", "GSE1234" )
                .succeeds();
    }
}