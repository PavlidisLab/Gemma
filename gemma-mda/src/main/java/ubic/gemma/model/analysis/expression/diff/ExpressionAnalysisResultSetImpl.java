/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2007 University of British Columbia
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
package ubic.gemma.model.analysis.expression.diff;

import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.association.BioSequence2GeneProduct;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.GeneImpl;

/**
 * @see ubic.gemma.model.expression.analysis.ExpressionAnalysisResultSet
 */
public class ExpressionAnalysisResultSetImpl extends ExpressionAnalysisResultSet {
    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 4890973130395919422L;

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();

        for ( DifferentialExpressionAnalysisResult dear : this.getResults() ) {
            int count = 0;

            CompositeSequence cs = dear.getProbe();
            buf.append( cs.getName() + "\t" );
            for ( BioSequence2GeneProduct bs2gp : cs.getBiologicalCharacteristic().getBioSequence2GeneProduct() ) {
                Gene g = bs2gp.getGeneProduct().getGene();
                if ( g instanceof GeneImpl ) {
                    buf.append( bs2gp.getGeneProduct().getGene().getOfficialSymbol() + "," );
                    count++;
                }
            }
            if ( count != 0 ) buf.deleteCharAt( buf.lastIndexOf( "," ) ); // removing trailing ,
            buf.append( "\t" );

            count = 0;
            for ( ExperimentalFactor ef : this.getExperimentalFactors() ) {
                buf.append( ef.getName() + "," );
                count++;
            }
            if ( count != 0 ) buf.deleteCharAt( buf.lastIndexOf( "," ) ); // removing trailing ,

            buf.append( "\t" );

            buf.append( dear.getCorrectedPvalue() + "\n" );
        }
        return buf.toString();

    }

}