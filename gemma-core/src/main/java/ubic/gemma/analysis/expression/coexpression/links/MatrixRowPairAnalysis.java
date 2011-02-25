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

import java.util.Collection;
import java.util.Map;

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

    /**
     * Get pvalue corrected for multiple testing of the genes. We conservatively penalize the pvalues for each
     * additional test the gene received. For example, if correlation is between two probes that each assay two genes,
     * the pvalue is penalized by a factor of 4.0.
     * 
     * @param i int
     * @param j int
     * @param correl double
     * @param numused int
     * @return double
     */
    double correctedPvalue( int i, int j, double correl, int numused );

    public void calculateMetrics();

    public QuantitationType getMetricType();

    public void setUseAbsoluteValue( boolean k );

    public void setPValueThreshold( double k );

    /**
     * Default is true; set to false to disable use of the pvalue threshold, in which case only the upper and lower tail
     * thresholds will be used.
     * 
     * @param useIt
     */
    public void setUsePvalueThreshold( boolean useIt );

    public void setOmitNegativeCorrelationLinks( boolean omitNegativeCorrelationLinks );

    public void setDuplicateMap( Map<CompositeSequence, Collection<Collection<Gene>>> m1 );

    public void setLowerTailThreshold( double k );

    public void setUpperTailThreshold( double k );

    public int numCached();

    public ObjectArrayList getKeepers();

    public DoubleArrayList getHistogramArrayList();

    public double getScoreInBin( int i );

    public CompositeSequence getProbeForRow( ExpressionDataMatrixRowElement rowEl );

    public void setMinNumpresent( int minSamplesToKeepCorrelation );

    double getNumUniqueGenes();

    /**
     * @return how many rows/columsn the matrix has.
     */
    int size();

}
