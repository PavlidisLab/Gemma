package ubic.gemma.apps;

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.cli.util.EntityLocator;
import ubic.gemma.cli.util.test.AuthenticatedCliTestConfig;
import ubic.gemma.cli.util.test.BaseCliTest;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.loader.expression.simple.SimpleExpressionDataLoaderService;
import ubic.gemma.core.loader.expression.singleCell.SingleCellDataLoaderService;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static ubic.gemma.cli.util.test.Assertions.assertThat;

@ContextConfiguration
public class LoadSimpleExpressionDataCliTest extends BaseCliTest {

    @Configuration
    @TestComponent
    @Import(AuthenticatedCliTestConfig.class)
    static class CC {
        @Bean
        public LoadSimpleExpressionDataCli loadSimpleExpressionDataCli() {
            return new LoadSimpleExpressionDataCli();
        }

        @Bean
        public SimpleExpressionDataLoaderService simpleExpressionDataLoaderService() {
            return mock();
        }

        @Bean
        public ExpressionExperimentService expressionExperimentService() {
            return mock();
        }

        @Bean
        public SingleCellDataLoaderService singleCellDataLoaderService() {
            return mock();
        }

        @Bean
        public EntityLocator entityLocator() {
            return mock();
        }
    }

    @Autowired
    private LoadSimpleExpressionDataCli loadSimpleExpressionDataCli;

    @Autowired
    private SimpleExpressionDataLoaderService simpleExpressionDataLoaderService;

    @Autowired
    private EntityLocator entityLocator;

    @After
    public void resetMocks() {
        reset( simpleExpressionDataLoaderService );
    }

    /**
     * Convers old-style files that lack a header.
     */
    @Test
    public void testLoadMetadataFileLackingHeader() throws IOException {
        Taxon human = new Taxon();
        when( entityLocator.locateTaxon( "human" ) ).thenReturn( human );
        assertThat( loadSimpleExpressionDataCli )
                .withArguments( "-f", new ClassPathResource( "ubic/gemma/apps/simple-data-file-lacking-header.tsv" ).getFile().getAbsolutePath(),
                        "-d", new ClassPathResource( "ubic/gemma/apps" ).getFile().getAbsolutePath(),
                        "-legacy" )
                .succeeds();
        verify( simpleExpressionDataLoaderService ).create( any(), assertArg( matrix -> {
            assertThat( matrix.getRowNames() ).containsExactly( "cs1", "cs2", "cs3" );
            assertThat( matrix.getColNames() ).containsExactly( "ba1", "ba2" );
        } ) );
    }

    @Test
    public void testLoadMetadata() throws IOException {
        Taxon human = new Taxon();
        when( entityLocator.locateTaxon( "human" ) ).thenReturn( human );
        assertThat( loadSimpleExpressionDataCli )
                .withArguments( "-f", new ClassPathResource( "ubic/gemma/apps/simple-data-file.tsv" ).getFile().getAbsolutePath(),
                        "-d", new ClassPathResource( "ubic/gemma/apps" ).getFile().getAbsolutePath() )
                .succeeds();
        verify( simpleExpressionDataLoaderService ).create( any(), assertArg( matrix -> {
            assertThat( matrix.getRowNames() ).containsExactly( "cs1", "cs2", "cs3" );
            assertThat( matrix.getColNames() ).containsExactly( "ba1", "ba2" );
        } ) );
    }

    @Test
    public void testLoadMetadataWithoutData() throws IOException {
        Taxon human = new Taxon();
        when( entityLocator.locateTaxon( "human" ) ).thenReturn( human );
        assertThat( loadSimpleExpressionDataCli )
                .withArguments( "-f", new ClassPathResource( "ubic/gemma/apps/simple-data-file-without-data.tsv" ).getFile().getAbsolutePath(),
                        "-d", new ClassPathResource( "ubic/gemma/apps" ).getFile().getAbsolutePath() )
                .succeeds();
        verify( simpleExpressionDataLoaderService ).create( any(), isNull() );
    }
}