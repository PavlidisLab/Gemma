package ubic.gemma.web.services.rest.util.args;

import com.google.common.base.Strings;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.util.ObjectFilter;
import ubic.gemma.web.services.rest.util.GemmaApiException;
import ubic.gemma.web.services.rest.util.StringUtils;

import java.util.List;

public class DatasetArrayArg
        extends AbstractEntityArrayArg<ExpressionExperiment, ExpressionExperimentService> {
    private static final String ERROR_MSG_DETAIL = "Provide a string that contains at least one ID or short name, or multiple, separated by (',') character. All identifiers must be same type, i.e. do not combine IDs and short names.";
    private static final String ERROR_MSG = ArrayArg.ERROR_MSG + " Dataset identifiers";

    private DatasetArrayArg( List<String> values ) {
        super( values );
    }

    @Override
    protected Class<? extends AbstractEntityArg> getEntityArgClass() {
        return DatasetArg.class;
    }

    private DatasetArrayArg( String errorMessage, Exception exception ) {
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
    public static DatasetArrayArg valueOf( final String s ) {
        if ( Strings.isNullOrEmpty( s ) ) {
            return new DatasetArrayArg( String.format( DatasetArrayArg.ERROR_MSG, s ),
                    new IllegalArgumentException( DatasetArrayArg.ERROR_MSG_DETAIL ) );
        }
        return new DatasetArrayArg( StringUtils.splitAndTrim( s ) );
    }

    @Override
    protected String getObjectDaoAlias() {
        return ObjectFilter.DAO_EE_ALIAS;
    }

}
