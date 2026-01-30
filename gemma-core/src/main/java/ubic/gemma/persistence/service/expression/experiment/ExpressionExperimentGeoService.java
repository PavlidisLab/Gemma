package ubic.gemma.persistence.service.expression.experiment;

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.core.loader.expression.geo.model.GeoSeries;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import javax.annotation.Nullable;

/**
 * This service provides access to GEO metadata for any given Gemma dataset.
 *
 * @author poirigui
 */
public interface ExpressionExperimentGeoService {

    /**
     * Obtain the GEO series metadata.
     */
    @Nullable
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    GeoSeries getGeoSeries( ExpressionExperiment ee );
}
