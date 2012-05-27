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
import java.util.Collection;
import java.util.List;

import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.expression.designElement.CompositeSequence;

/**
 * Filter to remove rows from an experiment matrix based on the probes.
 * 
 * @author paul
 * @version $Id$
 */
public class RowNameFilter implements Filter<ExpressionDataDoubleMatrix> {

    private List<CompositeSequence> keepers;

    /**
     * @param keepers list of probes that will be retained.
     */
    public RowNameFilter( Collection<CompositeSequence> keepers ) {
        this.keepers = new ArrayList<CompositeSequence>( keepers );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.preprocess.filter.Filter#filter(ubic.gemma.datastructure.matrix.ExpressionDataMatrix)
     */
    @Override
    public ExpressionDataDoubleMatrix filter( ExpressionDataDoubleMatrix dataMatrix ) {
        return new ExpressionDataDoubleMatrix( dataMatrix, keepers );
    }

}
