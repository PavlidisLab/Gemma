package ubic.gemma.web.services.rest.util.args;

import com.google.common.base.Strings;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;
import ubic.gemma.web.services.rest.util.MalformedArgException;
import ubic.gemma.web.services.rest.util.StringUtils;

import java.util.List;

@ArraySchema(schema = @Schema(implementation = TaxonArg.class))
public class TaxonArrayArg extends AbstractEntityArrayArg<String, Taxon, TaxonService> {
    private static final String ERROR_MSG_DETAIL = "Provide a string that contains at least one "
            + "ID, NCBI ID, scientific name or common name or multiple, separated by (',') character. "
            + "All identifiers must be same type, i.e. do not combine different kinds of IDs and string identifiers.";
    private static final String ERROR_MSG = AbstractArrayArg.ERROR_MSG + " Taxon identifiers";

    private TaxonArrayArg( List<String> values ) {
        super( TaxonArg.class, values );
    }

    private TaxonArrayArg( String errorMessage, Exception exception ) {
        super( TaxonArg.class, errorMessage, exception );
    }

    /**
     * The taxon implementation is different from the others, because the taxon checks the property name by attempting
     * to retrieve a taxon with the given identifier (no other entity allows multiple unconstrained free text identifiers).
     * Therefore, if the first identifier does not exist, we can not
     * determine the name of the property. This is why we iterate over the array before we encounter the first identifier
     * that exists. if none of the identifiers exist, null will be returned, which will in turn cause a 400 error.
     *
     * @param service see the parent class
     */
    @Override
    protected String getPropertyName( TaxonService service ) {
        for ( int i = 0; i < this.getValue().size(); i++ ) {
            try {
                String value = this.getValue().get( i );
                AbstractEntityArg<?, Taxon, TaxonService> arg = TaxonArg.valueOf( value );
                return this.checkPropertyNameString( arg, value, service );
            } catch ( MalformedArgException e ) {
                if ( i == this.getValue().size() - 1 ) {
                    throw e;
                }
            }
        }
        return "id";
    }

    /**
     * Used by RS to parse value of request parameters.
     *
     * @param s the request arrayTaxon argument
     * @return an instance of ArrayTaxonArg representing an array of Taxon identifiers from the input string,
     * or a malformed ArrayTaxonArg that will throw an {@link javax.ws.rs.BadRequestException} when accessing its value, if the
     * input String can not be converted into an array of Taxon identifiers.
     */
    @SuppressWarnings("unused")
    public static TaxonArrayArg valueOf( final String s ) {
        if ( Strings.isNullOrEmpty( s ) ) {
            return new TaxonArrayArg( String.format( TaxonArrayArg.ERROR_MSG, s ),
                    new IllegalArgumentException( TaxonArrayArg.ERROR_MSG_DETAIL ) );
        }
        return new TaxonArrayArg( StringUtils.splitAndTrim( s ) );
    }
}
