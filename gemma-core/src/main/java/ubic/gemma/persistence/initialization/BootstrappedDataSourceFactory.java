package ubic.gemma.persistence.initialization;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.jdbc.datasource.AbstractDriverBasedDataSource;

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
        return this.dataSource.getClass();
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
        if ( dataSource instanceof HikariDataSource ) {
            return bootstrapHikariDataSource( ( HikariDataSource ) dataSource );
        } else if ( dataSource instanceof AbstractDriverBasedDataSource ) {
            return bootstrapDriverBasedDataSource( ( AbstractDriverBasedDataSource ) dataSource );
        } else {
            throw new IllegalArgumentException( "Don't know how to bootstrap a data source of type " + dataSource.getClass().getName() + "." );
        }
    }

    private static HikariDataSource bootstrapHikariDataSource( HikariDataSource dataSource ) {
        HikariDataSource bootstrappedDataSource = new HikariDataSource();
        dataSource.copyStateTo( bootstrappedDataSource );
        bootstrappedDataSource.setJdbcUrl( stripPathComponent( dataSource.getJdbcUrl() ) );
        bootstrappedDataSource.setCatalog( null );
        return bootstrappedDataSource;
    }

    private static HikariDataSource bootstrapDriverBasedDataSource( AbstractDriverBasedDataSource dataSource ) {
        HikariDataSource bootstrappedDataSource = new HikariDataSource();
        bootstrappedDataSource.setJdbcUrl( stripPathComponent( dataSource.getUrl() ) );
        bootstrappedDataSource.setUsername( dataSource.getUsername() );
        bootstrappedDataSource.setPassword( dataSource.getPassword() );
        bootstrappedDataSource.setDataSourceProperties( dataSource.getConnectionProperties() );
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
