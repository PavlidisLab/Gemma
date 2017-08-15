package ubic.gemma.web.services.rest.util.args;

import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.PhysicalLocationValueObject;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneValueObject;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by tesarst on 16/05/17.
 * String argument type for Gene API. Represents the official symbol, which is non-taxon specific, so the argument
 * effectively represents all homologues that match the symbol.
 */
public class GeneSymbolArg extends GeneArg<String> {

    private static final String ID_NAME = "Official Symbol";

    GeneSymbolArg( String s ) {
        this.value = s;
        this.nullCause = this.getDefaultError();
    }

    /**
     * Tries to retrieve a Gene object based on its official symbol. Note that if the gene has multiple homologies,
     * a random taxon homology will be returned.
     *
     * @param service the GeneService that handles the search.
     * @return a Gene object of random homology, if found, or null, if the original argument value was null, or if the search did not find
     * any Gene that would match the string.
     */
    @Override
    public Gene getPersistentObject( GeneService service ) {
        Gene gene;
        Collection<Gene> genes = this.value == null || this.value.isEmpty() ? null : service.findByOfficialSymbol( this.value );
        if ( genes == null || genes.isEmpty() ) {
            gene = null;
        }else {
            gene = genes.iterator().next();
        }
        return check(gene);
    }

    @Override
    public Collection<GeneValueObject> getValueObjects( GeneService service ) {
        return service.loadValueObjects( service.findByOfficialSymbol( this.value ) );
    }

    @Override
    public Collection<PhysicalLocationValueObject> getGeneLocation( GeneService geneService ) {
        Collection<Gene> genes = geneService.findByOfficialSymbol( this.value );
        Collection<PhysicalLocationValueObject> gVos = new ArrayList<>( genes.size() );
        for ( Gene gene : genes ) {
            gVos.addAll( geneService.getPhysicalLocationsValueObjects( gene ) );
        }
        return gVos;
    }

    @Override
    public Collection<PhysicalLocationValueObject> getGeneLocation( GeneService geneService, Taxon taxon ) {
        Gene gene = geneService.findByOfficialSymbol( this.value, taxon );
        if ( gene == null )
            return null;
        if ( !gene.getTaxon().equals( taxon ) ) {
            this.nullCause = this.getTaxonError();
            return null;
        }
        return geneService.getPhysicalLocationsValueObjects( gene );
    }

    @Override
    String getIdentifierName() {
        return ID_NAME;
    }
}
