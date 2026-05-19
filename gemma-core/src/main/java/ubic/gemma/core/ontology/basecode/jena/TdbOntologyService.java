/*
 * Originated in baseCode (ubic.basecode.ontology.*); pulled in-tree for Gemma 2.0
 * (Phase 3 search/ontology Step 3). Ported from Jena 2.x (com.hp.hpl.jena.*)
 * to Jena 4.x (org.apache.jena.*) namespace. Configuration-related lookups
 * continue to use baseCode's ubic.basecode.util.Configuration via the
 * still-classpath baseCode JAR.
 */
package ubic.gemma.core.ontology.basecode.jena;

import org.apache.jena.query.Dataset;
import org.apache.jena.tdb.TDBFactory;
import org.apache.jena.tdb.base.file.Location;
import org.apache.commons.io.file.PathUtils;
import ubic.gemma.core.ontology.basecode.model.OntologyModel;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An implementation based on Jena TDB.
 * @author poirigui
 */
public class TdbOntologyService extends AbstractOntologyService {

    private final Path tdbDir;
    private final String modelName;
    private final boolean readOnly;

    @Nullable
    private Dataset dataset;

    @Nullable
    private Path tempDir;

    private boolean deleteTempDir = false;

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
                Location loc = Location.create( tdbDir.toString() );
                loc.getLock().obtain();
                try {
                    Set<Path> filesToLink;
                    try ( Stream<Path> z = Files.list( tdbDir ) ) {
                        filesToLink = z.collect( Collectors.toSet() );
                    }
                    if ( tempDir == null ) {
                        log.info( "Creating temporary directory for read-only TDB model." );
                        tempDir = Files.createTempDirectory( getOntologyName() + ".tdb" );
                        deleteTempDir = true;
                    } else if ( !Files.exists( tempDir ) ) {
                        log.info( "Creating temporary directory at {} for read-only TDB model.", tempDir );
                        Files.createDirectories( tempDir );
                        deleteTempDir = true;
                    } else {
                        log.info( "Reusing existing temporary directory at {} for read-only TDB model. Note that the directory will not be removed.", tempDir );
                        // never delete a temporary directory that wasn't created by this service (I've learned this the hard way!)
                        deleteTempDir = false;
                    }
                    for ( Path p : filesToLink ) {
                        Files.copy( p, tempDir.resolve( p.getFileName() ) );
                    }
                } finally {
                    loc.getLock().release();
                }
                log.info( "Reading read-only TDB model from {}.", tempDir );
                assert tempDir != null;
                dataset = TDBFactory.createDataset( tempDir.toString() );
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
     * Set the temporary directory used when opening the TDB in read-only mode.
     * <p>
     * This can only be set once, before the first model is loaded. Note that the directory will be removed when the
     * service is closed.
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
            if ( tempDir != null && deleteTempDir ) {
                PathUtils.delete( tempDir );
            }
        }
    }
}
