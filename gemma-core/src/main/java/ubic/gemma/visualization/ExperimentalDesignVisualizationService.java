/*
 * The Gemma project
 * 
 * Copyright (c) 2012 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.visualization;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.DoubleVectorValueObject;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * @author Paul
 * @version $Id$
 */
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
     * @return Map of EE ids to Map of BioAssays ...
     */
    public abstract Map<Long, LinkedHashMap<BioAssay, LinkedHashMap<ExperimentalFactor, Double>>> sortVectorDataByDesign(
            Collection<DoubleVectorValueObject> dedvs );

    /**
     * Sorts the layouts passed in by factor with factors ordered by their number of values, from fewest values to most.
     * The LinkedHashMap<BioAssay, {value}> and LinkedHashMap<ExperimentalFactor, Double>> portions of each layout are
     * both sorted.
     * 
     * @param layouts
     * @return sorted layouts
     */
    public abstract Map<Long, LinkedHashMap<BioAssay, LinkedHashMap<ExperimentalFactor, Double>>> sortLayoutSamplesByFactor(
            Map<Long, LinkedHashMap<BioAssay, LinkedHashMap<ExperimentalFactor, Double>>> layouts );

    /**
     * removed the cached layouts and cached BioAssayDimensions for this experiment
     * 
     * @param eeId
     */
    public void clearCaches( Long eeId );

    /**
     * removed the cached layouts and cached BioAssayDimensions for this experiment
     * 
     * @param ee
     */
    public void clearCaches( ExpressionExperiment ee );

    /**
     * removed all cached layouts and cached BioAssayDimensions
     */
    public void clearCaches();
}