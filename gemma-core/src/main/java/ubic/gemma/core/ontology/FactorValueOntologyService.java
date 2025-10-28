package ubic.gemma.core.ontology;

import ubic.basecode.ontology.model.OntologyIndividual;
import ubic.basecode.ontology.model.OntologyStatement;
import ubic.gemma.persistence.util.Slice;

import javax.annotation.Nullable;
import java.io.Writer;
import java.util.Collection;
import java.util.Set;

/**
 * Ontology service for factor values and their annotations.
 * <p>
 * There are two kind of entities represented in his ontologies:
 * <ul>
 *     <li>Factor values (i.e. http://gemma.msl.ubc.ca/ont/TGFVO/1)</li>
 *     <li>Factor value annotations (i.e. http://gemma.msl.ubc.ca/ont/TGFVO/1/2) which can be either a subject, object or a characteristic</li>
 * </ul>
 * TODO: fully implement the {@link ubic.basecode.ontology.providers.OntologyService} interface.
 */
public interface FactorValueOntologyService {

    /**
     * Obtain an individual from the ontology by URI.
     */
    @Nullable
    OntologyIndividual getIndividual( String uri );

    /**
     * Obtain all the factor value in the ontology.
     */
    Slice<OntologyIndividual> getFactorValues( int offset, int limit );

    Collection<String> getFactorValueUris();

    /**
     * Obtain all the factor value URIs in the ontology.
     */
    Slice<String> getFactorValueUris( int offset, int limit );

    /**
     * Obtain annotations belonging to the given URI representing a factor value.
     */
    Set<OntologyIndividual> getFactorValueAnnotations( String uri );

    /**
     * Obtain statements related to the given URI representing a factor value.
     */
    Set<OntologyStatement> getFactorValueStatements( String uri );

    /**
     * Write multiple individuals represented by the given URIs to RDF.
     */
    void writeToRdf( Collection<String> uri, Writer writer );

    /**
     * Write multiple individuals represented by the given URIs to RDF, ignoring ACLs.
     * <p>
     * use this only if the FVs were prefiltered with {@link #getFactorValueUris()} or {@link #getFactorValues(int, int)}
     */
    void writeToRdfIgnoreAcls( Collection<String> uri, Writer writer );
}
