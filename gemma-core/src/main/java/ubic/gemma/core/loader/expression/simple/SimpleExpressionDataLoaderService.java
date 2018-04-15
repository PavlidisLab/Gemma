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
import ubic.gemma.core.loader.expression.simple.model.SimpleExpressionExperimentMetaData;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

/**
 * Load experiment from a flat file. See also {@link ubic.gemma.core.loader.expression.DataUpdater} for related
 * operations on experiments already in the system.
 *
 * @author paul
 */
public interface SimpleExpressionDataLoaderService {

    ExpressionExperiment convert( SimpleExpressionExperimentMetaData metaData, DoubleMatrix<String, String> matrix );

    @SuppressWarnings("unused")
        // Possible external use
    DoubleMatrix<String, String> getSubMatrixForArrayDesign( DoubleMatrix<String, String> matrix,
            Collection<Object> usedDesignElements, ArrayDesign design );

    /**
     * Parses, converts (into Gemma objects), and loads data into the database.
     *
     * @param metaData meta data
     * @param data     tab-delimited file with row names corresponding to CompositeSequence names and column names
     *                 corresponding to BioAssay names.
     * @return new experiment
     * @throws IOException when IO problems occur.
     */
    ExpressionExperiment create( SimpleExpressionExperimentMetaData metaData, InputStream data ) throws IOException;

    DoubleMatrix<String, String> parse( InputStream data ) throws IOException;

}