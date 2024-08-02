package ubic.gemma.persistence.initialization;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.util.Assert;

import javax.sql.DataSource;

/**
 * A bootstrapped data source that strips the database from the JDBC URL.
 * @author poirigui
 */
public class BootstrappedDataSourceFactory implements FactoryBean<DataSource>, DisposableBean {

    private final HikariDataSource dataSource;

    public BootstrappedDataSourceFactory( DataSource dataSource ) {
        this.dataSource = createBootstrappedDataSource( dataSource );
    }

    @Override
    public DataSource getObject() throws Exception {
        return this.dataSource;
    }

    @Override
    public Class<?> getObjectType() {
        return HikariDataSource.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public void destroy() throws Exception {
        this.dataSource.close();
    }

    public static HikariDataSource createBootstrappedDataSource( DataSource dataSource ) {
        Assert.isInstanceOf( HikariDataSource.class, dataSource,
                "Don't know how to bootstrap a data source of type " + dataSource.getClass().getName() + "." );
        HikariDataSource hikariDataSource = ( HikariDataSource ) dataSource;
        HikariDataSource bootstrappedDataSource = new HikariDataSource();
        hikariDataSource.copyStateTo( bootstrappedDataSource );
        bootstrappedDataSource.setJdbcUrl( stripPathComponent( hikariDataSource.getJdbcUrl() ) );
        bootstrappedDataSource.setCatalog( null );
        return bootstrappedDataSource;
    }

    static String stripPathComponent( String jdbcUrl ) {
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
