package ubic.gemma.rest.util.args;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.core.ontology.providers.GeneOntologyService;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.GeneOntologyTermValueObject;
import ubic.gemma.model.genome.PhysicalLocationValueObject;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneValueObject;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class GeneArgService extends AbstractEntityArgService<Gene, GeneService> {

    private final GeneOntologyService geneOntologyService;


    @Autowired
    public GeneArgService( GeneService service, GeneOntologyService geneOntologyService ) {
        super( service );
        this.geneOntologyService = geneOntologyService;
    }

    public Gene getEntityWithTaxon( GeneArg<?> arg, Taxon taxon ) {
        return checkEntity( arg, arg.getEntityWithTaxon( service, taxon ) );
    }

    /**
     * @return all genes that match the value of the GeneArg.
     */
    public List<GeneValueObject> getValueObjects( GeneArg<?> arg ) {
        return service.loadValueObjects( getEntities( arg ) );
    }

    /**
     * Returns all known locations of the gene(s) that this GeneArg represents.
     *
     * @return collection of physical location objects.
     */
    public List<PhysicalLocationValueObject> getGeneLocation( GeneArg<?> geneArg ) {
        List<Gene> genes = getEntities( geneArg );
        List<PhysicalLocationValueObject> gVos = new ArrayList<>( genes.size() );
        for ( Gene gene : genes ) {
            gVos.addAll( service.getPhysicalLocationsValueObjects( gene ) );
        }
        return gVos;
    }

    /**
     * Returns all known locations of the gene that this GeneArg represents.
     *
     * @param taxon       the taxon to limit the search to. Can be null.
     * @return collection of physical location objects.
     */
    public List<PhysicalLocationValueObject> getGeneLocation( GeneArg<?> arg, Taxon taxon ) {
        return service.getPhysicalLocationsValueObjects( getEntityWithTaxon( arg, taxon ) );
    }

    /**
     * @return a collection of Gene value objects..
     */
    public List<GeneValueObject> getGenesOnTaxon( GeneArg<?> arg, Taxon taxon ) {
        return this.getValueObjects( arg ).stream()
                .filter( vo -> Objects.equals( vo.getTaxonId(), taxon.getId() ) )
                .collect( Collectors.toList() );
    }

    /**
     * Returns GO terms for the gene that this GeneArg represents.
     *
     * @return collection of physical location objects.
     */
    public List<GeneOntologyTermValueObject> getGoTerms( GeneArg<?> arg ) {
        Gene gene = this.getEntity( arg );
        return geneOntologyService.getValueObjects( gene );
    }
}
