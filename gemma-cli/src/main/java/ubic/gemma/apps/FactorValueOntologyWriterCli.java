package ubic.gemma.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.file.PathUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import ubic.gemma.cli.util.AbstractAuthenticatedCLI;
import ubic.gemma.core.ontology.FactorValueOntologyService;
import ubic.gemma.core.util.locking.FileLockManager;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.zip.GZIPOutputStream;

public class FactorValueOntologyWriterCli extends AbstractAuthenticatedCLI {

    @Autowired
    private FactorValueOntologyService factorValueOntologyService;

    @Autowired
    private FileLockManager fileLockManager;

    @Value("${tgfvo.path}")
    private Path tgfvoPath;

    private Path outputFile;
    private boolean force;

    public FactorValueOntologyWriterCli() {
        // generate the file from the perspective of an anonymous user
        setAuthenticateAnonymously();
    }

    @Override
    public String getCommandName() {
        return "getTgfvo";
    }

    @Override
    public String getShortDesc() {
        return "Generate the OWL file for TGFVO.";
    }

    @Override
    protected void buildOptions( Options options ) {
        options.addOption( "o", "output-file" );
        options.addOption( "force", "force", false, "Force overwriting the output file if it exists." );
    }

    @Override
    protected void processOptions( CommandLine commandLine ) throws ParseException {
        if ( commandLine.hasOption( "o" ) ) {
            this.outputFile = commandLine.getParsedOptionValue( "o" );
        } else {
            this.outputFile = tgfvoPath;
        }
        this.force = commandLine.hasOption( "force" );
    }

    @Override
    protected void doAuthenticatedWork() throws Exception {
        Collection<String> uris = factorValueOntologyService.getFactorValueUris();
        log.info( "Writing " + uris.size() + " factor values to " + outputFile + "..." );
        try ( Writer writer = openFile( outputFile ) ) {
            factorValueOntologyService.writeToRdfIgnoreAcls( uris, writer );
        }
    }

    private Writer openFile( Path outputFile ) throws IOException {
        if ( !force && Files.exists( outputFile ) ) {
            throw new IllegalArgumentException( "Output file already exists: " + outputFile + ". Use -force,--force to overwrite it." );
        }
        PathUtils.createParentDirectories( outputFile );

        if ( outputFile.getFileName().toString().endsWith( ".gz" ) ) {
            return new OutputStreamWriter( new GZIPOutputStream( fileLockManager.newOutputStream( outputFile ) ) );
        } else {
            return Files.newBufferedWriter( outputFile );
        }
    }
}
