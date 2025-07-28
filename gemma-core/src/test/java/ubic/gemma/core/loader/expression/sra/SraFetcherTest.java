package ubic.gemma.core.loader.expression.sra;

import org.assertj.core.data.Offset;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ubic.gemma.core.loader.entrez.EntrezUtils;
import ubic.gemma.core.loader.expression.sra.model.*;
import ubic.gemma.core.util.SimpleRetryPolicy;
import ubic.gemma.core.util.test.category.SlowTest;

import java.io.IOException;
import java.io.StringReader;

import static org.assertj.core.api.Assertions.assertThat;
import static ubic.gemma.core.util.test.Assumptions.assumeThatResourceIsAvailable;

public class SraFetcherTest {

    private final SraFetcher sraFetcher = new SraFetcher( new SimpleRetryPolicy( 3, 1000, 1.5 ), null );

    @Test
    public void testFetch() throws IOException {
        assumeThatResourceIsAvailable( EntrezUtils.EFETCH + "?db=sra&id=SRX12015965" );
        SraExperimentPackageSet seps = sraFetcher.fetch( "SRX12015965" );
        checkExperiment( seps );
        assertThat( seps.getExperimentPackages() ).extracting( SraExperimentPackage::getOrganization )
                .allSatisfy( org -> {
                    assertThat( org.getType() ).isEqualTo( "center" );
                    assertThat( org.getName() ).isEqualTo( "NCBI" );
                    assertThat( org.getContact() ).satisfies( c -> {
                        assertThat( c.getFirstName() ).isEqualTo( "Geo" );
                        assertThat( c.getLastName() ).isEqualTo( "Curators" );
                        assertThat( c.getEmail() ).isEqualTo( "geo-group@ncbi.nlm.nih.gov" );
                    } );
                } );
    }

    @Test
    public void testFetchProject() throws IOException {
        SraExperimentPackageSet result = sraFetcher.fetch( "SRP313710" );
        assertThat( result.getExperimentPackages() )
                .hasSize( 29 )
                .extracting( SraExperimentPackage::getExperiment )
                .extracting( SraExperiment::getAccession )
                .containsExactlyInAnyOrder(
                        "SRX10528674",
                        "SRX10528673",
                        "SRX10528672",
                        "SRX10528671",
                        "SRX10528670",
                        "SRX10528669",
                        "SRX10528668",
                        "SRX10528667",
                        "SRX10528666",
                        "SRX10528665",
                        "SRX10528664",
                        "SRX10528663",
                        "SRX10528662",
                        "SRX10528661",
                        "SRX10528660",
                        "SRX10528659",
                        "SRX10528658",
                        "SRX10528657",
                        "SRX10528656",
                        "SRX10528655",
                        "SRX10528654",
                        "SRX10528653",
                        "SRX10528652",
                        "SRX10528651",
                        "SRX10528650",
                        "SRX10528649",
                        "SRX10528648",
                        "SRX10528647",
                        "SRX10528646" );
    }

    @Test
    public void testFetchRunInfo() throws IOException {
        assumeThatResourceIsAvailable( EntrezUtils.EFETCH + "?db=sra&id=SRX12015965" );
        String runinfo = sraFetcher.fetchRunInfo( "SRX12015965" );
        assertThat( runinfo )
                .hasLineCount( 1 + 8 )
                .startsWith( "Run,ReleaseDate,LoadDate,spots,bases" )
                .contains( "SRR15720449" );
        SraRuninfoParser parser = new SraRuninfoParser();
        SraExperimentPackageSet result = parser.parse( new StringReader( runinfo ) );
        checkExperiment( result );
    }

    @Test
    public void testFetchProjectRunInfo() throws IOException {
        String result = sraFetcher.fetchRunInfo( "SRP313710" );
        assertThat( result )
                // this has more lines because some SRX have multiple runs
                .hasLineCount( 33 )
                .contains(
                        "SRX10528674",
                        "SRX10528673",
                        "SRX10528672",
                        "SRX10528671",
                        "SRX10528670",
                        "SRX10528669",
                        "SRX10528668",
                        "SRX10528667",
                        "SRX10528666",
                        "SRX10528665",
                        "SRX10528664",
                        "SRX10528663",
                        "SRX10528662",
                        "SRX10528661",
                        "SRX10528660",
                        "SRX10528659",
                        "SRX10528658",
                        "SRX10528657",
                        "SRX10528656",
                        "SRX10528655",
                        "SRX10528654",
                        "SRX10528653",
                        "SRX10528652",
                        "SRX10528651",
                        "SRX10528650",
                        "SRX10528649",
                        "SRX10528648",
                        "SRX10528647",
                        "SRX10528646" );
    }

