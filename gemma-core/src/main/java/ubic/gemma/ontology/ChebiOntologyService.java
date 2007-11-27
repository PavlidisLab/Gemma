/**
 * 
 */
package ubic.gemma.ontology;

import java.io.IOException;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;

/**
 * Loads the CHEBI Ontology at startup in its own thread. 
 * Controlled in build.properties by load.chebiOntology = false (defaults to true). 
 * 
 * @author klc
 *
 * @spring.bean id="chebiOntologyService"
 */
public class ChebiOntologyService extends AbstractOntologyService {

	/* (non-Javadoc)
	 * @see ubic.gemma.ontology.AbstractOntologyService#getOntologyName()
	 */
	@Override
	protected String getOntologyName() {
		
		return "chebiOntology";
	}

	/* (non-Javadoc)
	 * @see ubic.gemma.ontology.AbstractOntologyService#getOntologyUrl()
	 */
	@Override
	protected String getOntologyUrl() {
		return "http://www.berkeleybop.org/ontologies/owl/CHEBI.owl";
	}

	/* (non-Javadoc)
	 * @see ubic.gemma.ontology.AbstractOntologyService#loadModel(java.lang.String, com.hp.hpl.jena.ontology.OntModelSpec)
	 */
	@Override
	protected OntModel loadModel(String url, OntModelSpec spec)
			throws IOException {
		
		return OntologyLoader.loadPersistentModel( url, false );
	}

}
