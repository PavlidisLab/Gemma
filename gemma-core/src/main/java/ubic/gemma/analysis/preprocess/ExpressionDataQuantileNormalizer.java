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
package ubic.gemma.analysis.preprocess;

import java.io.IOException;

import org.apache.commons.lang.ArrayUtils;

import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;

/**
 * @author Paul
 * @version $Id$
 */
public class ExpressionDataQuantileNormalizer {

    /**
     * Quantile normalize the matrix (in place)
     * 
     * @param matrix
     */
    public static void normalize( ExpressionDataDoubleMatrix matrix ) {

        DoubleMatrix<CompositeSequence, BioMaterial> rawMatrix = matrix.getMatrix();

        try {
            QuantileNormalizer<CompositeSequence, BioMaterial> normalizer = new QuantileNormalizer<CompositeSequence, BioMaterial>();
            DoubleMatrix<CompositeSequence, BioMaterial> normalized = normalizer.normalize( rawMatrix );

            for ( int i = 0; i < normalized.rows(); i++ ) {
                matrix.setRow( i, ArrayUtils.toObject( normalized.getRow( i ) ) );
            }
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }

    }
}