    @Test
    @Category(SlowTest.class)
    public void testGSE230440() throws IOException {
        SraExperimentPackageSet result = sraFetcher.fetchByGeoAccession( "GSE231774" );
        assertThat( result.getExperimentPackages() )
                .flatExtracting( SraExperimentPackage::getRunSets )
                .flatExtracting( SraRunSet::getRuns )
                .extracting( SraRun::getStatistics )
                .satisfiesOnlyOnce( stats -> {
                    assertThat( stats.getNumberOfReads() ).isEqualTo( "variable" );
                    assertThat( stats.getNumberOfSpots() ).isNull();
                    assertThat( stats.getReadStatistics() ).isNull();
                } );
    }

    @Test
    public void testGSE165635() throws IOException {
        SraExperimentPackageSet result = sraFetcher.fetchByGeoAccession( "GSE165635" );
        assertThat( result.getExperimentPackages() ).hasSize( 3 );
    }

    private void checkExperiment( SraExperimentPackageSet seps ) {
        assertThat( seps.getExperimentPackages() )
                .singleElement()
                .satisfies( ep -> {
                    assertThat( ep.getExperiment() ).satisfies( e -> {
                        assertThat( e.getAccession() ).isEqualTo( "SRX12015965" );
                        assertThat( e.getIdentifiers().getPrimaryId().getId() ).isEqualTo( "SRX12015965" );
                        assertThat( e.getDesign().getLibraryDescriptor().getStrategy() ).isEqualTo( "RNA-Seq" );
                        assertThat( e.getDesign().getLibraryDescriptor().getSource() ).isEqualTo( "TRANSCRIPTOMIC" );
                        assertThat( e.getDesign().getLibraryDescriptor().getSelection() ).isEqualTo( "cDNA" );
                        assertThat( e.getDesign().getLibraryDescriptor().getLayout().isPaired() ).isTrue();
                        assertThat( e.getDesign().getLibraryDescriptor().getLayout().isSingle() ).isFalse();
                        assertThat( e.getPlatform() ).satisfies( platform -> {
                            assertThat( platform.getInstrumentPlatform() ).isEqualTo( "ILLUMINA" );
                            assertThat( platform.getInstrumentModel() ).isEqualTo( "Illumina NovaSeq 6000" );
                        } );
                    } );
                    assertThat( ep.getSubmission() ).satisfies( sub -> {
                        assertThat( sub.getAccession() ).isEqualTo( "SRA1289059" );
                        assertThat( sub.getCenterName() ).isEqualTo( "GEO" );
                    } );
                    assertThat( ep.getRunSets() ).singleElement()
                            .satisfies( sr -> {
                                assertThat( sr.getBases() ).isEqualTo( 28041219976L );
                                // runinfo values are rounded to the next megabyte times 8 for the number of samples
                                assertThat( sr.getBytes() ).isCloseTo( 8690154253L, Offset.offset( 1024L * 1024L * 8 ) );
                                assertThat( sr.getSpots() ).isEqualTo( 471281008L );
                                assertThat( sr.getRuns() ).hasSize( 8 );
                                assertThat( sr.getRuns() )
                                        .extracting( SraRun::getPool )
                                        .allSatisfy( p -> {
                                            assertThat( p.getMembers() )
                                                    .singleElement()
                                                    .satisfies( m2 -> {
                                                        assertThat( m2.getTaxonId() ).isEqualTo( 10090 );
                                                        assertThat( m2.getOrganism() ).isEqualTo( "Mus musculus" );
                                                    } );
                                        } );
                                assertThat( sr.getRuns() )
                                        .extracting( SraRun::getAccession )
                                        .containsExactly(
                                                "SRR15720449",
                                                "SRR15720450",
                                                "SRR15720451",
                                                "SRR15720452",
                                                "SRR15720453",
                                                "SRR15720454",
                                                "SRR15720455",
                                                "SRR15720456" );
                                assertThat( sr.getRuns() )
                                        .extracting( SraRun::getTotalBases )
                                        .containsExactly(
                                                1601266520L,
                                                5204116190L,
                                                1585467548L,
                                                5152769531L,
                                                1614651612L,
                                                5247617739L,
                                                1796548432L,
                                                5838782404L );
                                assertThat( sr.getRuns() )
                                        .extracting( SraRun::getTotalSpots )
                                        .containsExactly(
                                                57188090L,
                                                57188090L,
                                                56623841L,
                                                56623841L,
                                                57666129L,
                                                57666129L,
                                                64162444L,
                                                64162444L
                                        );
                                long[] closeTo = {
                                        484343211L,
                                        1625757823L,
                                        476689987L,
                                        1601837740L,
                                        485060588L,
                                        1658807706L,
                                        541304976L,
                                        1816352222L };
                                for ( int i = 0; i < closeTo.length; i++ ) {
                                    assertThat( sr.getRuns().get( i ).getSize() )
                                            .isCloseTo( closeTo[i], Offset.offset( 1024L * 1024L ) );
                                }
                            } );
                } );
    }
}