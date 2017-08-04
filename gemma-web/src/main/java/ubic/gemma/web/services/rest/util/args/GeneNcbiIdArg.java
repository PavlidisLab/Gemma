package ubic.gemma.web.services.rest.util.args;

import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneValueObject;

import java.util.Collection;
import java.util.Collections;

/**
 * Created by tesarst on 16/05/17.
 * Long argument type for Gene API, referencing the Gene ID.
 */
public class GeneNcbiIdArg extends GeneArg<Integer> {

    /**
     * @param l intentionally primitive type, so the value property can never be null.
     */
    GeneNcbiIdArg( int l ) {
        this.value = l;
        this.nullCause = "The identifier was recognised to be an NCBI ID, but no Gene with this NCBI ID exists.";
    }

    @Override
    public Gene getPersistentObject( GeneService service ) {
        return service.findByNCBIId( this.value );
    }

    @Override
    public Collection<GeneValueObject> getValueObjects( GeneService service ) {
        return Collections.singleton( service.loadValueObject( this.getPersistentObject( service ) ) );
    }
}
