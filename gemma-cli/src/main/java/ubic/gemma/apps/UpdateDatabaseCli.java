package ubic.gemma.apps;

import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import ubic.gemma.cli.util.AbstractCLI;
import ubic.gemma.persistence.hibernate.LocalSessionFactoryBean;
import ubic.gemma.persistence.initialization.DatabaseSchemaUpdatePopulator;

import javax.annotation.Nullable;
import javax.sql.DataSource;

/**
 * Update the database.
 * <p>
 * This is exclusively available for the test database.
 */
@Profile("testdb")
public class UpdateDatabaseCli extends AbstractCLI {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private LocalSessionFactoryBean factory;

    private boolean force;

    @Nullable
    @Override
    public String getCommandName() {
        return "updateDatabase";
    }

    @Nullable
    @Override
    public String getShortDesc() {
        return "Update the database";
    }

    @Override
    public CommandGroup getCommandGroup() {
        return CommandGroup.SYSTEM;
    }

    @Override
    protected void buildOptions( Options options ) {
        options.addOption( "force", "Force the update, ignoring confirmation prompt." );
    }

    @Override
    protected void processOptions( CommandLine commandLine ) throws ParseException {
        this.force = getOptions().hasOption( "force" );
    }

    @Override
    protected void doWork() throws Exception {
        String jdbcUrl;
        if ( dataSource instanceof HikariDataSource ) {
            jdbcUrl = ( ( HikariDataSource ) dataSource ).getJdbcUrl();
        } else {
            jdbcUrl = dataSource.toString();
        }
        DatabaseSchemaUpdatePopulator pop = new DatabaseSchemaUpdatePopulator( factory.getConfiguration(), "mysql" );
        if ( force ) {
            log.info( "The following database will be updated: " + jdbcUrl );
        } else {
            promptConfirmationOrAbort( "The following database will be updated: " + jdbcUrl );
        }
        DatabasePopulatorUtils.execute( pop, dataSource );
    }
}
