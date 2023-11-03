package ubic.gemma.core.ontology;

import ubic.basecode.ontology.model.OntologyIndividual;

import javax.annotation.Nullable;
import java.util.Set;

/**
 * Ontology service for factor values and their annotations.
 * <p>
 * There are two kind of entities represented in his ontologies:
 * <ul>
 *     <li>Factor values (i.e. http://gemma.msl.ubc.ca/ont/TGFVO/1)</li>
 *     <li>Factor value annotations (i.e. http://gemma.msl.ubc.ca/ont/TGFVO/1/2)</li>
 * </ul>
 * TODO: fully implement the {@link ubic.basecode.ontology.providers.OntologyService} interface.
 */
public interface FactorValueOntologyService {

    String URI_PREFIX = "http://gemma.msl.ubc.ca/ont/TGFVO/";

    static String factorValueUri( Long factorValueId ) {
        return FactorValueOntologyService.URI_PREFIX + factorValueId;
    }

    static String factorValueAnnotationUri( Long factorValueId, Long id ) {
        return FactorValueOntologyServiceImpl.URI_PREFIX + factorValueId + "/" + id;
    }

    /**
     * Obtain an individual from the ontology by URI.
     */
    @Nullable
    OntologyIndividual getIndividual( String uri );

    /**
     * Obtain individuals related to the given URI.
     * <p>
     * In general, this is used to retrieve annotations for a factor value.
     */
    Set<OntologyIndividual> getRelatedIndividuals( String uri );
}
