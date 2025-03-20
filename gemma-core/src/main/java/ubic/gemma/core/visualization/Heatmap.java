package ubic.gemma.core.visualization;

import org.jfree.chart.renderer.LookupPaintScale;
import org.jfree.chart.renderer.PaintScale;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.List;

/**
 * Minimal interface for a labelled heatmap.
 * @author poirigui
 */
public interface Heatmap extends Serializable {

    /**
     * Borrowed from Heatmap.js
     */
    PaintScale GEMMA_PAINT_SCALE = new LookupPaintScale( 0, 1, Color.GRAY ) {
        {
            Paint[] paints = new Paint[] {
                    new Color( 0, 0, 0 ), new Color( 32, 0, 0 ), new Color( 64, 0, 0 ),
                    new Color( 96, 0, 0 ), new Color( 128, 0, 0 ), new Color( 159, 32, 0 ),
                    new Color( 191, 64, 0 ), new Color( 223, 96, 0 ), new Color( 255, 128, 0 ),
                    new Color( 255, 159, 32 ), new Color( 255, 191, 64 ), new Color( 255, 223, 96 ),
                    new Color( 255, 255, 128 ), new Color( 255, 255, 159 ), new Color( 255, 255, 191 ),
                    new Color( 255, 255, 223 ), new Color( 255, 255, 255 )
            };
            for ( int i = 0; i < paints.length; i++ ) {
                add( ( double ) i / ( double ) paints.length, paints[i] );
            }
        }

        @Override
        public Paint getPaint( double value ) {
            if ( Double.isNaN( value ) ) {
                return getDefaultPaint();
            }
            return super.getPaint( value );
        }
    };

    default BufferedImage createImage() {
        return createImage( getCellSize() );
    }

    /**
     * Render a heatmap image.
     * @param cellSize size in pixel to use for individual cells in the heatmap
     */
    BufferedImage createImage( int cellSize );

    /**
     * Get the size of a cell in pixels.
     * <p>
     * Use this to adjust how labels are displayed.
     */
    int getCellSize();

    List<String> getXLabels();

    List<String> getYLabels();

    /**
     * Indicate if this heatmap is transposed.
     */
    boolean isTranspose();
}
