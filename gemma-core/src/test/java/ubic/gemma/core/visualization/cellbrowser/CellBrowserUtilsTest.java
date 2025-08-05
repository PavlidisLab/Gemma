package ubic.gemma.core.visualization.cellbrowser;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CellBrowserUtilsTest {
    @Test
    public void testGetMetaColummIdentifier() {
        assertEquals( "scMinuspipelineMinus111", CellBrowserUtils.getMetaColumnIdentifier( "sc-pipeline-1.1.1" ) );
        assertEquals( "scMinuspipelineMinus111aftersplits", CellBrowserUtils.getMetaColumnIdentifier( "sc-pipeline-1.1.1 after splits" ) );
    }
}