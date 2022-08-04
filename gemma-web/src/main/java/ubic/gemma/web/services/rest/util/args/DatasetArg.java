package ubic.gemma.web.services.rest.util.args;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.StaleStateException;
import ubic.gemma.core.analysis.preprocess.OutlierDetails;
import ubic.gemma.core.analysis.preprocess.OutlierDetectionService;
import ubic.gemma.core.analysis.preprocess.filter.FilteringException;
import ubic.gemma.core.analysis.preprocess.filter.NoRowsLeftAfterFilteringException;
import ubic.gemma.model.common.description.AnnotationValueObject;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.bioAssay.BioAssayService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.web.services.rest.util.MalformedArgException;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Mutable argument type base class for dataset (ExpressionExperiment) API.
 *
 * @author tesarst
 */
@Schema(oneOf = { DatasetIdArg.class, DatasetStringArg.class })
@CommonsLog
public abstract class DatasetArg<T>
        extends AbstractEntityArg<T, ExpressionExperiment, ExpressionExperimentService> {

    protected DatasetArg( T value ) {
        super( ExpressionExperiment.class, value );
    }

    /**
     * Used by RS to parse value of request parameters.
     *
     * @param s the request dataset argument
     * @return instance of appropriate implementation of DatasetArg based on the actual Type the argument represents.
     */
    @SuppressWarnings("unused")
    public static DatasetArg<?> valueOf( final String s ) throws MalformedArgException {
        if ( StringUtils.isBlank( s ) ) {
            throw new MalformedArgException( "Dataset identifier cannot be null or empty." );
        }
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
    public List<ArrayDesignValueObject> getPlatforms( ExpressionExperimentService service,
            ArrayDesignService adService ) {
        ExpressionExperiment ee = this.getEntity( service );
        return adService.loadValueObjectsForEE( ee.getId() );
    }

    /**
     * @param service                 service that will be used to retrieve the persistent EE object.
     * @param baService               service that will be used to convert the samples (BioAssays) to VOs.
     * @param outlierDetectionService service that will be used to detect which samples are outliers and fill their
     *                                corresponding predictedOutlier attribute.
     * @return a collection of BioAssays that represent the experiments samples.
     */
    public List<BioAssayValueObject> getSamples( ExpressionExperimentService service,
            BioAssayService baService, OutlierDetectionService outlierDetectionService ) {
        ExpressionExperiment ee = service.thawBioAssays( this.getEntity( service ) );
        List<BioAssayValueObject> bioAssayValueObjects = baService.loadValueObjects( ee.getBioAssays(), true );
        try {
            Set<Long> predictedOutlierBioAssayIds = outlierDetectionService.identifyOutliersByMedianCorrelation( ee ).stream()
                    .map( OutlierDetails::getBioAssay )
                    .map( BioAssay::getId )
                    .collect( Collectors.toSet() );
            for ( BioAssayValueObject vo : bioAssayValueObjects ) {
                vo.setPredictedOutlier( predictedOutlierBioAssayIds.contains( vo.getId() ) );
            }
        } catch ( NoRowsLeftAfterFilteringException e ) {
            // there are no rows left in the data matrix, thus no outliers ;o
        } catch ( FilteringException e ) {
            throw new RuntimeException( e );
        } catch ( StaleStateException e ) {
            log.warn( String.format( "Failed to determine outliers for %s. This is due to a high contention for the public-facing API endpoint. See https://github.com/PavlidisLab/Gemma/issues/242 for more details.", ee ), e );
        }
        return bioAssayValueObjects;
    }

    /**
     * @param service service that will be used to retrieve the persistent EE object.
     * @return a collection of Annotations value objects that represent the experiments annotations.
     */
    public Set<AnnotationValueObject> getAnnotations( ExpressionExperimentService service ) {
        ExpressionExperiment ee = this.getEntity( service );
        return service.getAnnotations( ee.getId() );
    }
}
