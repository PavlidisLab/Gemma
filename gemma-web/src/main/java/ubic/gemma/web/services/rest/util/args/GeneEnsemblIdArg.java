package ubic.gemma.web.services.rest.util.args;

import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.model.genome.Gene;

/**
 * Long argument type for Gene API, referencing the Gene Ensembl ID.
 *
 * @author tesarst
 */
public class GeneEnsemblIdArg extends GeneAnyIdArg<String> {

    private static final String ID_NAME = "Ensembl ID";

    /**
     * @param s intentionally primitive type, so the value property can never be null.
     */
    GeneEnsemblIdArg( String s ) {
        this.value = s;
        setNullCause( ID_NAME, "Gene" );
    }

    @Override
    public Gene getPersistentObject( GeneService service ) {
        return check( service.findByEnsemblId( this.value ) );
    }

    @Override
    public String getPropertyName( GeneService service ) {
        return "ensemblId";
    }

    @Override
    String getIdentifierName() {
        return ID_NAME;
    }
}
