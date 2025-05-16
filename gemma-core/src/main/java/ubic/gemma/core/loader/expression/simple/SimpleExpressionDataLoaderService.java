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
package ubic.gemma.core.loader.expression.simple;

import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.gemma.core.loader.expression.simple.model.SimpleExpressionExperimentMetadata;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import javax.annotation.Nullable;

/**
 * Load experiment from a flat file. See also {@link ubic.gemma.core.loader.expression.DataUpdater} for related
 * operations on experiments already in the system.
 *
 * @author paul
 */
public interface SimpleExpressionDataLoaderService {

    /**
     * Parses, converts (into Gemma objects), and loads data into the database.
     * <p>
     * If data is provided and preferred, post-processing will be triggered.
     * @param metaData meta data
     * @param data     tab-delimited file with row names corresponding to {@link CompositeSequence} names and column
     *                 names corresponding to {@link BioAssay} names. Note that if you provide samples in the metadata,
     *                 all the samples referenced in the data file must be declared.
     * @return new experiment
     */
    ExpressionExperiment create( SimpleExpressionExperimentMetadata metaData, @Nullable DoubleMatrix<String, String> data );

    /**
     * Convert simple experiment metadata and data into Gemma objects.
     */
    ExpressionExperiment convert( SimpleExpressionExperimentMetadata metaData, @Nullable DoubleMatrix<String, String> data );
}