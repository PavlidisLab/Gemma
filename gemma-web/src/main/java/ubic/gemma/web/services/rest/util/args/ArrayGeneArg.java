package ubic.gemma.web.services.rest.util.args;

import com.google.common.base.Strings;
import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.persistence.util.ObjectFilter;
import ubic.gemma.web.services.rest.util.GemmaApiException;

import java.util.Arrays;
import java.util.List;

public class ArrayGeneArg extends ArrayEntityArg<Gene, GeneValueObject, GeneService> {
    private static final String ERROR_MSG_DETAIL = "Provide a string that contains at least one Ncbi ID, Ensembl ID or official symbol, or multiple, separated by (',') character. All identifiers must be same type, i.e. do not combine Ensembl and Ncbi IDs.";
    private static final String ERROR_MSG = ArrayArg.ERROR_MSG + " Gene identifiers";

    private ArrayGeneArg( List<String> values ) {
        super( values, GeneArg.class );
    }

    private ArrayGeneArg( String errorMessage, Exception exception ) {
        super( errorMessage, exception );
    }

    /**
     * Used by RS to parse value of request parameters.
     *
     * @param s the request arrayGene argument
     * @return an instance of ArrayGeneArg representing an array of Gene identifiers from the input string,
     * or a malformed ArrayGeneArg that will throw an {@link GemmaApiException} when accessing its value, if the
     * input String can not be converted into an array of Gene identifiers.
     */
    @SuppressWarnings("unused")
    public static ArrayGeneArg valueOf( final String s ) {
        if ( Strings.isNullOrEmpty( s ) ) {
            return new ArrayGeneArg( String.format( ArrayGeneArg.ERROR_MSG, s ),
                    new IllegalArgumentException( ArrayGeneArg.ERROR_MSG_DETAIL ) );
        }
        return new ArrayGeneArg( Arrays.asList( ArrayEntityArg.splitString( s ) ) );
    }

    @Override
    protected void setPropertyNameAndType( GeneService service ) {
        String value = this.getValue().get( 0 );
        MutableArg<?, Gene, GeneValueObject, GeneService> arg = GeneArg.valueOf( value );
        this.argValueName = this.checkPropertyNameString( arg, value, service );
        this.argValueClass = arg.value.getClass();
    }

    @Override
    protected String getObjectDaoAlias() {
        return ObjectFilter.DAO_GENE_ALIAS;
    }

}
