package ubic.gemma.visualization;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.DoubleVectorValueObject;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

public interface ExperimentalDesignVisualizationService {

    /**
     * For an experiment, spit out
     * 
     * @param e, experiment; should be lightly thawed.
     * @return Map of bioassays to factors to values for plotting. If there are no Factors, a dummy value is returned.
     */
    public abstract LinkedHashMap<BioAssay, LinkedHashMap<ExperimentalFactor, Double>> getExperimentalDesignLayout(
            ExpressionExperiment e );

    /**
     * @param experiment assumed thawed
     * @param bd assumed thawed
     * @return the map's double value is either the measurement associated with the factor or the id of the factor value
     *         object
     */
    public abstract LinkedHashMap<BioAssay, LinkedHashMap<ExperimentalFactor, Double>> getExperimentalDesignLayout(
            ExpressionExperiment experiment, BioAssayDimension bd );

    /**
     * Test method for now, shows how this can be used.
     * 
     * @param e
     */
    public abstract void plotExperimentalDesign( ExpressionExperiment e );

    /**
     * Put data vectors in the order you'd want to display the experimental design.
     * 
     * @param dedvs
     */
    public abstract Map<ExpressionExperiment, LinkedHashMap<BioAssay, LinkedHashMap<ExperimentalFactor, Double>>> sortVectorDataByDesign(
            Collection<DoubleVectorValueObject> dedvs );

    /**
     * Sorts the layouts passed in by factor with factors ordered by their number of values, from fewest values to most.
     * The LinkedHashMap<BioAssay, {value}> and LinkedHashMap<ExperimentalFactor, Double>> portions of each layout are
     * both sorted.
     * 
     * @param layouts
     * @return sorted layouts
     */
    public abstract Map<ExpressionExperiment, LinkedHashMap<BioAssay, LinkedHashMap<ExperimentalFactor, Double>>> sortLayoutSamplesByFactor(
            Map<ExpressionExperiment, LinkedHashMap<BioAssay, LinkedHashMap<ExperimentalFactor, Double>>> layouts );

}