package ubic.gemma.apps;

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
import ubic.gemma.cli.authentication.CLIAuthenticationManager;
import ubic.gemma.cli.util.EntityLocator;
import ubic.gemma.cli.util.test.BaseCliTest5;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.loader.genome.gene.ncbi.GeneProductChangeTsv;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.persistence.service.genome.gene.GeneProductChange;
import ubic.gemma.persistence.service.genome.gene.GeneProductRemovalOutcome;
import ubic.gemma.persistence.service.genome.gene.GeneWriteService;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static ubic.gemma.cli.util.test.Assertions.assertThat;

@ContextConfiguration
@TestExecutionListeners(value = WithSecurityContextTestExecutionListener.class,
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
class ReplayGeneProductRemovalsCliTest extends BaseCliTest5 {

    @Configuration
    @TestComponent
    static class ReplayGeneProductRemovalsCliTestContextConfiguration {

        @Bean
        @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
        public ReplayGeneProductRemovalsCli replayGeneProductRemovalsCli() {
            return new ReplayGeneProductRemovalsCli();
        }

        @Bean
        public CLIAuthenticationManager cliAuthenticationManager() {
            return mock();
        }

        @Bean
        public GeneWriteService geneWriteService() {
            return mock();
        }

        @Bean
        public EntityLocator entityLocator() {
            return mock();
        }
    }

    private static final GeneProductChange KEPT = new GeneProductChange( GeneProductChange.Kind.REMOVE, false, "human",
            7003, "TEAD1", null, null, 12L, "NM_021961", "4507437", 1, 0, List.of( "GPL570" ) );
    private static final GeneProductChange REMOVED = new GeneProductChange( GeneProductChange.Kind.REMOVE, true, "human",
            7003, "TEAD1", null, null, 13L, "NM_021962", "4507438", 1, 0, List.of( "GPL570" ) );
    private static final GeneProductChange MOVED = new GeneProductChange( GeneProductChange.Kind.SWITCH, true, "human",
            7003, "TEAD1", 7004, "TEAD2", 14L, "NM_021963", "4507439", 1, 0, List.of( "GPL570" ) );

    @Autowired
    private ObjectProvider<ReplayGeneProductRemovalsCli> cli;

    @Autowired
    private GeneWriteService geneWriteService;

    @Autowired
    private EntityLocator entityLocator;

    @AfterEach
    void tearDown() {
        reset( geneWriteService, entityLocator );
    }

    /**
     * Removals already made and moves between genes are in the report too, and are left alone.
     */
    @Test
    @WithMockUser
    void onlyRemovalsNotMadeAreReplayedOnTheGivenPlatforms( @TempDir Path dir ) throws IOException {
        Path report = write( dir, false, KEPT, REMOVED, MOVED );
        ArrayDesign platform = ArrayDesign.Factory.newInstance( "GPL570", null );
        when( entityLocator.locateArrayDesign( "GPL570" ) ).thenReturn( platform );
        when( geneWriteService.replayGeneProductRemoval( any(), any(), anyBoolean() ) )
                .thenReturn( new GeneProductRemovalOutcome( null, "would delete 1 BLAT association", false, 1, 0, List.of( "GPL570" ) ) );

        assertThat( cli.getObject() )
                .withArguments( "-report", report.toString(), "-platforms", "GPL570", "-dryRun" )
                .succeeds();

        verify( geneWriteService ).replayGeneProductRemoval( KEPT, List.of( platform ), true );
        verifyNoMoreInteractions( geneWriteService );
    }

    /**
     * A dry run's changes were rolled back, so its rows say nothing about the database.
     */
    @Test
    @WithMockUser
    void aReportFromADryRunIsRefused( @TempDir Path dir ) throws IOException {
        Path report = write( dir, true, KEPT );

        assertThat( cli.getObject() )
                .withArguments( "-report", report.toString() )
                .fails();

        verifyNoInteractions( geneWriteService );
    }

    private static Path write( Path dir, boolean dryRun, GeneProductChange... changes ) throws IOException {
        Path report = dir.resolve( "report.tsv" );
        try ( Writer out = Files.newBufferedWriter( report ); GeneProductChangeTsv tsv = new GeneProductChangeTsv( out, dryRun ) ) {
            for ( GeneProductChange change : changes ) {
                tsv.accept( change );
            }
        }
        return report;
    }
}
