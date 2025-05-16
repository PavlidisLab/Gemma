package ubic.gemma.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.hibernate.dialect.Dialect;
import org.hibernate.tool.hbm2ddl.DatabaseMetadata;
import org.hibernate.tool.hbm2ddl.SchemaUpdateScript;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.cli.util.AbstractCLI;
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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Generate a database update script.
 * @see InitializeDatabaseCli
 */
public class GenerateDatabaseUpdateCli extends AbstractCLI {

    private static final String
            CREATE_OPTION = "c",
            VENDOR_OPTION = "vendor",
            OUTPUT_FILE_OPTION = "o";

    @Autowired
    private DataSource dataSource;

    @Autowired
    private LocalSessionFactoryBean factory;

    private boolean create;

    private Dialect dialect;

    @Nullable
    private Path outputFile;

    @Nullable
    @Override
    public String getCommandName() {
        return "generateDatabaseUpdate";
    }

    @Nullable
    @Override
    public String getShortDesc() {
        return "Generate SQL statements to update the database";
    }

    @Override
    public CommandGroup getCommandGroup() {
        return CommandGroup.SYSTEM;
    }

    @Override
    protected void buildOptions( Options options ) {
        options.addOption( CREATE_OPTION, "create", false, "Generate a creation script" );
        options.addOption( VENDOR_OPTION, "vendor", true, "Vendor to use to generate SQL statements (either mysql or h2, defaults to mysql)" );
        options.addOption( Option.builder( OUTPUT_FILE_OPTION ).longOpt( "output-file" ).hasArg().type( Path.class ).desc( "File destination for the update script (defaults to stdout)" ).build() );
    }

    @Override
    protected void processOptions( CommandLine commandLine ) throws ParseException {
        create = commandLine.hasOption( CREATE_OPTION );
        if ( commandLine.hasOption( VENDOR_OPTION ) ) {
            String dialectStr = commandLine.getOptionValue( VENDOR_OPTION );
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
        outputFile = commandLine.getParsedOptionValue( OUTPUT_FILE_OPTION );
    }

    @Override
    protected void doWork() throws Exception {
        List<String> sqlStatements;
        if ( create ) {
            sqlStatements = Arrays.asList( factory.getConfiguration().generateSchemaCreationScript( dialect ) );
        } else {
            DatabaseMetadata dm;
            try ( Connection c = dataSource.getConnection() ) {
                dm = new DatabaseMetadata( c, dialect, factory.getConfiguration() );
            }
            sqlStatements = factory.getConfiguration()
                    .generateSchemaUpdateScriptList( dialect, dm )
                    .stream()
                    .map( SchemaUpdateScript::getScript )
                    .collect( Collectors.toList() );
        }
        try ( PrintWriter writer = getWriter() ) {
            for ( String sqlUpdate : sqlStatements ) {
                writer.println( sqlUpdate + ";" );
            }
        }
    }

    public PrintWriter getWriter() throws IOException {
        return outputFile != null ? new PrintWriter( Files.newBufferedWriter( outputFile ) ) : new PrintWriter( getCliContext().getOutputStream() );
    }
}
