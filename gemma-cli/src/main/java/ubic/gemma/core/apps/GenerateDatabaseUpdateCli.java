package ubic.gemma.core.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.hibernate.dialect.Dialect;
import org.hibernate.tool.hbm2ddl.DatabaseMetadata;
import org.hibernate.tool.hbm2ddl.SchemaUpdateScript;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.util.AbstractCLI;
import ubic.gemma.persistence.hibernate.H2Dialect;
import ubic.gemma.persistence.hibernate.LocalSessionFactoryBean;
import ubic.gemma.persistence.hibernate.MySQL57InnoDBDialect;

import javax.annotation.Nullable;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Generate a database update script.
 * @see InitializeDatabaseCli
 */
public class GenerateDatabaseUpdateCli extends AbstractCLI {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private LocalSessionFactoryBean factory;

    @Nullable
    private Path outputFile;

    private Dialect dialect;

    @Nullable
    @Override
    public String getCommandName() {
        return "generateDatabaseUpdate";
    }

    @Nullable
    @Override
    public String getShortDesc() {
        return "Generate a script to update the database";
    }

    @Override
    public GemmaCLI.CommandGroup getCommandGroup() {
        return GemmaCLI.CommandGroup.SYSTEM;
    }

    @Override
    protected void buildOptions( Options options ) {
        options.addOption( "d", "dialect", true, "Dialect to use to generate SQL statements (either mysql or h2, defaults to mysql)" );
        options.addOption( Option.builder( "o" ).longOpt( "output-file" ).hasArg().type( Path.class ).desc( "File destination for the update script (defaults to stdout)" ).build() );
    }

    @Override
    protected void processOptions( CommandLine commandLine ) throws ParseException {
        if ( commandLine.hasOption( "d" ) ) {
            String dialectStr = commandLine.getOptionValue( "d" );
            if ( "mysql".equalsIgnoreCase( dialectStr ) ) {
                dialect = new MySQL57InnoDBDialect();
            } else if ( "h2".equalsIgnoreCase( dialectStr ) ) {
                dialect = new H2Dialect();
            } else {
                throw new IllegalArgumentException( "Unknown dialect " + dialectStr );
            }
        } else {
            log.info( "No dialect specified, defaulting to MySQL 5.7." );
            dialect = new MySQL57InnoDBDialect();
        }
        outputFile = commandLine.getParsedOptionValue( "o" );
    }

    @Override
    protected void doWork() throws Exception {
        DatabaseMetadata dm;
        try ( Connection c = dataSource.getConnection() ) {
            dm = new DatabaseMetadata( c, dialect, factory.getConfiguration() );
        }
        List<String> sqlUpdates = factory.getConfiguration()
                .generateSchemaUpdateScriptList( dialect, dm )
                .stream()
                .map( SchemaUpdateScript::getScript )
                .collect( Collectors.toList() );
        try ( PrintWriter writer = getWriter() ) {
            for ( String sqlUpdate : sqlUpdates ) {
                writer.println( sqlUpdate + ";" );
            }
        }
    }

    public PrintWriter getWriter() throws IOException {
        return outputFile != null ? new PrintWriter( Files.newBufferedWriter( outputFile ) ) : new PrintWriter( System.out );
    }
}
