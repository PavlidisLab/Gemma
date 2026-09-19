package ubic.gemma.apps;

import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.transaction.PlatformTransactionManager;
import ubic.gemma.cli.authentication.CLIAuthenticationManager;
import ubic.gemma.cli.util.EntityLocator;
import ubic.gemma.cli.util.test.BaseCliTest5;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.persistence.service.common.description.ExternalDatabaseService;
import ubic.gemma.persistence.service.genome.gene.GeneWriteService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ContextConfiguration
@TestExecutionListeners(value = WithSecurityContextTestExecutionListener.class,
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
class NcbiGeneLoaderCliTest extends BaseCliTest5 {

    @Configuration
    @TestComponent
    static class NcbiGeneLoaderCliTestContextConfiguration {

        @Bean
        @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
        public NcbiGeneLoaderCLI ncbiGeneLoaderCli() {
            return new NcbiGeneLoaderCLI();
        }

        @Bean
        public CLIAuthenticationManager cliAuthenticationManager() {
            return mock();
        }

        @Bean
        public TaxonService taxonService() {
            return mock();
        }

        @Bean
        public GeneWriteService geneWriteService() {
            return mock();
        }

        @Bean
        public ExternalDatabaseService externalDatabaseService() {
            return mock();
        }

        @Bean
        public EntityLocator entityLocator() {
            return mock();
        }

        @Bean
        public PlatformTransactionManager transactionManager() {
            return mock();
        }

        @Bean
        public SessionFactory sessionFactory() {
            return mock();
        }
    }

    @Autowired
    private ObjectProvider<NcbiGeneLoaderCLI> cli;

    @Autowired
    private GeneWriteService geneWriteService;

    @Autowired
    private TaxonService taxonService;

    @Autowired
    private EntityLocator entityLocator;

    @AfterEach
    void tearDown() {
        reset( geneWriteService, taxonService, entityLocator );
    }

    /**
     * The removals that are not made would be recorded nowhere.
     */
    @Test
    @WithMockUser
    void noRemoveWithoutReportIsRefused( @TempDir Path dir ) {
        // -f with no files in it, so that a run that got past the options would fail at once, not download from NCBI
        ubic.gemma.cli.util.test.Assertions.assertThat( cli.getObject() )
                .withArguments( "-noRemove", "-f", dir.toString(), "-taxon", "human" )
                .fails()
                .standardError()
                .asString( StandardCharsets.UTF_8 )
                .contains( "-report" );
        verifyNoInteractions( geneWriteService, taxonService, entityLocator );
    }

    @Test
    @WithMockUser
    void anExistingReportIsNotOverwrittenAndNoGeneIsLoaded( @TempDir Path dir ) throws Exception {
        Path report = dir.resolve( "report.tsv" );
        Files.writeString( report, "an earlier run" );

        ubic.gemma.cli.util.test.Assertions.assertThat( cli.getObject() )
                .withArguments( "-noRemove", "-report", report.toString(), "-f", dir.toString(), "-taxon", "human" )
                .fails();

        assertThat( report ).hasContent( "an earlier run" );
        verifyNoInteractions( geneWriteService, taxonService, entityLocator );
    }
}
