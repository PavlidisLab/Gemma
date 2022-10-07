package ubic.gemma.web.services.rest.util.args;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.commons.lang3.StringUtils;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;
import ubic.gemma.persistence.util.ObjectFilter;
import ubic.gemma.web.services.rest.util.MalformedArgException;

import java.util.List;

import static ubic.gemma.web.services.rest.util.StringUtils.splitAndTrim;

@ArraySchema(schema = @Schema(implementation = TaxonArg.class))
public class TaxonArrayArg extends AbstractEntityArrayArg<String, Taxon, TaxonService> {
    private static final String ERROR_MSG_DETAIL = "Provide a string that contains at least one "
            + "ID, NCBI ID, scientific name or common name or multiple, separated by (',') character. "
            + "All identifiers must be same type, i.e. do not combine different kinds of IDs and string identifiers.";
    private static final String ERROR_MSG = AbstractArrayArg.ERROR_MSG + " Taxon identifiers";

    private TaxonArrayArg( List<String> values ) {
        super( TaxonArg.class, values );
    }

    @Override
    public ObjectFilter[] getObjectFilters( TaxonService service ) throws MalformedArgException {
        if ( getPropertyName( service ).equals( "commonName" ) ) {
            return new ObjectFilter[] {
                    service.getObjectFilter( "commonName", ObjectFilter.Operator.in, this.getValue() ),
                    service.getObjectFilter( "scientificName", ObjectFilter.Operator.in, this.getValue() ) };
        } else {
            return super.getObjectFilters( service );
        }
    }

    /**
     * Used by RS to parse value of request parameters.
     *
     * @param s the request arrayTaxon argument
     * @return an instance of ArrayTaxonArg representing an array of Taxon identifiers from the input string, or a
     * malformed ArrayTaxonArg that will throw an {@link javax.ws.rs.BadRequestException} when accessing its value, if
     * the input String can not be converted into an array of Taxon identifiers.
     */
    @SuppressWarnings("unused")
    public static TaxonArrayArg valueOf( final String s ) {
        if ( StringUtils.isBlank( s ) ) {
            throw new MalformedArgException( String.format( TaxonArrayArg.ERROR_MSG, s ),
                    new IllegalArgumentException( TaxonArrayArg.ERROR_MSG_DETAIL ) );
        }
        return new TaxonArrayArg( splitAndTrim( s ) );
    }
}
