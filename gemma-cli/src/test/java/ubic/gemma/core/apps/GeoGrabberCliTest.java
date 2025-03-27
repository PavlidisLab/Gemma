package ubic.gemma.core.apps;

import org.junit.Test;
import ubic.gemma.core.loader.expression.geo.model.GeoRecord;

import static org.assertj.core.api.Assertions.assertThat;

public class GeoGrabberCliTest {

    @Test
    public void testExtractKeywords() {
        GeoGrabberCli cli = new GeoGrabberCli();
        GeoRecord record = new GeoRecord();
        record.setSampleDescriptions( "single-cell ncRNA, other; text{} (haha) the fromage " );
        assertThat( cli.extractKeywords( record ) )
                .containsExactlyInAnyOrder( "single-cell", "ncrna", "other", "text", "haha", "fromage" );
    }
}