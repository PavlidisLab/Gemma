package ubic.gemma.core.ontology.jena;

import ubic.gemma.core.ontology.model.OntologyModel;

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
