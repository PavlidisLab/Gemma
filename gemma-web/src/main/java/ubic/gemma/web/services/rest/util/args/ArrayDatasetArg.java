package ubic.gemma.web.services.rest.util.args;

import com.google.common.base.Strings;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.persistence.service.BaseVoEnabledService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.util.ObjectFilter;
import ubic.gemma.web.services.rest.util.GemmaApiException;
import ubic.gemma.web.services.rest.util.WellComposedErrorBody;

import javax.ws.rs.core.Response;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ArrayDatasetArg extends ArrayEntityArg<ExpressionExperiment, ExpressionExperimentValueObject, ExpressionExperimentService> {
    private static final String ERROR_MSG_DETAIL = "Provide a string that contains at least one ID or short name, or multiple, separated by (',') character. All identifiers must be same type, i.e. do not combine IDs and short names.";
    private static final String ERROR_MSG = ArrayArg.ERROR_MSG + " Dataset identifiers";

    private ArrayDatasetArg( List<String> values ) {
        super( values );
    }

    private ArrayDatasetArg( String errorMessage, Exception exception ) {
        super( errorMessage, exception );
    }

    /**
     * Used by RS to parse value of request parameters.
     *
     * @param s the request arrayDataset argument
     * @return an instance of ArrayDatasetArg representing an array of Dataset identifiers from the input string,
     * or a malformed ArrayDatasetArg that will throw an {@link GemmaApiException} when accessing its value, if the
     * input String can not be converted into an array of Dataset identifiers.
     */
    @SuppressWarnings("unused")
    public static ArrayStringArg valueOf( final String s ) {
        if ( Strings.isNullOrEmpty( s ) ) {
            return new ArrayDatasetArg( String.format( ERROR_MSG, s ),
                    new IllegalArgumentException( ERROR_MSG_DETAIL ) );
        }
        return new ArrayDatasetArg( Arrays.asList( splitString( s ) ) );
    }

    @Override
    protected String getObjectDaoAlias() {
        return ObjectFilter.DAO_EE_ALIAS;
    }

    @Override
    protected String getPropertyName( ExpressionExperimentService service ) {
        String value = this.getValue().get( 0 );
        DatasetArg arg = DatasetArg.valueOf( value );
        return checkPropertyNameString( arg, value, service );
    }

}
