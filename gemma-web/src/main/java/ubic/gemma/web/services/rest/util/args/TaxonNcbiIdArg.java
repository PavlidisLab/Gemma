package ubic.gemma.web.services.rest.util.args;

import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

/**
 * Long argument type for taxon API, referencing the Taxon ID.
 *
 * @author tesarst
 */
public class TaxonNcbiIdArg extends TaxonArg<Long> {

    /**
     * @param l intentionally primitive type, so the value property can never be null.
     */
    TaxonNcbiIdArg( long l ) {
        super( l );
    }

    @Override
    public Taxon getEntity( TaxonService service ) {
        return checkEntity( service.findByNcbiId( this.getValue() ) );
    }

    @Override
    public String getPropertyName() {
        return "id";
    }
}
