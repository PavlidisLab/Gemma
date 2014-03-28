/*
 * The linkAnalysis project
 * 
 * Copyright (c) 2006 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.analysis.expression.coexpression.links;

import java.util.Map;
import java.util.Set;

import ubic.gemma.datastructure.matrix.ExpressionDataMatrixRowElement;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Gene;
import cern.colt.list.DoubleArrayList;
import cern.colt.list.ObjectArrayList;

/**
 * @author Paul Pavlidis
 * @version $Id$
 */
public interface MatrixRowPairAnalysis {
    static final int NUM_BINS = 2048;

    public void calculateMetrics();

    public DoubleArrayList getHistogramArrayList();

    public ObjectArrayList getKeepers();

    public QuantitationType getMetricType();

    public CompositeSequence getProbeForRow( ExpressionDataMatrixRowElement rowEl );

    public double getScoreInBin( int i );

    public int numCached();

    public void setDuplicateMap( Map<CompositeSequence, Set<Gene>> probeToGeneMap );

    public void setLowerTailThreshold( double k );

    public void setMinNumpresent( int minSamplesToKeepCorrelation );

    public void setOmitNegativeCorrelationLinks( boolean omitNegativeCorrelationLinks );

    public void setPValueThreshold( double k );

    public void setUpperTailThreshold( double k );

    public void setUseAbsoluteValue( boolean k );

    /**
     * Default is true; set to false to disable use of the pvalue threshold, in which case only the upper and lower tail
     * thresholds will be used.
     * 
     * @param useIt
     */
    public void setUsePvalueThreshold( boolean useIt );

    /**
     * Use after analysis.
     * 
     * @return how many pairs were rejected due to cross-hybridization potential
     */
    long getCrossHybridizationRejections();

    double getNumUniqueGenes();

    /**
     * @return how many rows/columns the matrix has.
     */
    int size();

}
