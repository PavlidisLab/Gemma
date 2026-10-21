package ubic.gemma.core.ontology.jena;

import org.apache.jena.query.Dataset;
import org.apache.jena.tdb.TDBFactory;
import org.apache.jena.tdb.TDBLoader;
import org.apache.jena.tdb.base.file.Location;
import org.apache.jena.tdb.base.file.LocationLock;
import org.apache.jena.tdb.sys.TDBInternal;
import org.apache.commons.io.file.PathUtils;
import ubic.gemma.core.ontology.model.OntologyModel;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An implementation based on Jena TDB.
 * <p>
 * The store is TDB1 ({@code org.apache.jena.tdb}), not TDB2. The model is read from request threads and the
 * ontology loader thread with no transaction open, which TDB1 allows and TDB2 refuses with "Not in a transaction".
 * {@link #createDataset(Path, List)} and {@link ubic.gemma.core.ontology.search.JenaTextOntologySearchService} open the
 * same directory and must stay on the same TDB version.
 * @author poirigui
 */
public class TdbOntologyService extends AbstractOntologyService {

    /**
     * Name of the lock file TDB1 keeps in a store directory. It records the PID of its owner, so it must not be copied
     * into the read-only copy.
     */
    private static final String TDB_LOCK_FILE = "tdb.lock";

    /**
     * Load OWL/RDF files into a new TDB1 store at the given directory.
     * <p>
     * This is the format {@link TdbOntologyService} reads.
     */
    public static void createDataset( Path tdbDir, List<Path> sourceFiles ) {
        Dataset ds = TDBFactory.createDataset( Location.create( tdbDir.toString() ) );
        try {
            TDBLoader.load( TDBInternal.getDatasetGraphTDB( ds ), sourceFiles.stream().map( Path::toString ).collect( Collectors.toList() ) );
        } finally {
            TDBFactory.release( ds );
        }
    }

    private final Path tdbDir;
    private final String modelName;
    private final boolean readOnly;

    @Nullable
    private Dataset dataset;

    @Nullable
    private Path tempDir;

    /**
     * Directory holding the read-only copy, created by this service and removed on {@link #close()}.
     */
    @Nullable
    private Path copyDir;

    /**
     * @param readOnly open the TDB database in read-only mode, allowing multiple JVMs to share a common TDB. For this
     *                 to work safely, all the TDB files must not be writable. Additionally, a temporary directory with
     *                 symbolic links will be created since Jena will still be using a lock file.
     */
    public TdbOntologyService( String ontologyName, Path tdbDir, @Nullable String modelName, boolean ontologyEnabled, @Nullable String cacheName, boolean readOnly ) {
        super( ontologyName, tdbDir.toUri().toString(), ontologyEnabled, cacheName );
        this.tdbDir = tdbDir;
        this.modelName = modelName;
        this.readOnly = readOnly;
    }

    @Override
    protected OntologyModel loadModel( boolean processImports, LanguageLevel languageLevel, InferenceMode inferenceMode ) throws IOException {
        if ( dataset == null ) {
            if ( readOnly ) {
                // lock the location and make a copy
                LocationLock lock = Location.create( tdbDir.toString() ).getLock();
                // Another dataset in this JVM (the ontology search service) may already hold the lock. Releasing
                // it here would delete that dataset's lock file, so only release a lock this method took.
                boolean alreadyOwned = lock.isOwned();
                if ( !alreadyOwned ) {
                    lock.obtain();
                }
                try {
                    // A fresh directory on every load: copying into a directory left over from an earlier run
                    // fails with FileAlreadyExistsException on the first file.
                    if ( tempDir == null ) {
                        copyDir = Files.createTempDirectory( getOntologyName() + ".tdb" );
                    } else {
                        Files.createDirectories( tempDir );
                        copyDir = Files.createTempDirectory( tempDir, "tdb" );
                    }
                    log.info( "Copying TDB model from {} to {} for read-only access.", tdbDir, copyDir );
                    // TDB1 keeps a store in regular files; subdirectories (the search index under .text) are not
                    // part of it.
                    Set<Path> filesToCopy;
                    try ( Stream<Path> z = Files.list( tdbDir ) ) {
                        filesToCopy = z
                                .filter( Files::isRegularFile )
                                .filter( p -> !p.getFileName().toString().equals( TDB_LOCK_FILE ) )
                                .collect( Collectors.toSet() );
                    }
                    for ( Path p : filesToCopy ) {
                        Files.copy( p, copyDir.resolve( p.getFileName() ) );
                    }
                } finally {
                    if ( !alreadyOwned ) {
                        lock.release();
                    }
                }
                log.info( "Reading read-only TDB model from {}.", copyDir );
                dataset = TDBFactory.createDataset( copyDir.toString() );
            } else {
                dataset = TDBFactory.createDataset( tdbDir.toString() );
            }
        }
        return new OntologyModelImpl( OntologyLoader.createTdbModel( dataset, modelName, processImports, getSpec( languageLevel, inferenceMode ), readOnly ) );
    }

    @Override
    protected OntologyModel loadModelFromStream( InputStream is, boolean processImports, LanguageLevel languageLevel, InferenceMode inferenceMode ) {
        throw new UnsupportedOperationException( "TDB cannot be loaded from an input stream." );
    }

    /**
     * Set the directory under which the read-only copy of the TDB is made.
     * <p>
     * This can only be set once, before the first model is loaded. The copy goes in a new subdirectory, which is
     * removed when the service is closed; the directory itself and anything else in it are left alone.
     */
    public void setTempDir( Path tempDir ) {
        if ( this.tempDir != null ) {
            throw new IllegalStateException( "Temporary directory has already been set." );
        }
        this.tempDir = tempDir;
    }

    @Override
    public void close() throws Exception {
        try {
            super.close();
        } finally {
            if ( dataset != null ) {
                TDBFactory.release( dataset );
            }
            if ( copyDir != null ) {
                PathUtils.deleteDirectory( copyDir );
            }
        }
    }
}
