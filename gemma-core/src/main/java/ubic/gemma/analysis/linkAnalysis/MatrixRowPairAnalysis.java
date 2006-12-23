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

import hep.aida.IHistogram1D;

import java.util.Map;

import ubic.basecode.dataStructure.matrix.NamedMatrix;
import cern.colt.list.DoubleArrayList;
import cern.colt.list.ObjectArrayList;

/**
 * @author Paul Pavlidis
 * @version $Id$
 */

public interface MatrixRowPairAnalysis {

    public void calculateMetrics();

    public void setUseAbsoluteValue( boolean k );

    public void setPValueThreshold( double k );

    public IHistogram1D getHistogram();

    public void setDuplicateMap( Map geneToProbeMap, Map probeToGeneMap );
    
    public void setLowerTailThreshold( double k );

    public void setUpperTailThreshold( double k );

    public int numCached();

    public ObjectArrayList getKeepers();

    public NamedMatrix getMatrix();

    public DoubleArrayList getHistogramArrayList();
   
}
