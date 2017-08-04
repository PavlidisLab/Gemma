package ubic.gemma.web.services.rest.util.args;

import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneValueObject;

import java.util.Collection;

/**
 * Created by tesarst on 16/05/17.
 * String argument type for Gene API. Can be either official symbol.
 */
public class GeneSymbolArg extends GeneArg<String> {

    GeneSymbolArg( String s ) {
        this.value = s;
        this.nullCause = "The identifier was recognised to be an official symbol, but no Gene with such symbol exists.";
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
        if(this.value == null){
            return null;
        }

        Collection<Gene> genes = service.findByOfficialSymbol( this.value );
        if(genes == null || genes.isEmpty()){
            return null;
        }
        return genes.iterator().next();
    }

    @Override
    public Collection<GeneValueObject> getValueObjects(GeneService service) {
        return service.loadValueObjects( service.findByOfficialSymbol( this.value ) );
    }
}
