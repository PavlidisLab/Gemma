package ubic.gemma.core.visualization;

import org.junit.Test;

import java.awt.*;

import static org.junit.Assert.assertEquals;

public class HeatmapTest {

    @Test
    public void test() {
        assertEquals( Color.GRAY, Heatmap.GEMMA_PAINT_SCALE.getPaint( Double.NaN ) );
        assertEquals( Color.GRAY, Heatmap.GEMMA_PAINT_SCALE.getPaint( Double.POSITIVE_INFINITY ) );
        assertEquals( Color.GRAY, Heatmap.GEMMA_PAINT_SCALE.getPaint( Double.NEGATIVE_INFINITY ) );
    }
}