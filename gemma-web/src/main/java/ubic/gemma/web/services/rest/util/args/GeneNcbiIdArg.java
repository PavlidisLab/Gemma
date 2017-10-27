package ubic.gemma.web.services.rest.util.args;

import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.model.genome.Gene;

/**
 * Long argument type for Gene API, referencing the Gene NCBI ID.
 *
 * @author tesarst
 */
public class GeneNcbiIdArg extends GeneAnyIdArg<Integer> {

    private static final String ID_NAME = "NCBI ID";

    /**
     * @param l intentionally primitive type, so the value property can never be null.
     */
    GeneNcbiIdArg( int l ) {
        this.value = l;
        setNullCause( ID_NAME, "Gene" );
    }

    @Override
    public Gene getPersistentObject( GeneService service ) {
        return check( service.findByNCBIId( this.value ) );
    }

    @Override
    public String getPropertyName( GeneService service ) {
        return "ncbiId";
    }

    @Override
    String getIdentifierName() {
        return ID_NAME;
    }
}
