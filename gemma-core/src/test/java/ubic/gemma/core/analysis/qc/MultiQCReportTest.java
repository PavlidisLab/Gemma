package ubic.gemma.core.analysis.qc;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * TODO: cover more versions of MultiQC
 */
public class MultiQCReportTest {

    /**
     * This test use MultiQC 1.19
     */
    @Test
    public void testGSE263946() throws IOException {
        MultiQCReport report;
        try ( InputStream is = getClass().getResourceAsStream( "/data/analysis/qc/GSE263946_multiqc_data.json" ) ) {
            assertNotNull( is );
            report = MultiQCReport.parse( is );
        }
        assertEquals( "1.19", report.getConfigVersion() );
        List<String> expectedSampleNames = Arrays.asList( "GSM8207827", "GSM8207833", "GSM8207832", "GSM8207834",
                "GSM8207830", "GSM8207828", "GSM8207835", "GSM8207831", "GSM8207829" );
        System.out.println(report.getDataSources());
        assertThat( report.getGeneralStatsData() )
                .hasSize( 3 )
                .satisfiesExactlyInAnyOrder(
                        map -> {
                            assertThat( map )
                                    .hasSize( 2 * expectedSampleNames.size() )
                                    .allSatisfy( ( k, v ) -> {
                                        assertThat( v ).isInstanceOf( MultiQCReport.FastQcGeneralStats.class );
                                    } );
                        },
                        map -> {
                            assertThat( map )
                                    .containsOnlyKeys( expectedSampleNames )
                                    .allSatisfy( ( k, v ) -> {
                                        assertThat( v ).isInstanceOf( MultiQCReport.StarGeneralStats.class );
                                    } );
                        },
                        map -> {
                            assertThat( map )
                                    .containsOnlyKeys( expectedSampleNames )
                                    .allSatisfy( ( k, v ) -> assertThat( v ).isInstanceOf( MultiQCReport.RsemGeneralStats.class ) );
                        }
                );
    }
}