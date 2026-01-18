package ubic.gemma.core.loader.expression.ucsc.cellbrowser;

import org.junit.Rule;
import org.junit.Test;
import ubic.gemma.core.util.test.NetworkAvailable;
import ubic.gemma.core.util.test.NetworkAvailableRule;

import java.io.IOException;

public class UcscCellBrowserUtilsTest {

    @Rule
    public final NetworkAvailableRule networkAvailableRule = new NetworkAvailableRule();

    @Test
    @NetworkAvailable
    public void testGetDatasets() throws IOException {
        UcscCellBrowserUtils.getDatasets().forEach( System.out::println );
    }

    @Test
    public void testParseDatasets() throws IOException {
        UcscCellBrowserUtils.parseDatasets( getClass().getResource( "/data/loader/expression/ucsc/cellbrowser/dataset.json" ) );
    }

    @Test
    @NetworkAvailable
    public void testGetDataset() throws IOException {
        UcscCellBrowserUtils.getDataset( "aging-brain" );
    }

    @Test
    public void testParseDataset() throws IOException {
        UcscCellBrowserUtils.parseDataset( getClass().getResource( "/data/loader/expression/ucsc/cellbrowser/aging-brain.json" ) );
    }
}