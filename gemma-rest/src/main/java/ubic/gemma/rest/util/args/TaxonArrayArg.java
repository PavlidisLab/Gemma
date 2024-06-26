package ubic.gemma.rest.util.args;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import java.util.List;

@ArraySchema(arraySchema = @Schema(description = TaxonArrayArg.ARRAY_SCHEMA_DESCRIPTION), schema = @Schema(implementation = TaxonArg.class), minItems = 1)
public class TaxonArrayArg extends AbstractEntityArrayArg<Taxon, TaxonService> {

    public static final String OF_WHAT = "taxon IDs, NCBI IDs, common names or scientific names";
    public static final String ARRAY_SCHEMA_DESCRIPTION = ARRAY_SCHEMA_DESCRIPTION_PREFIX + OF_WHAT + ".";

    private TaxonArrayArg( List<String> values ) {
        super( TaxonArg.class, values );
    }

    public static TaxonArrayArg valueOf( final String s ) {
        return valueOf( s, OF_WHAT, TaxonArrayArg::new, false );
    }
}
