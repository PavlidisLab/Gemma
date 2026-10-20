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
import ubic.gemma.model.genome.biosequence.BioSequence;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Only retain design elements that have a {@link BioSequence} associated.
 *
 * @author paul
 */
public class RowsWithSequencesFilter implements ExpressionDataFilter<ExpressionDataDoubleMatrix> {

    private static final Log log = LogFactory.getLog( RowsWithSequencesFilter.class.getName() );

    @Override
    public ExpressionDataDoubleMatrix filter( ExpressionDataDoubleMatrix dataMatrix ) throws NoDesignElementsException {
        List<CompositeSequence> kept = dataMatrix.getDesignElements().stream()
                .filter( cs -> cs.getBiologicalCharacteristic() != null )
                .collect( Collectors.toList() );

        if ( kept.isEmpty() ) {
            throw new NoDesignElementsException( "No design element left after filtering for those having associated BioSequences." );
        }

        log.info( String.format( "Retaining %d/%d design elements that have associated BioSequences", kept.size(),
                dataMatrix.rows() ) );

        return dataMatrix.sliceRows( kept );
    }

    @Override
    public String toString() {
        return "RowWithSequencesFilter";
    }
}
