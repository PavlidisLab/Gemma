package ubic.gemma.rest.util.args;

import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.media.Schema;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.service.genome.gene.GeneService;

import javax.annotation.Nonnull;

/**
 * Long argument type for Gene API, referencing the Gene NCBI ID.
 *
 * @author tesarst
 */
@Schema(type = "string", description = "An NCBI gene identifier.",
        externalDocs = @ExternalDocumentation(url = "https://www.ncbi.nlm.nih.gov/gene"))
public class GeneNcbiIdArg extends GeneArg<Integer> {

    /**
     * @param l intentionally primitive type, so the value property can never be null.
     */
    GeneNcbiIdArg( int l ) {
        super( "ncbiGeneId", Integer.class, l );
    }

    @Nonnull
    @Override
    Gene getEntity( GeneService service ) {
        return service.findByNCBIId( this.getValue() );
    }
}
