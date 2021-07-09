package ubic.gemma.web.services.rest.util.args;

import com.google.common.base.Strings;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.util.ObjectFilter;
import ubic.gemma.web.services.rest.util.GemmaApiException;
import ubic.gemma.web.services.rest.util.StringUtils;

import java.util.List;

public class PlatformArrayArg extends AbstractEntityArrayArg<ArrayDesign, ArrayDesignService> {
    private static final String ERROR_MSG_DETAIL = "Provide a string that contains at least one ID or short name, or multiple, separated by (',') character. All identifiers must be same type, i.e. do not combine IDs and short names.";
    private static final String ERROR_MSG = ArrayArg.ERROR_MSG + " Platform identifiers";

    private PlatformArrayArg( List<String> values ) {
        super( values );
    }

    @Override
    protected Class<? extends AbstractEntityArg> getEntityArgClass() {
        return PlatformArg.class;
    }

    private PlatformArrayArg( String errorMessage, Exception exception ) {
        super( errorMessage, exception );
    }

    /**
     * Used by RS to parse value of request parameters.
     *
     * @param s the request arrayPlatform argument
     * @return an instance of ArrayPlatformArg representing an array of Platform identifiers from the input string,
     * or a malformed ArrayPlatformArg that will throw an {@link GemmaApiException} when accessing its value, if the
     * input String can not be converted into an array of Platform identifiers.
     */
    @SuppressWarnings("unused")
    public static PlatformArrayArg valueOf( final String s ) {
        if ( Strings.isNullOrEmpty( s ) ) {
            return new PlatformArrayArg( String.format( PlatformArrayArg.ERROR_MSG, s ),
                    new IllegalArgumentException( PlatformArrayArg.ERROR_MSG_DETAIL ) );
        }
        return new PlatformArrayArg( StringUtils.splitAndTrim( s ) );
    }

    @Override
    protected String getObjectDaoAlias() {
        return ObjectFilter.DAO_AD_ALIAS;
    }

}
