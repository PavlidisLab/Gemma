package ubic.gemma.web.services.rest.util.args;

import com.google.common.base.Strings;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;
import ubic.gemma.persistence.util.ObjectFilter;
import ubic.gemma.web.services.rest.util.GemmaApiException;

import java.util.Arrays;
import java.util.List;

public class ArrayTaxonArg extends ArrayEntityArg<Taxon, TaxonService> {
    private static final String ERROR_MSG_DETAIL = "Provide a string that contains at least one "
            + "ID, NCBI ID, scientific name or common name or multiple, separated by (',') character. "
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
    public static ArrayTaxonArg valueOf( final String s ) {
        if ( Strings.isNullOrEmpty( s ) ) {
            return new ArrayTaxonArg( String.format( ArrayTaxonArg.ERROR_MSG, s ),
                    new IllegalArgumentException( ArrayTaxonArg.ERROR_MSG_DETAIL ) );
        }
        return new ArrayTaxonArg( Arrays.asList( ArrayArg.splitString( s ) ) );
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
    protected void setPropertyNameAndType( TaxonService service ) {
        for ( int i = 0; i < this.getValue().size(); i++ ) {
            try {
                String value = this.getValue().get( i );
                MutableArg<?, Taxon, TaxonService> arg = TaxonArg.valueOf( value );
                this.argValueName = this.checkPropertyNameString( arg, value, service );
                this.argValueClass = arg.value.getClass();
            } catch ( GemmaApiException e ) {
                if ( i == this.getValue().size() - 1 ) {
                    throw e;
                }
            }
        }
    }

    @Override
    protected String getObjectDaoAlias() {
        return ObjectFilter.DAO_TAXON_ALIAS;
    }

}
