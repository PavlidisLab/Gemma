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
package ubic.gemma.analysis.linkAnalysis;

import java.util.Collection;
import java.util.Map;

import ubic.gemma.datastructure.matrix.ExpressionDataMatrixRowElement;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.genome.Gene;
import cern.colt.list.DoubleArrayList;
import cern.colt.list.ObjectArrayList;

/**
 * @author Paul Pavlidis
 * @version $Id$
 */
public interface MatrixRowPairAnalysis {

    public void calculateMetrics();

    public QuantitationType getMetricType();

    public void setUseAbsoluteValue( boolean k );

    public void setPValueThreshold( double k );

    // public IHistogram1D getHistogram();

    /**
     * @param k GroupMap
     */
    public void setDuplicateMap( Map<CompositeSequence, Collection<Gene>> m1,
            Map<Gene, Collection<CompositeSequence>> m2 );

    public void setLowerTailThreshold( double k );

    public void setUpperTailThreshold( double k );

    public int numCached();

    public ObjectArrayList getKeepers();

    public DoubleArrayList getHistogramArrayList();

    public double getScoreInBin( int i );

    public DesignElement getProbeForRow( ExpressionDataMatrixRowElement rowEl );

    public void setMinNumpresent( int minSamplesToKeepCorrelation );

}
