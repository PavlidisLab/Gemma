package ubic.gemma.web.services.rest.util.args;

import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.model.genome.Gene;

/**
 * Created by tesarst on 16/05/17.
 * Long argument type for Gene API, referencing the Gene Ensembl ID.
 */
public class GeneEnsemblIdArg extends GeneAnyIdArg<String> {

    private static final String ID_NAME = "Ensembl ID";

    /**
     * @param s intentionally primitive type, so the value property can never be null.
     */
    GeneEnsemblIdArg( String s ) {
        this.value = s;
        this.nullCause = getDefaultError();
    }

    @Override
    public Gene getPersistentObject( GeneService service ) {
        return check(service.findByEnsemblId( this.value ));
    }

    @Override
    String getIdentifierName() {
        return ID_NAME;
    }
}
