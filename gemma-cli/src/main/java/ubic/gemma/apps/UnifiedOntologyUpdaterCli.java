package ubic.gemma.apps;

import com.hp.hpl.jena.tdb.TDBLoader;
import com.hp.hpl.jena.tdb.base.file.Location;
import com.hp.hpl.jena.tdb.setup.StoreParams;
import com.hp.hpl.jena.tdb.store.DatasetGraphTDB;
import com.hp.hpl.jena.tdb.sys.TDBMaker;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.file.PathUtils;
import org.apache.commons.io.file.StandardDeleteOption;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import ubic.basecode.ontology.providers.OntologyService;
import ubic.gemma.cli.util.AbstractCLI;
import ubic.gemma.core.loader.util.ftp.FTPClientFactory;
import ubic.gemma.core.util.SimpleDownloader;
import ubic.gemma.core.util.SimpleRetryPolicy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UnifiedOntologyUpdaterCli extends AbstractCLI {

    @Autowired
    private FTPClientFactory ftpClientFactory;

    @Autowired
    @Qualifier("unifiedOntologyService")
    private OntologyService unifiedOntologyService;

    @Value("${gemma.ontology.unified.dir}")
    private Path tdbDir;

    private boolean skipDownload = false;
    private boolean force = false;

    @Override
    public String getCommandName() {
        return "updateUnifiedOntology";
    }

    @Override
    public String getShortDesc() {
        return "Update or initialize the unified ontology";
    }

    @Override
    protected void buildOptions( Options options ) {
        options.addOption( "skipDownload", "skip-download", false, "Skip download of ontology files" );
        options.addOption( "force", "force", false, "Force download of ontology files, even if they are up-to-date." );
    }

    @Override
    protected void processOptions( CommandLine commandLine ) throws ParseException {
        skipDownload = commandLine.hasOption( "skipDownload" );
        force = commandLine.hasOption( "force" );
        if ( skipDownload && force ) {
            throw new ParseException( "Cannot force download if it is skipped." );
        }
    }

    @Override
    protected void doWork() throws Exception {
        Resource res = new ClassPathResource( "/ubic/gemma/core/ontology/unifiedOntology.urls.txt" );
        Set<String> urls = new BufferedReader( new InputStreamReader( res.getInputStream(), StandardCharsets.UTF_8 ) ).lines()
                .filter( line -> !line.startsWith( "#" ) )
                .collect( Collectors.toSet() );
        List<String> downloadedFiles = new ArrayList<>();
        if ( skipDownload ) {
            log.info( "Skipping download of ontology files, will only create the TDB dataset from existing files." );
            for ( String urlS : urls ) {
                URL url = new URL( urlS );
                String fileName = Paths.get( url.getFile() ).getFileName().toString();
                Path dest = tdbDir.getParent().resolve( "sources" ).resolve( fileName );
                if ( Files.exists( dest ) ) {
                    downloadedFiles.add( dest.toString() );
                }
            }
        } else {
            SimpleDownloader downloader = new SimpleDownloader( new SimpleRetryPolicy( 3, 1000, 1.5 ), ftpClientFactory );
            for ( String urlS : urls ) {
                URL url = new URL( urlS );
                String fileName = Paths.get( url.getFile() ).getFileName().toString();
                Path dest = tdbDir.getParent().resolve( "sources" ).resolve( fileName );
                downloader.download( url, dest, force );
                downloadedFiles.add( dest.toString() );
            }
        }
        if ( downloadedFiles.isEmpty() ) {
            throw new IllegalStateException( "No ontology files to use for creating the TDB dataset." );
        }
        // update sources
        Path newDir = tdbDir.resolveSibling( tdbDir.getFileName() + ".new" );
        if ( Files.exists( newDir ) ) {
            throw new IllegalStateException( "There is a already a directory at " + newDir + ", remove it first." );
        }
        try {
            createTdbDataset( newDir, downloadedFiles );
            removeWritePermissions( newDir );
            moveToFinalLocation( newDir );
            rebuildSearchIndex();
        } catch ( Exception e ) {
            if ( Files.exists( newDir ) ) {
                Files.delete( newDir );
            }
            throw e;
        }
    }

    private void createTdbDataset( Path newDir, List<String> downloadedFiles ) {
        Location loc = Location.create( newDir.toString() );
        try {
            log.info( "Creating TDB dataset at " + newDir + "..." );
            DatasetGraphTDB dataset = TDBMaker.createDatasetGraphTDB( loc, StoreParams.getDftStoreParams() );
            TDBLoader.load( dataset, downloadedFiles, true );
        } finally {
            TDBMaker.releaseLocation( loc );
        }
    }

    private void removeWritePermissions( Path newDir ) throws IOException {
        // remove write permissions
        log.info( "Removing write permissions on " + newDir + "..." );
        try ( Stream<Path> s = Files.walk( newDir ) ) {
            s.forEach( path -> {
                try {
                    Set<PosixFilePermission> permissions = Files.getPosixFilePermissions( path );
                    permissions.remove( PosixFilePermission.OWNER_WRITE );
                    permissions.remove( PosixFilePermission.GROUP_WRITE );
                    permissions.remove( PosixFilePermission.OTHERS_WRITE );
                    Files.setPosixFilePermissions( path, permissions );
                } catch ( IOException e ) {
                    log.error( "Failed to remove write permissions on " + path + ".", e );
                }
            } );
        }
    }

    private void moveToFinalLocation( Path newDir ) throws IOException {
        log.info( "Moving " + newDir + " to " + tdbDir + "..." );
        if ( Files.exists( tdbDir ) ) {
            Path oldDir = tdbDir.resolveSibling( tdbDir.getFileName() + ".old" );
            if ( Files.exists( oldDir ) ) {
                throw new IllegalStateException( "There is a already a directory at " + oldDir + ", remove it first." );
            }
            Files.move( tdbDir, oldDir );
            Files.move( newDir, tdbDir );
            PathUtils.deleteDirectory( oldDir, StandardDeleteOption.OVERRIDE_READ_ONLY );
        } else {
            Files.move( newDir, tdbDir );
        }
    }

    private void rebuildSearchIndex() {
        log.info( "Reindexing the unified ontology..." );
        unifiedOntologyService.initialize( true, true );
    }
}
