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
        this.value = l;
        this.nullCause = String.format( ERROR_FORMAT_ENTITY_NOT_FOUND, "ID", "Taxon" );
    }

    @Override
    public Taxon getPersistentObject( TaxonService service ) {
        return check( service.findByNcbiId( this.value ) );
    }

    @Override
    public String getPropertyName( TaxonService service ) {
        return "id";
    }
}
