package ubic.gemma.rest.util.args;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.commons.lang3.StringUtils;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.service.genome.gene.GeneService;
import ubic.gemma.rest.util.MalformedArgException;

import java.util.List;

@ArraySchema(schema = @Schema(implementation = GeneArg.class))
public class GeneArrayArg extends AbstractEntityArrayArg<Gene, GeneService> {
    private static final String ERROR_MSG_DETAIL = "Provide a string that contains at least one Ncbi ID, Ensembl ID or official symbol, or multiple, separated by (',') character. All identifiers must be same type, i.e. do not combine Ensembl and Ncbi IDs.";
    private static final String ERROR_MSG = AbstractArrayArg.ERROR_MSG + " Gene identifiers";

    private GeneArrayArg( List<String> values ) {
        super( GeneArg.class, values );
    }

    /**
     * Used by RS to parse value of request parameters.
     *
     * @param s the request arrayGene argument
     * @return an instance of ArrayGeneArg representing an array of Gene identifiers from the input string, or a
     * malformed ArrayGeneArg that will throw an {@link javax.ws.rs.BadRequestException} when accessing its value, if
     * the input String can not be converted into an array of Gene identifiers.
     */
    @SuppressWarnings("unused")
    public static GeneArrayArg valueOf( final String s ) {
        if ( StringUtils.isBlank( s ) ) {
            throw new MalformedArgException( String.format( GeneArrayArg.ERROR_MSG, s ),
                    new IllegalArgumentException( GeneArrayArg.ERROR_MSG_DETAIL ) );
        }
        return new GeneArrayArg( splitAndTrim( s ) );
    }

}
