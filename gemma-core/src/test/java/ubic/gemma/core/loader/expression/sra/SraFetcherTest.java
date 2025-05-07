package ubic.gemma.core.loader.expression.sra;

import org.junit.Test;
import ubic.gemma.core.loader.entrez.EntrezUtils;
import ubic.gemma.core.loader.expression.sra.model.SraExperimentPackageSet;
import ubic.gemma.core.util.SimpleRetryPolicy;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static ubic.gemma.core.util.test.Assumptions.assumeThatResourceIsAvailable;

public class SraFetcherTest {

    private final SraFetcher sraFetcher = new SraFetcher( new SimpleRetryPolicy( 3, 1000, 1.5 ), null );

    @Test
    public void test() throws IOException {
        assumeThatResourceIsAvailable( EntrezUtils.EFETCH + "?db=sra&id=SRX12015965" );
        assertThat( sraFetcher.fetchRunInfo( "SRX12015965" ) )
                .hasLineCount( 1 + 8 )
                .startsWith( "Run,ReleaseDate,LoadDate,spots,bases" )
                .contains( "SRR15720449" );
    }

    @Test
    public void testFetchExperiment() throws IOException {
        assumeThatResourceIsAvailable( EntrezUtils.EFETCH + "?db=sra&id=SRX12015965" );
        SraExperimentPackageSet seps = sraFetcher.fetchExperiment( "SRX12015965" );
        assertThat( seps.getExperimentPackages() )
                .singleElement()
                .satisfies( ep -> {
                    assertThat( ep.getExperiment() ).satisfies( e -> {
                        assertThat( e.getAccession() ).isEqualTo( "SRX12015965" );
                        assertThat( e.getIdentifiers().getPrimaryId().getId() ).isEqualTo( "SRX12015965" );
                    } );
                    assertThat( ep.getRunSets() ).singleElement()
                            .satisfies( sr -> {
                                assertThat( sr.getRuns() ).hasSize( 8 );
                            } );
                } );
    }
}