package ubic.gemma.persistence.initialization;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.util.Assert;

import javax.sql.DataSource;

/**
 * A {@link DataSourceInitializer} that bootstraps a data source by removing its database name from the JDBC URL.
 * @author poirigui
 */
public class BootstrappedDataSourceInitializer extends DataSourceInitializer {

    @Override
    public void setDataSource( DataSource dataSource ) {
        Assert.isInstanceOf( HikariDataSource.class, dataSource, "No idea how to bootstrap a data source of type " + dataSource.getClass().getName() + "." );
        HikariDataSource hikariDataSource = ( HikariDataSource ) dataSource;
        HikariDataSource bootstrappedDataSource = new HikariDataSource();
        hikariDataSource.copyStateTo( bootstrappedDataSource );
        bootstrappedDataSource.setJdbcUrl( stripPathComponent( bootstrappedDataSource.getJdbcUrl() ) );
        bootstrappedDataSource.setCatalog( null );
        super.setDataSource( bootstrappedDataSource );
    }

    String stripPathComponent( String jdbcUrl ) {
        int indexOfProtocol = jdbcUrl.indexOf( "://" );
        int indexOfPath = jdbcUrl.lastIndexOf( '/' );
        int indexOfQuery = jdbcUrl.lastIndexOf( '?' );
        if ( indexOfPath == indexOfProtocol + 2 ) {
            return jdbcUrl;
        }
        return jdbcUrl.substring( 0, indexOfPath )
                + ( ( indexOfQuery > 0 ) ? jdbcUrl.substring( indexOfQuery ) : "" );
    }
}
