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
package ubic.gemma.loader.expression.simple;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.gemma.loader.expression.simple.model.SimpleExpressionExperimentMetaData;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Load experiment from a flat file.
 * 
 * @author paul
 * @version $Id$
 */
public interface SimpleExpressionDataLoaderService {

    /**
     * @param metaData
     * @param matrix
     * @return ExpressionExperiment
     */
    public abstract ExpressionExperiment convert( SimpleExpressionExperimentMetaData metaData,
            DoubleMatrix<String, String> matrix );

    /**
     * @param matrix
     * @param usedDesignElements
     * @param design
     * @return
     */
    public abstract DoubleMatrix<String, String> getSubMatrixForArrayDesign( DoubleMatrix<String, String> matrix,
            Collection<Object> usedDesignElements, ArrayDesign design );

    /**
     * Parses, converts (into Gemma objects), and loads data into the database.
     * 
     * @param metaData
     * @param data tab-delimited file with row names corresponding to CompositeSequence names and column names
     *        corresponding to BioAssay names.
     * @return
     * @throws IOException
     */
    public abstract ExpressionExperiment create( SimpleExpressionExperimentMetaData metaData, InputStream data )
            throws IOException;

    /**
     * @param data
     * @return DoubleMatrixNamed
     * @throws IOException
     */
    public abstract DoubleMatrix<String, String> parse( InputStream data ) throws IOException;

}