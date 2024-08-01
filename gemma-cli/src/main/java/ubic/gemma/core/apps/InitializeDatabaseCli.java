package ubic.gemma.core.apps;

import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.init.CompositeDatabasePopulator;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import ubic.gemma.core.util.AbstractCLI;
import ubic.gemma.persistence.hibernate.LocalSessionFactoryBean;
import ubic.gemma.persistence.initialization.BootstrappedDataSourceInitializer;
import ubic.gemma.persistence.initialization.CreateDatabasePopulator;
import ubic.gemma.persistence.initialization.DatabaseSchemaPopulator;
import ubic.gemma.persistence.initialization.InitialDataPopulator;

import javax.annotation.Nullable;
import javax.sql.DataSource;

/**
 * This is exclusively available for the test database.
 */
@Profile("testdb")
public class InitializeDatabaseCli extends AbstractCLI {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private LocalSessionFactoryBean factory;

    @Value("${gemma.testdb.name}")
    private String databaseName;

    @Nullable
    @Override
    public String getCommandName() {
        return "initializeDatabase";
    }

    @Nullable
    @Override
    public String getShortDesc() {
        return "";
    }

    @Override
    public GemmaCLI.CommandGroup getCommandGroup() {
        return GemmaCLI.CommandGroup.MISC;
    }

    @Override
    protected void buildOptions( Options options ) {
    }

    @Override
    protected void processOptions( CommandLine commandLine ) throws ParseException {
    }

    @Override
    protected void doWork() throws Exception {
        String jdbcUrl;
        if ( dataSource instanceof HikariDataSource ) {
            jdbcUrl = ( ( HikariDataSource ) dataSource ).getJdbcUrl();
        } else {
            jdbcUrl = dataSource.toString();
        }
        promptConfirmationOrAbort( "The following data source will be initialized: " + jdbcUrl );
        BootstrappedDataSourceInitializer bdi = new BootstrappedDataSourceInitializer();
        bdi.setDataSource( dataSource );
        CreateDatabasePopulator cdb = new CreateDatabasePopulator( databaseName );
        cdb.setDropIfExists( true );
        bdi.setDatabasePopulator( cdb );
        bdi.afterPropertiesSet();
        DataSourceInitializer di = new DataSourceInitializer();
        di.setDataSource( dataSource );
        CompositeDatabasePopulator cdp = new CompositeDatabasePopulator();
        cdp.addPopulators(
                new DatabaseSchemaPopulator( factory, "mysql" ),
                new InitialDataPopulator( false ) );
        di.setDatabasePopulator( cdp );
        di.setEnabled( true );
        di.afterPropertiesSet();
    }
}
