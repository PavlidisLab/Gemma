package ubic.gemma.core.visualization;

import org.jfree.chart.ChartTheme;
import org.jfree.chart.StandardChartTheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.awt.*;

@Configuration
public class JFreeChartConfig {

    @Bean
    public ChartTheme chartTheme() {
        StandardChartTheme chartTheme = new StandardChartTheme( "Gemma" );
        chartTheme.setBaselinePaint( new Color( 0, 0, 0, 0 ) );
        chartTheme.setChartBackgroundPaint( new Color( 0, 0, 0, 0 ) );
        chartTheme.setLegendBackgroundPaint( new Color( 0, 0, 0, 0 ) );
        chartTheme.setPlotBackgroundPaint( Color.WHITE );
        // see typo.css
        chartTheme.setExtraLargeFont( new Font( "Avenir", Font.BOLD, ( int ) ( 1.5 * 12 ) ) );
        chartTheme.setLargeFont( new Font( "Avenir", Font.BOLD, ( int ) ( 1.25 * 12 ) ) );
        chartTheme.setRegularFont( new Font( "Avenir", Font.PLAIN, ( int ) ( 1.0 * 12 ) ) );
        chartTheme.setSmallFont( new Font( "Avenir", Font.PLAIN, ( int ) ( 0.875 * 12 ) ) );
        return chartTheme;
    }
}
