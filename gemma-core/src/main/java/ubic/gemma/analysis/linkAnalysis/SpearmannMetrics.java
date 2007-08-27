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
import ubic.gemma.model.common.quantitationtype.GeneralType;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.genome.Gene;
import cern.colt.list.DoubleArrayList;
import cern.colt.list.ObjectArrayList;

/**
 * @author not attributable
 * @version $Id$
 */

public class SpearmannMetrics implements MatrixRowPairAnalysis {

    private int minSamplesToKeepCorrelation = 0;

    public SpearmannMetrics() {
        throw new UnsupportedOperationException();
    }

    public void calculateMetrics() {
        // TODO Auto-generated method stub

    }

    public DoubleArrayList getHistogramArrayList() {
        // TODO Auto-generated method stub
        return null;
    }

    public ObjectArrayList getKeepers() {
        // TODO Auto-generated method stub
        return null;
    }

    public DesignElement getProbeForRow( ExpressionDataMatrixRowElement rowEl ) {
        // TODO Auto-generated method stub
        return null;
    }

    public double getScoreInBin( int i ) {
        // TODO Auto-generated method stub
        return 0;
    }

    public int numCached() {
        // TODO Auto-generated method stub
        return 0;
    }

    public void setDuplicateMap( Map<CompositeSequence, Collection<Gene>> m1,
            Map<Gene, Collection<CompositeSequence>> m2 ) {
        // TODO Auto-generated method stub

    }

    public void setLowerTailThreshold( double k ) {
        // TODO Auto-generated method stub

    }

    public void setPValueThreshold( double k ) {
        // TODO Auto-generated method stub

    }

    public void setUpperTailThreshold( double k ) {
        // TODO Auto-generated method stub

    }

    public void setUseAbsoluteValue( boolean k ) {
        // TODO Auto-generated method stub

    }

    public QuantitationType getMetricType() {
        QuantitationType m = QuantitationType.Factory.newInstance();
        m.setIsBackground( false );
        m.setIsBackgroundSubtracted( false );
        m.setIsNormalized( false );
        m.setIsPreferred( false );
        m.setIsRatio( false );
        m.setType( StandardQuantitationType.CORRELATION );
        m.setName( "Spearmann rank correlation" );
        m.setGeneralType( GeneralType.QUANTITATIVE );
        m.setRepresentation( PrimitiveType.DOUBLE );
        m.setScale( ScaleType.LINEAR );
        return m;
    }

    public void setMinNumpresent( int minSamplesToKeepCorrelation ) {
        this.minSamplesToKeepCorrelation = minSamplesToKeepCorrelation;
    }

}
