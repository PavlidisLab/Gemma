package ubic.gemma.web.services.rest.util.args;

import io.swagger.v3.oas.annotations.media.Schema;
import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.model.genome.Gene;

/**
 * Long argument type for Gene API, referencing the Gene Ensembl ID.
 *
 * @author tesarst
 */
@Schema(implementation = String.class)
public class GeneEnsemblIdArg extends GeneAnyIdArg<String> {

    private static final String ID_NAME = "Ensembl ID";

    /**
     * @param s intentionally primitive type, so the value property can never be null.
     */
    GeneEnsemblIdArg( String s ) {
        super( s );
    }

    @Override
    public Gene getEntity( GeneService service ) {
        return checkEntity( service.findByEnsemblId( this.getValue() ) );
    }

    @Override
    public String getPropertyName() {
        return "ensemblId";
    }

    @Override
    String getIdentifierName() {
        return ID_NAME;
    }
}
