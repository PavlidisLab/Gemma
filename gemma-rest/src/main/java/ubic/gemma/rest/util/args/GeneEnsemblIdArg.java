package ubic.gemma.rest.util.args;

import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.media.Schema;
import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.model.genome.Gene;

import javax.annotation.Nonnull;

/**
 * Long argument type for Gene API, referencing the Gene Ensembl ID.
 *
 * @author tesarst
 */
@Schema(type = "string", description = "An Ensembl gene identifier which typically starts with 'ENSG'.",
        externalDocs = @ExternalDocumentation(url = "https://www.ensembl.org/"))
public class GeneEnsemblIdArg extends GeneAnyIdArg<String> {

    /**
     * @param s intentionally primitive type, so the value property can never be null.
     */
    GeneEnsemblIdArg( String s ) {
        super( s );
    }

    @Nonnull
    @Override
    public Gene getEntity( GeneService service ) {
        return checkEntity( service, service.findByEnsemblId( this.getValue() ) );
    }

    @Override
    public String getPropertyName( GeneService service ) {
        return "ensemblId";
    }

}
