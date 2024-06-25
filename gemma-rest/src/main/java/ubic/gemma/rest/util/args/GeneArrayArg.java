package ubic.gemma.rest.util.args;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.service.genome.gene.GeneService;

import java.util.List;

@ArraySchema(arraySchema = @Schema(description = GeneArrayArg.ARRAY_SCHEMA_DESCRIPTION), schema = @Schema(implementation = GeneArg.class), minItems = 1)
public class GeneArrayArg extends AbstractEntityArrayArg<Gene, GeneService> {

    public static final String OF_WHAT = "NCBI IDs, Ensembl IDs or gene symbols";
    public static final String ARRAY_SCHEMA_DESCRIPTION = ARRAY_SCHEMA_DESCRIPTION_PREFIX + OF_WHAT + ". " + ARRAY_SCHEMA_COMPRESSION_DESCRIPTION;

    private GeneArrayArg( List<String> values ) {
        super( GeneArg.class, values );
    }

    public static GeneArrayArg valueOf( final String s ) {
        return valueOf( s, OF_WHAT, GeneArrayArg::new, true );
    }
}
