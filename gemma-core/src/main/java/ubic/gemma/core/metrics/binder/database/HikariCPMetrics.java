package ubic.gemma.core.metrics.binder.database;

import com.zaxxer.hikari.HikariDataSource;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;

import javax.sql.DataSource;

public class HikariCPMetrics implements MeterBinder {

    private final HikariDataSource dataSource;

    public HikariCPMetrics( HikariDataSource dataSource ) {
        this.dataSource = dataSource;
    }

    @Override
    public void bindTo( MeterRegistry registry ) {
        dataSource.setMetricRegistry( registry );
    }
}
