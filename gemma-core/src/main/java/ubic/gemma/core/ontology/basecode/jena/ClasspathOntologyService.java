/*
 * Originated in baseCode (ubic.basecode.ontology.*); pulled in-tree for Gemma 2.0
 * (Phase 3 search/ontology Step 3). Ported from Jena 2.x (com.hp.hpl.jena.*)
 * to Jena 4.x (org.apache.jena.*) namespace. Configuration-related lookups
 * continue to use baseCode's ubic.basecode.util.Configuration via the
 * still-classpath baseCode JAR.
 */
package ubic.gemma.core.ontology.basecode.jena;

import ubic.gemma.core.ontology.basecode.model.OntologyModel;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

/**
 * An ontology service that loads an ontology from a classpath resource.
 * @author poirigui
 */
public class ClasspathOntologyService extends AbstractOntologyService {

    private final String classpathResource;

    public ClasspathOntologyService( String name, String classpathResource, boolean ontologyEnabled, @Nullable String cacheName ) {
        super( name, "classpath:" + classpathResource, ontologyEnabled, cacheName );
        this.classpathResource = classpathResource;
    }

    @Override
    protected OntologyModel loadModel( boolean processImports, LanguageLevel languageLevel, InferenceMode inferenceMode ) throws IOException {
        try ( InputStream stream = getClass().getResourceAsStream( classpathResource ) ) {
            if ( stream == null ) {
                throw new RuntimeException( String.format( "The NIF ontology was not found in classpath at %s.", classpathResource ) );
            }
            return loadModelFromStream( classpathResource.endsWith( ".gz" ) ? new GZIPInputStream( stream ) : stream, processImports, languageLevel, inferenceMode );
        }
    }

    @Override
    protected OntologyModel loadModelFromStream( InputStream is, boolean processImports, LanguageLevel languageLevel, InferenceMode inferenceMode ) throws IOException {
        return new OntologyModelImpl( OntologyLoader.createMemoryModel( is, this.getOntologyUrl(), processImports, this.getSpec( languageLevel, inferenceMode ) ) );
    }
}
