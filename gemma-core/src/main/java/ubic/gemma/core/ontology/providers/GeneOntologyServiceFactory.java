package ubic.gemma.core.ontology.providers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.persistence.service.association.Gene2GOAssociationService;

@Service
public class GeneOntologyServiceFactory extends OntologyServiceFactory<GeneOntologyService> {

    @Autowired
    private Gene2GOAssociationService gene2GOAssociationService;
    @Autowired
    private GeneService geneService;

    @Override
    protected GeneOntologyService createOntologyService() {
        return new GeneOntologyServiceImpl( gene2GOAssociationService, geneService );
    }
}
