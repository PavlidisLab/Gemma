package ubic.gemma.apps;

import com.hp.hpl.jena.tdb.TDBLoader;
import com.hp.hpl.jena.tdb.base.file.Location;
import com.hp.hpl.jena.tdb.setup.StoreParams;
import com.hp.hpl.jena.tdb.store.DatasetGraphTDB;
import com.hp.hpl.jena.tdb.sys.TDBMaker;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.file.PathUtils;
import org.apache.commons.io.file.StandardDeleteOption;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import ubic.basecode.ontology.jena.TdbOntologyService;
import ubic.basecode.ontology.providers.OntologyService;
import ubic.gemma.cli.util.AbstractCLI;
import ubic.gemma.core.loader.util.ftp.FTPClientFactory;
import ubic.gemma.core.util.SimpleDownloader;
import ubic.gemma.core.util.SimpleRetryPolicy;
import ubic.gemma.core.util.SimpleThreadFactory;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UnifiedOntologyUpdaterCli extends AbstractCLI {

    @Autowired
    private FTPClientFactory ftpClientFactory;

    @Autowired
    @Qualifier("unifiedOntologyService")
    private OntologyService unifiedOntologyService;

    @Value("${gemma.ontology.unified.sources.dir}")
    private Path sourcesDir;

    @Value("${gemma.ontology.unified.dir}")
    private Path destDir;

    /**
     * Indicate that the destination directory is not the default one.
     */
    private boolean differentDestDir = false;
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
        options.addOption( Option.builder( "sourcesDir" ).longOpt( "sources-dir" ).hasArg( true ).type( Path.class )
                .desc( "Destination where ontology source files are downloaded. Default is " + sourcesDir + "." ).build() );
        options.addOption( Option.builder( "destDir" ).longOpt( "dest-dir" ).hasArg( true ).type( Path.class )
                .desc( "Destination where to generate the TDB dataset. Default is " + destDir + "." ).build() );
        options.addOption( "skipDownload", "skip-download", false, "Skip download of ontology files" );
        options.addOption( "force", "force", false, "Force download of ontology files, even if they are up-to-date." );
        addThreadsOption( options );
    }

    @Override
    protected void processOptions( CommandLine commandLine ) throws ParseException {
        if ( commandLine.hasOption( "destDir" ) ) {
            Path dir = Paths.get( commandLine.getOptionValue( "destDir" ) );
            differentDestDir = !destDir.equals( dir );
            destDir = dir;
        }
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
                Path dest = sourcesDir.resolve( fileName );
                if ( Files.exists( dest ) ) {
                    downloadedFiles.add( dest.toString() );
                } else {
                    log.warn( "There is no downloaded file for " + url + ", it will not be included in the TDB dataset." );
                }
            }
        } else {
            ExecutorService executor = Executors.newFixedThreadPool( getNumThreads(), new SimpleThreadFactory( "gemma-unified-ontology-downloader-thread-" ) );
            try {
                SimpleDownloader downloader = new SimpleDownloader( new SimpleRetryPolicy( 3, 1000, 1.5 ), ftpClientFactory, executor );
                for ( String urlS : urls ) {
                    URL url = new URL( urlS );
                    String fileName = Paths.get( url.getFile() ).getFileName().toString();
                    Path dest = sourcesDir.resolve( fileName );
                    downloader.download( url, dest, force );
                    downloadedFiles.add( dest.toString() );
                }
            } finally {
                executor.shutdown();
            }
        }
        if ( downloadedFiles.isEmpty() ) {
            throw new IllegalStateException( "No ontology files to use for creating the TDB dataset." );
        }
        // update sources
        Path newDir = destDir.resolveSibling( destDir.getFileName() + ".new" );
        if ( Files.exists( newDir ) ) {
            throw new IllegalStateException( "There is a already a directory at " + newDir + ", remove it first." );
        }
        try {
            createTdbDataset( newDir, downloadedFiles );
            // FIXME: Jena cannot open a TDB directory in read-only mode, so we can't really use this right now.
            // removeWritePermissions( newDir );
            // make sure it works
            verifyTdbDataset( newDir );
            moveToFinalLocation( newDir );
            rebuildSearchIndex();
        } catch ( Exception e ) {
            if ( Files.exists( newDir ) ) {
                try {
                    PathUtils.deleteDirectory( newDir, StandardDeleteOption.OVERRIDE_READ_ONLY );
                } catch ( IOException e2 ) {
                    log.error( "Failed to delete " + newDir + ".", e2 );
                }
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

    private void verifyTdbDataset( Path newDir ) {
        log.info( "Verifying TDB dataset at " + newDir + "..." );
        try ( TdbOntologyService os = new TdbOntologyService( "Test", newDir, null, true, null ) ) {
            os.initialize( false, false );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    @SuppressWarnings("unused")
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
        log.info( "Moving " + newDir + " to " + destDir + "..." );
        if ( Files.exists( destDir ) ) {
            Path oldDir = destDir.resolveSibling( destDir.getFileName() + ".old" );
            if ( Files.exists( oldDir ) ) {
                throw new IllegalStateException( "There is a already a directory at " + oldDir + ", remove it first." );
            }
            Files.move( destDir, oldDir );
            Files.move( newDir, destDir );
            try {
                PathUtils.deleteDirectory( oldDir, StandardDeleteOption.OVERRIDE_READ_ONLY );
            } catch ( IOException e ) {
                log.error( "Failed to delete " + oldDir + ".", e );
            }
        } else {
            Files.move( newDir, destDir );
        }
    }

    private void rebuildSearchIndex() {
        if ( differentDestDir ) {
            log.warn( "Destination directory is not standard, will not re-build the search index." );
            return;
        }
        log.info( "Reindexing the unified ontology..." );
        unifiedOntologyService.initialize( true, true );
    }
}
