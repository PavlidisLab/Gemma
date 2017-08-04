package ubic.gemma.web.services.rest.util.args;

import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;

/**
 * Created by tesarst on 24/05/17.
 * String argument type for platform API, referencing the Platform short name. Can also be null.
 */
public class PlatformStringArg extends PlatformArg<String> {

    PlatformStringArg( String s ) {
        this.value = s;
        this.nullCause = "The identifier was recognised to be a short name, but platform with this short name does not exist or is accessible.";
    }

    /**
     * Tries to retrieve a dataset object by its short name.
     *
     * @param service the ExpressionExperimentService that handles the search.
     * @return an ExpressionExperiment object, if found, or null, if the original argument value was null, or if the search did not find
     * any dataset that would match the string.
     */
    @Override
    public ArrayDesign getPersistentObject( ArrayDesignService service ) {
        return this.value == null ? null : service.findByShortName( this.value );
    }

}
