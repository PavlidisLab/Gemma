package ubic.gemma.web.services.rest.util.args;

import ubic.basecode.ontology.model.OntologyTerm;
import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.core.ontology.OntologyService;
import ubic.gemma.core.ontology.providers.GeneOntologyService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.PhysicalLocationValueObject;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneValueObject;

import java.util.Collection;
import java.util.Collections;

/**
 * Base class for GeneArg representing any of the identifiers of a Gene.
 *
 * @param <T>
 */
public abstract class GeneAnyIdArg<T> extends GeneArg<T> {

    @Override
    public Collection<GeneValueObject> getValueObjects( GeneService service ) {
        return Collections.singleton( service.loadValueObject( this.getPersistentObject( service ) ) );
    }

    @Override
    public Collection<PhysicalLocationValueObject> getGeneLocation( GeneService geneService ) {
        Gene gene = this.getPersistentObject( geneService );
        return gene == null ? null : geneService.getPhysicalLocationsValueObjects( gene );
    }

    @Override
    public Collection<PhysicalLocationValueObject> getGeneLocation( GeneService geneService, Taxon taxon ) {
        Gene gene = this.getPersistentObject( geneService );
        if ( gene == null )
            return null;
        if ( !gene.getTaxon().equals( taxon ) ) {
            this.nullCause = getTaxonError();
            return null;
        }
        return geneService.getPhysicalLocationsValueObjects( gene );
    }

}
