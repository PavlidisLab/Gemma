package ubic.gemma.web.services.rest.util.args;

import com.google.common.base.Strings;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonValueObject;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;
import ubic.gemma.persistence.util.ObjectFilter;
import ubic.gemma.web.services.rest.util.GemmaApiException;

import java.util.Arrays;
import java.util.List;

public class ArrayTaxonArg extends ArrayEntityArg<Taxon, TaxonValueObject, TaxonService> {
    private static final String ERROR_MSG_DETAIL = "Provide a string that contains at least one "
            + "ID, NCBI ID, scientific name, common name or abbreviation, or multiple, separated by (',') character. "
            + "All identifiers must be same type, i.e. do not combine different kinds of IDs and string identifiers.";
    private static final String ERROR_MSG = ArrayArg.ERROR_MSG + " Taxon identifiers";

    private ArrayTaxonArg( List<String> values ) {
        super( values, TaxonArg.class );
    }

    private ArrayTaxonArg( String errorMessage, Exception exception ) {
        super( errorMessage, exception );
    }

    /**
     * Used by RS to parse value of request parameters.
     *
     * @param s the request arrayTaxon argument
     * @return an instance of ArrayTaxonArg representing an array of Taxon identifiers from the input string,
     * or a malformed ArrayTaxonArg that will throw an {@link GemmaApiException} when accessing its value, if the
     * input String can not be converted into an array of Taxon identifiers.
     */
    @SuppressWarnings("unused")
    public static ArrayStringArg valueOf( final String s ) {
        if ( Strings.isNullOrEmpty( s ) ) {
            return new ArrayTaxonArg( String.format( ERROR_MSG, s ), new IllegalArgumentException( ERROR_MSG_DETAIL ) );
        }
        return new ArrayTaxonArg( Arrays.asList( splitString( s ) ) );
    }

    @Override
    protected String getObjectDaoAlias() {
        return ObjectFilter.DAO_TAXON_ALIAS;
    }

    @Override
    protected String getPropertyName( TaxonService service ) {
        String propertyName;
        for ( int i = 0; i < this.getValue().size(); i++ ) {
            try {
                String value = this.getValue().get( i );
                TaxonArg arg = TaxonArg.valueOf( value );
                propertyName = checkPropertyNameString( arg, value, service );
                return propertyName;
            } catch ( GemmaApiException e ) {
                if ( i == this.getValue().size() - 1 ) {
                    throw e;
                }
            }
        }
        // should never happen as the catch will rethrow at the end of the loop
        return null;
    }

}
