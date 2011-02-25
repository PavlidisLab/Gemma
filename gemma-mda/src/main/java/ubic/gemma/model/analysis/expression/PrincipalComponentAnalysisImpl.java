/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2010 University of British Columbia
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
/**
 * This is only generated once! It will never be overwritten.
 * You can (and have to!) safely modify it by hand.
 */
package ubic.gemma.model.analysis.expression;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;

import ubic.basecode.io.ByteArrayConverter;
import ubic.gemma.model.analysis.Eigenvalue;
import ubic.gemma.model.analysis.Eigenvector;

/**
 * @see ubic.gemma.model.analysis.PrincipalComponentAnalysis
 */
public class PrincipalComponentAnalysisImpl extends ubic.gemma.model.analysis.expression.PrincipalComponentAnalysis {
    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 2431137012977899024L;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.analysis.expression.PrincipalComponentAnalysis#getEigenvectorArrays()
     */
    @Override
    public List<Double[]> getEigenvectorArrays() {
        ByteArrayConverter bac = new ByteArrayConverter();
        List<Double[]> result = new ArrayList<Double[]>( this.getNumComponentsStored() );
        for ( int i = 0; i < this.getNumComponentsStored(); i++ ) {
            result.add( null );
        }
        for ( Eigenvector ev : this.getEigenVectors() ) {
            result.set( ev.getComponentNumber() - 1, ArrayUtils.toObject( bac.byteArrayToDoubles( ev.getVector() ) ) );
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.analysis.expression.PrincipalComponentAnalysis#getVarianceFractions()
     */
    @Override
    public Double[] getVarianceFractions() {
        Double[] result = new Double[this.getEigenValues().size()];
        for ( Eigenvalue v : this.getEigenValues() ) {
            result[v.getComponentNumber() - 1] = v.getVarianceFraction();
        }
        return result;
    }

}