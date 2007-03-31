/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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
import ubic.gemma.datastructure.matrix.ExpressionDataMatrixRowElement;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.biosequence.BioSequence;

/**
 * @author pavlidis
 * @version $Id$
 */
public class AffyProbeNameFilter implements Filter<ExpressionDataDoubleMatrix> {

    private static Log log = LogFactory.getLog( AffyProbeNameFilter.class.getName() );

    private boolean skip_ST = false;
    private boolean skip_AFFX = false;
    private boolean skip_F = false;
    private boolean skip_X = false;
    private boolean skip_G = false;

    public enum Pattern {
        ST, AFFX, F, X, G
    };

    /**
     * @param criteria int[] of constants indicating the criteria to use.
     */
    public AffyProbeNameFilter( Pattern[] criteria ) {
        this.setCriteria( criteria );
    }

    private void setCriteria( Pattern[] criteria ) {
        for ( int i = 0; i < criteria.length; i++ ) {
            switch ( criteria[i] ) {
                case ST: {
                    skip_ST = true;
                }
                case AFFX: {
                    skip_AFFX = true;
                }
                case F: {
                    skip_F = true;
                }
                case X: {
                    skip_X = true;
                }
                case G: {
                    skip_G = true;
                }
                default: {
                    break;
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.filter.Filter#filter(ubic.gemma.datastructure.matrix.ExpressionDataMatrix)
     */
    public ExpressionDataDoubleMatrix filter( ExpressionDataDoubleMatrix data ) {
        int numRows = data.rows();

        List<ExpressionDataMatrixRowElement> rowElements = data.getRowElements();

        assert rowElements != null;

        List<Integer> kept = new ArrayList<Integer>();
        for ( int i = 0; i < numRows; i++ ) {

            ExpressionDataMatrixRowElement rowEl = rowElements.get( i );

            assert rowEl != null;

            BioSequence sequence = ( ( CompositeSequence ) rowEl.getDesignElement() ).getBiologicalCharacteristic();

            String name = sequence.getName();

            // apply the rules.
            if ( skip_ST && name.contains( "_st" ) ) { // 'st' means sense strand.
                continue;
            }

            if ( skip_AFFX && name.contains( "AFFX" ) ) {
                continue;
            }

            if ( skip_F && name.contains( "_f_at" ) ) { // gene family. We don't
                // like.
                continue;
            }

            if ( skip_X && name.contains( "_x_at" ) ) {
                continue;
            }
            if ( skip_G && name.contains( "_g_at" ) ) {
                continue;
            }

            kept.add( i );
        }

        log.info( "There are " + kept.size() + " rows left after filtering." );

        return new ExpressionDataDoubleMatrix( data, kept );
    }

}
