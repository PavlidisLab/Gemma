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
        return chartTheme;
    }
}
