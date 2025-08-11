package ubic.gemma.core.visualization;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.ArrayUtils;
import org.jfree.chart.ChartTheme;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.StandardChartTheme;

import java.awt.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static java.util.Objects.requireNonNull;

@CommonsLog
public class ChartThemeUtils {

    /**
     * List of fonts in order of preference (matches what the frontend uses, see typo.css).
     * <p>
     * Liberation Sans is not defined in the frontend, but we make it available as a substitute for Arial.
     */
    private static final String[] fonts = { "Avenir", "Helvetica", "Arial", "Liberation Sans" };

    static {
        // LiberationSans is a free font that is compatible with Arial
        String[] librationSansFonts = {
                "/ubic/gemma/core/visualization/LiberationSans-Regular.ttf",
                "/ubic/gemma/core/visualization/LiberationSans-Bold.ttf",
                "/ubic/gemma/core/visualization/LiberationSans-Italic.ttf",
                "/ubic/gemma/core/visualization/LiberationSans-BoldItalic.ttf"
        };
        for ( String fontPath : librationSansFonts ) {
            try {
                GraphicsEnvironment.getLocalGraphicsEnvironment()
                        .registerFont( Font.createFont( Font.TRUETYPE_FONT, requireNonNull( ChartUtils.class.getResourceAsStream( fontPath ) ) ) );
            } catch ( FontFormatException | IOException e ) {
                throw new RuntimeException( e );
            }
        }
    }

    /**
     * @param font a font name, one of "Avenir", "Helvetica", "Arial" or "Liberation Sans". If the font is not
     *             available, a graceful fallback will be used.
     */
    public static ChartTheme getGemmaChartTheme( String font ) {
        int fi = ArrayUtils.indexOf( fonts, font );
        if ( fi == -1 ) {
            throw new IllegalArgumentException( "Requested font is not supported, choose one among: " + String.join( ", ", fonts ) + "." );
        }
        String fontFamily = null;
        Set<String> availableFonts = new HashSet<>( Arrays.asList( GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames() ) );
        for ( int i = fi; i < fonts.length; i++ ) {
            if ( availableFonts.contains( fonts[i] ) ) {
                // Found a matching font, use it
                fontFamily = fonts[i];
                break;
            }
        }
        if ( fontFamily == null ) {
            if ( availableFonts.contains( "Liberation Sans" ) ) {
                // Liberation Sans is a free font that is compatible with Arial, use it if available
                log.warn( "None of the pre-defined frontend fonts are available on this system, using 'Liberation Sans' as fallback." );
                fontFamily = "Liberation Sans";
            } else {
                log.warn( "None of the pre-defined frontend fonts are available on this system, using Font.SANS_SERIF as fallback." );
                fontFamily = Font.SANS_SERIF;
            }
        }
        StandardChartTheme chartTheme = new StandardChartTheme( "Gemma" );
        chartTheme.setBaselinePaint( new Color( 0, 0, 0, 0 ) );
        chartTheme.setChartBackgroundPaint( new Color( 0, 0, 0, 0 ) );
        chartTheme.setLegendBackgroundPaint( new Color( 0, 0, 0, 0 ) );
        chartTheme.setPlotBackgroundPaint( Color.WHITE );
        // TODO: match the frontend font family, it uses Avenir, Helvetica, Arial, sans-serif (in order of availability)
        chartTheme.setExtraLargeFont( new Font( fontFamily, Font.BOLD, ( int ) ( 1.5 * 12 ) ) );
        chartTheme.setLargeFont( new Font( fontFamily, Font.BOLD, ( int ) ( 1.25 * 12 ) ) );
        chartTheme.setRegularFont( new Font( fontFamily, Font.PLAIN, ( int ) ( 1.0 * 12 ) ) );
        chartTheme.setSmallFont( new Font( fontFamily, Font.PLAIN, ( int ) ( 0.875 * 12 ) ) );
        return chartTheme;
    }
}
