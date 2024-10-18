package ubic.gemma.rest.util.args;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ubic.gemma.core.ontology.providers.GeneOntologyService;
import ubic.gemma.model.expression.designElement.CompositeSequenceValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.GeneOntologyTermValueObject;
import ubic.gemma.model.genome.PhysicalLocationValueObject;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;
import ubic.gemma.persistence.service.genome.gene.GeneService;
import ubic.gemma.persistence.util.Filter;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;

import javax.annotation.Nonnull;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import java.util.ArrayList;
import java.util.List;

@Service
public class GeneArgService extends AbstractEntityArgService<Gene, GeneService> {

    private final GeneOntologyService geneOntologyService;
    private final CompositeSequenceService compositeSequenceService;

    @Autowired
    public GeneArgService( GeneService service, GeneOntologyService geneOntologyService, CompositeSequenceService compositeSequenceService ) {
        super( service );
        this.geneOntologyService = geneOntologyService;
        this.compositeSequenceService = compositeSequenceService;
    }

    /**
     * {@inheritDoc}
     * @throws BadRequestException if more than one gene match the supplied gene argument
     */
    @Nonnull
    @Override
    public Gene getEntity( AbstractEntityArg<?, Gene, GeneService> entityArg ) throws NotFoundException, BadRequestException {
        List<Gene> matchedGenes = getEntities( entityArg );
        if ( matchedGenes.isEmpty() ) {
            return checkEntity( entityArg, null );
        } else if ( matchedGenes.size() > 1 ) {
            throw new BadRequestException( "Gene identifier " + entityArg + " matches more than one gene, supply a taxon to disambiguate or use a different type of identifier such as an NCBI or Ensembl ID." );
        } else {
            return matchedGenes.iterator().next();
        }
    }

    /**
     * Obtain a gene from a specific taxon.
     * @throws BadRequestException if more than one gene match the supplied gene argumen in the given taxon
     */
    @Nonnull
    public Gene getEntityWithTaxon( GeneArg<?> entityArg, Taxon taxon ) {
        List<Gene> matchedGenes = getEntitiesWithTaxon( entityArg, taxon );
        if ( matchedGenes.isEmpty() ) {
            return checkEntity( entityArg, null );
        } else if ( matchedGenes.size() > 1 ) {
            throw new BadRequestException( "Gene identifier " + entityArg + " matches more than one gene in " + taxon.getCommonName() + ", use a different type of identifier such as an NCBI or Ensembl ID." );
        } else {
            return matchedGenes.iterator().next();
        }
    }

    /**
     * Obtain genes from a specific taxon.
     */
    public List<Gene> getEntitiesWithTaxon( GeneArg<?> genes, Taxon taxon ) {
        return genes.getEntitiesWithTaxon( service, taxon );
    }

    /**
     * Obtain genes from a specific taxon.
     */
    public List<Gene> getEntitiesWithTaxon( GeneArrayArg genes, Taxon taxon ) {
        List<Gene> objects = new ArrayList<>( genes.getValue().size() );
        for ( String s : genes.getValue() ) {
            GeneArg<?> arg = ( GeneArg<?> ) entityArgValueOf( genes.getEntityArgClass(), s );
            objects.add( getEntityWithTaxon( arg, taxon ) );
        }
        return objects;
    }

    public Slice<GeneValueObject> getGenes( int offset, int limit ) {
        return service.loadValueObjects( null, service.getSort( "id", Sort.Direction.ASC, Sort.NullMode.LAST ), offset, limit );
    }

    public Slice<GeneValueObject> getGenesInTaxon( Taxon taxon, int offset, int limit ) {
        return service.loadValueObjects( Filters.by( service.getFilter( "taxon.id", Long.class, Filter.Operator.eq, taxon.getId() ) ), service.getSort( "id", Sort.Direction.ASC, Sort.NullMode.LAST ), offset, limit );
    }

    /**
     * @return a collection of Gene value objects..
     */
    public List<GeneValueObject> getGenesInTaxon( GeneArrayArg arg, Taxon taxon ) {
        return service.loadValueObjects( getEntitiesWithTaxon( arg, taxon ) );
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
    public List<PhysicalLocationValueObject> getGeneLocationInTaxon( GeneArg<?> arg, Taxon taxon ) {
        return service.getPhysicalLocationsValueObjects( getEntityWithTaxon( arg, taxon ) );
    }

    /**
     * Returns GO terms for the gene that this GeneArg represents.
     */
    public List<GeneOntologyTermValueObject> getGeneGoTerms( GeneArg<?> arg ) {
        return geneOntologyService.getValueObjects( this.getEntity( arg ) );
    }

    /**
     * Obtain GO terms for the gene in the given taxon.
     */
    public List<GeneOntologyTermValueObject> getGeneGoTermsInTaxon( GeneArg<?> geneArg, Taxon taxon ) {
        return geneOntologyService.getValueObjects( this.getEntityWithTaxon( geneArg, taxon ) );
    }

    /**
     * Obtain probes for the gene across all platforms.
     */
    public Slice<CompositeSequenceValueObject> getGeneProbes( GeneArg<?> geneArg, int offset, int limit ) {
        return compositeSequenceService.loadValueObjectsForGene( getEntity( geneArg ), offset, limit );
    }

    /**
     * Obtain probes for the gene in the given taxon across all platforms.
     */
    public Slice<CompositeSequenceValueObject> getGeneProbesInTaxon( GeneArg<?> geneArg, Taxon taxon, int offset, int limit ) {
        return compositeSequenceService.loadValueObjectsForGene( getEntityWithTaxon( geneArg, taxon ), offset, limit );
    }
}
