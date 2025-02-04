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
package ubic.gemma.core.visualization;

import ubic.gemma.model.expression.bioAssay.BioAssayValueObject;
import ubic.gemma.model.expression.bioAssayData.DoubleVectorValueObject;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Paul
 */
public interface ExperimentalDesignVisualizationService {

    /**
     * Put data vectors in the order you'd want to display the experimental design. This causes the "isReorganized" flag
     * of the dedVs to be set to true.
     *
     * @param dedVs         dedVs, already sliced for the subset of samples needed for display (if necessary); will be
     *                      modified
     * @param primaryFactor if non-null this factor will be used to order the data, otherwise the first factor will be
     *                      chosen using built-in heuristics. Set this to ensure that the data is ordered by the factor
     *                      you want as in the case of showing DE genes
     * @return Map of EE ids to "layouts", which are Maps of BioAssays to map of experimental factors to doubles.
     */
    Map<Long, LinkedHashMap<BioAssayValueObject, LinkedHashMap<ExperimentalFactor, Double>>> sortVectorDataByDesign(
            Collection<DoubleVectorValueObject> dedVs, ExperimentalFactor primaryFactor );

}