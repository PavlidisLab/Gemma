package ubic.gemma.web.services.rest.util.args;

import ubic.gemma.persistence.service.genome.taxon.TaxonService;
import ubic.gemma.model.genome.Taxon;

/**
 * Created by tesarst on 16/05/17.
 * Long argument type for taxon API, referencing the Taxon ID.
 */
public class TaxonIdArg extends TaxonArg<Long> {

    /**
     * @param l intentionally primitive type, so the value property can never be null.
     */
    TaxonIdArg( long l ) {
        this.value = l;
    }

    @Override
    protected Taxon getTaxon( TaxonService service ) {
        return service.load( this.value );
    }
}
