package ubic.gemma.corej11.visualization;

import org.jfree.chart.JFreeChart;
import ubic.gemma.core.util.BuildInfo;
import ubic.gemma.core.util.Constants;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @see org.jfree.chart.ChartUtils
 * @author poirigui
 */
public class ChartUtils {

    public static void applyCurrentTheme( JFreeChart chart ) {
        org.jfree.chart.ChartUtils.applyCurrentTheme( chart );
    }

    public static void writeChartAsPNG( OutputStream out, JFreeChart chart, int width, int height, double dpi, String title, BuildInfo buildInfo ) throws IOException {
        BufferedImage bufferedImage = chart.createBufferedImage( width, height, null );
        writeBufferedImageAsPNG( out, bufferedImage, dpi, title, buildInfo );
    }

    public static void writeBufferedImageAsPNG( OutputStream out, BufferedImage bufferedImage, double dpi, String title, BuildInfo buildInfo ) throws IOException {
        SunPNGEncoderWithMetadataAdapter imageEncoder = new SunPNGEncoderWithMetadataAdapter();
        imageEncoder.setDpi( dpi );
        imageEncoder.setTitle( title );
        imageEncoder.setCopyright( Constants.GEMMA_COPYRIGHT_NOTICE );
        imageEncoder.setDisclaimer( Constants.GEMMA_LICENSE_NOTICE );
        imageEncoder.setSoftware( "Gemma " + buildInfo );
        imageEncoder.encode( bufferedImage, out );
    }
}
