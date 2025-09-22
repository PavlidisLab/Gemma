package ubic.gemma.persistence.service.expression.experiment;

import ubic.gemma.core.loader.expression.geo.model.GeoSeries;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import javax.annotation.Nullable;

/**
 * This service provides access to GEO metadata for any given Gemma dataset.
 * @author poirigui
 */
public interface ExpressionExperimentGeoService {

    /**
     * Obtain the GEO series metadata.
     */
    @Nullable
    GeoSeries getGeoSeries( ExpressionExperiment ee );
}
