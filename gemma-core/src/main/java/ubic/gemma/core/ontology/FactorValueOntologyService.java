package ubic.gemma.core.ontology;

import lombok.Value;
import ubic.basecode.ontology.model.OntologyIndividual;
import ubic.basecode.ontology.model.OntologyProperty;

import javax.annotation.Nullable;
import java.io.Writer;
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
     * Obtain annotations belonging to the given URI representing a factor value.
     */
    Set<OntologyIndividual> getFactorValueAnnotations( String uri );

    /**
     * Represents an ontology statement.
     * TODO: move this into baseCode.
     */
    @Value
    class OntologyStatement {
        OntologyIndividual subject;
        OntologyProperty predicate;
        OntologyIndividual object;
    }

    /**
     * Obtain statements related to the given URI representing a factor value.
     */
    Set<OntologyStatement> getFactorValueStatements( String uri );

    void writeToRdf( String iri, Writer writer );
}
