package ubic.gemma.web.services.rest.util.args;

import ubic.gemma.model.common.description.AnnotationValueObject;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.bioAssay.BioAssayValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.bioAssay.BioAssayService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.util.Collection;

/**
 * Created by tesarst on 24/05/17.
 * Mutable argument type base class for dataset (ExpressionExperiment) API
 */
public abstract class DatasetArg<T>
        extends MutableArg<T, ExpressionExperiment, ExpressionExperimentService, ExpressionExperimentValueObject> {

    /**
     * Used by RS to parse value of request parameters.
     *
     * @param s the request dataset argument
     * @return instance of appropriate implementation of DatasetArg based on the actual Type the argument represents.
     */
    @SuppressWarnings("unused")
    public static DatasetArg valueOf( final String s ) {
        try {
            return new DatasetIdArg( Long.parseLong( s.trim() ) );
        } catch ( NumberFormatException e ) {
            return new DatasetStringArg( s );
        }
    }

    /**
     * Retrieves the Platforms of the Dataset that this argument represents.
     *
     * @param service   service that will be used to retrieve the persistent EE object.
     * @param adService service to use to retrieve the ADs.
     * @return a collection of Platforms that the dataset represented by this argument is in.
     */
    public Collection<ArrayDesignValueObject> getPlatforms( ExpressionExperimentService service,
            ArrayDesignService adService ) {
        ExpressionExperiment ee = this.getPersistentObject( service );
        return ee == null ? null : adService.loadValueObjectsForEE( ee.getId() );
    }

    /**
     * @param service   service that will be used to retrieve the persistent EE object.
     * @param baService service that will be used to convert the samples (BioAssays) to VOs.
     * @return a collection of BioAssays that represent the experiments samples.
     */
    public Collection<BioAssayValueObject> getSamples( ExpressionExperimentService service,
            BioAssayService baService ) {
        ExpressionExperiment ee = service.thawBioAssays( this.getPersistentObject( service ) );
        return ee == null ? null : baService.loadValueObjects( ee.getBioAssays() );
    }

    /**
     * @param service service that will be used to retrieve the persistent EE object.
     * @return a collection of Annotations value objects that represent the experiments annotations.
     */
    public Collection<AnnotationValueObject> getAnnotations( ExpressionExperimentService service ) {
        ExpressionExperiment ee = this.getPersistentObject( service );
        return ee == null ? null : service.getAnnotations( ee.getId() );
    }

}
