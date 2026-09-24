package ubic.gemma.apps;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.cli.authentication.CLIAuthenticationManager;
import ubic.gemma.cli.util.test.BaseCliTest5;
import ubic.gemma.core.analysis.preprocess.PreprocessorService;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.loader.expression.geo.service.GeoService;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static ubic.gemma.cli.util.test.Assertions.assertThat;

/**
 * addGEOData: which load a given option combination reaches, and what a failed {@code -force} reload reports.
 * <p>
 * Each test gets a fresh CLI bean: the options it has parsed stay in fields that nothing clears.
 */
@ContextConfiguration
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class LoadExpressionDataCliTest extends BaseCliTest5 {

    @Configuration
    @TestComponent
    static class CC {

        @Bean
        public LoadExpressionDataCli loadExpressionDataCli() {
            return new LoadExpressionDataCli();
        }

        @Bean
        public ExpressionExperimentService expressionExperimentService() {
            return mock();
        }

        @Bean
        public PreprocessorService preprocessorService() {
            return mock();
        }

        @Bean
        public GeoService geoService() {
            return mock();
        }

        @Bean
        public ArrayDesignService arrayDesignService() {
            return mock();
        }

        @Bean
        public CLIAuthenticationManager cliAuthenticationManager() {
            return mock();
        }
    }

    @Autowired
    private LoadExpressionDataCli cli;
    @Autowired
    private GeoService geoService;
    @Autowired
    private ExpressionExperimentService eeService;
    @Autowired
    private ArrayDesignService arrayDesignService;

    /**
     * 🛑 {@code -y} applies to accessions read from {@code -f}. The file path called processAccession()
     * directly and skipped the platform-only check, so full experiments were loaded.
     */
    @Test
    public void testPlatformOnlyAppliesToAnAccessionFile( @TempDir Path dir ) throws Exception {
        Path file = dir.resolve( "accessions.txt" );
        Files.write( file, Collections.singletonList( "GSE1" ), StandardCharsets.UTF_8 );
        ArrayDesign ad = new ArrayDesign();
        ad.setShortName( "GPL1" );
        doReturn( Collections.singletonList( ad ) ).when( geoService ).fetchAndLoad( "GSE1", true, true, false, true, true );
        when( arrayDesignService.thawLite( ad ) ).thenReturn( ad );

        assertThat( cli ).withArguments( "-y", "-f", file.toString() ).succeeds();

        verify( geoService ).fetchAndLoad( "GSE1", true, true, false, true, true );
        verify( geoService, never() ).fetchAndLoad( eq( "GSE1" ), eq( false ), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean() );
    }

    /**
     * {@code -force} deletes the existing experiment before the fetch, so a failed fetch leaves no copy
     * of it. The error has to say so: it used to report only the load failure.
     */
    @Test
    public void testAFailedForceReloadSaysTheOldExperimentWasDeleted() {
        ExpressionExperiment existing = new ExpressionExperiment();
        existing.setId( 1L );
        existing.setShortName( "GSE1" );
        when( eeService.findByAccession( any( DatabaseEntry.class ) ) ).thenReturn( Collections.singletonList( existing ) );
        when( geoService.fetchAndLoad( eq( "GSE1" ), eq( false ), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean() ) )
                .thenThrow( new RuntimeException( "GEO is down" ) );

        assertThat( cli ).withArguments( "-e", "GSE1", "-force" )
                .fails()
                .standardOutput()
                .asString( StandardCharsets.UTF_8 )
                .contains( "deleted the existing experiment(s) GSE1" )
                .contains( "the old data is gone" );

        verify( eeService ).remove( existing );
    }
}
