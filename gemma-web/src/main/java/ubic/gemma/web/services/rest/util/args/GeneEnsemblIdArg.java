package ubic.gemma.web.services.rest.util.args;

import io.swagger.v3.oas.annotations.media.Schema;
import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.model.genome.Gene;

/**
 * Long argument type for Gene API, referencing the Gene Ensembl ID.
 *
 * @author tesarst
 */
@Schema(type = "string")
public class GeneEnsemblIdArg extends GeneAnyIdArg<String> {

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

}
