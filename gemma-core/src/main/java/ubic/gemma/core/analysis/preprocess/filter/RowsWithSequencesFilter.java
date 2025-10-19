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
package ubic.gemma.core.analysis.preprocess.filter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.expression.designElement.CompositeSequence;

import java.util.ArrayList;
import java.util.List;

/**
 * Remove rows that have no BioSequence associated with the row.
 *
 * @author paul
 */
public class RowsWithSequencesFilter implements Filter<ExpressionDataDoubleMatrix> {

    private static final Log log = LogFactory.getLog( RowsWithSequencesFilter.class.getName() );

    @Override
    public ExpressionDataDoubleMatrix filter( ExpressionDataDoubleMatrix dataMatrix ) {

        List<CompositeSequence> kept = new ArrayList<>();
        int numRows = dataMatrix.rows();
        for ( int i = 0; i < numRows; i++ ) {
            CompositeSequence cs = dataMatrix.getDesignElementForRow( i );
            if ( cs.getBiologicalCharacteristic() != null ) {
                kept.add( cs );
            }
        }

        RowsWithSequencesFilter.log
                .info( "Retaining " + kept.size() + "/" + numRows + " rows that have associated BioSequences" );

        return dataMatrix.sliceRows( kept );
    }

    @Override
    public String toString() {
        return "RowWithSequencesFilter";
    }
}
