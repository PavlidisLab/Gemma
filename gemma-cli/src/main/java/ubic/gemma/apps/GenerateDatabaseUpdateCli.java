package ubic.gemma.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.hibernate.dialect.Dialect;
// Renovations: Hibernate 5 removed org.hibernate.tool.hbm2ddl.{DatabaseMetadata,SchemaUpdateScript} in favour of
// the new org.hibernate.tool.schema.spi.* model driven from MetadataSources. doWork() below now no-ops; rewriting
// it against the new API is deferred (production schemas come from sql/migrations/*.sql).
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
        // Renovations: previously generated DDL via Configuration.generateSchemaCreationScript() and
        // generateSchemaUpdateScriptList(), both removed in Hibernate 5. Until this is rewritten against the
        // Hibernate 5 SchemaCreator/SchemaUpdate APIs (driven from MetadataSources), the command is a no-op.
        // Production schemas come from gemma-core/src/main/resources/sql/migrations/*.sql.
        try ( PrintWriter writer = getWriter() ) {
            writer.println( "-- GenerateDatabaseUpdateCli is disabled on the renovations branch." );
            writer.println( "-- Use the versioned scripts in gemma-core/src/main/resources/sql/migrations/ instead." );
        }
    }

    public PrintWriter getWriter() throws IOException {
        return outputFile != null ? new PrintWriter( Files.newBufferedWriter( outputFile ) ) : new PrintWriter( getCliContext().getOutputStream() );
    }
}
