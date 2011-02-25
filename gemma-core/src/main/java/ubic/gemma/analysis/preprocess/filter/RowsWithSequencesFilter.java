/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
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
package ubic.gemma.analysis.preprocess.filter;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.expression.designElement.CompositeSequence;

/**
 * Remove rows that have no BioSequence associated with the row.
 * 
 * @author paul
 * @version $Id$
 */
public class RowsWithSequencesFilter implements Filter<ExpressionDataDoubleMatrix> {

    private static Log log = LogFactory.getLog( RowsWithSequencesFilter.class.getName() );

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.preprocess.filter.Filter#filter(ubic.gemma.datastructure.matrix.ExpressionDataMatrix)
     */
    public ExpressionDataDoubleMatrix filter( ExpressionDataDoubleMatrix dataMatrix ) {

        List<CompositeSequence> kept = new ArrayList<CompositeSequence>();
        int numRows = dataMatrix.rows();
        for ( int i = 0; i < numRows; i++ ) {
            CompositeSequence cs = dataMatrix.getDesignElementForRow( i );
            if ( cs.getBiologicalCharacteristic() != null ) {
                kept.add( cs );
            }
        }

        log.info( "Retaining " + kept.size() + "/" + numRows + " rows that have associated BioSequences" );

        return new ExpressionDataDoubleMatrix( dataMatrix, kept );
    }

}
