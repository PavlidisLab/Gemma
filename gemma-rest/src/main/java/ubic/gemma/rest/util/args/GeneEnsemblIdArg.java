package ubic.gemma.rest.util.args;

import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.media.Schema;
import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.model.genome.Gene;

/**
 * Long argument type for Gene API, referencing the Gene Ensembl ID.
 *
 * @author tesarst
 */
@Schema(type = "string", description = "An Ensembl gene identifier which typically starts with 'ENSG'.",
        externalDocs = @ExternalDocumentation(url = "https://www.ensembl.org/"))
public class GeneEnsemblIdArg extends GeneArg<String> {

    /**
     * @param s intentionally primitive type, so the value property can never be null.
     */
    GeneEnsemblIdArg( String s ) {
        super( "ensemblId", String.class, s );
    }

    @Override
    Gene getEntity( GeneService service ) {
        return service.findByEnsemblId( this.getValue() );
    }
}
