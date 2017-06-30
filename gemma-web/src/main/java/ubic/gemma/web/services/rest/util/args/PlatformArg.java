package ubic.gemma.web.services.rest.util.args;

import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.bioAssay.BioAssayValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.bioAssay.BioAssayService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.beans.Expression;
import java.util.Collection;

/**
 * Created by tesarst on 24/05/17.
 * Mutable argument type base class for dataset (ExpressionExperiment) API
 */
public abstract class PlatformArg<T>
        extends MutableArg<T, ArrayDesign, ArrayDesignService, ArrayDesignValueObject> {

    /**
     * Used by RS to parse value of request parameters.
     *
     * @param s the request dataset argument
     * @return instance of appropriate implementation of DatasetArg based on the actual Type the argument represents.
     */
    @SuppressWarnings("unused")
    public static PlatformArg valueOf( final String s ) {
        try {
            return new PlatformIdArg( Long.parseLong( s.trim() ) );
        } catch ( NumberFormatException e ) {
            return new PlatformStringArg( s );
        }
    }

    /**
     * Retrieves the Datasets of the Platform that this argument represents.
     *
     * @param service   service that will be used to retrieve the persistent AD object.
     * @param eeService service to use to retrieve the EEs.
     * @return a collection of Datasets that the platform represented by this argument contains.
     */
    public Collection<ExpressionExperimentValueObject> getExperiments( ArrayDesignService service,
            ExpressionExperimentService eeService ) {
        ArrayDesign ad = this.getPersistentObject( service );
        return ad == null ? null : eeService.loadValueObjects( service.getExpressionExperiments( ad ) );
    }
}
