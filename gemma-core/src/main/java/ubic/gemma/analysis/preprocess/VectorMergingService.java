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
package ubic.gemma.analysis.preprocess;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.io.ByteArrayConverter;
import ubic.gemma.datastructure.matrix.ExpressionDataBooleanMatrix;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.datastructure.matrix.ExpressionDataMatrix;
import ubic.gemma.datastructure.matrix.ExpressionDataMatrixRowElement;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;

/**
 * Tackles the problem of concatenating DesignElementDataVectors for a single experiment. This is necessary When a study
 * uses two or more similar array designs without 'replication'. Typical of the genre is GSE60 ("Diffuse large B-cell
 * lymphoma"), with 31 BioAssays on GPL174, 35 BioAssays on GPL175, and 66 biomaterials.
 * <p>
 * The algorithm for dealing with this is a preprocessing step:
 * <ol>
 * <li>Generate a merged ExpressionDataMatrix for each of the (important) quantitation types.</li>
 * <li>Create a merged BioAssayDimension</li>
 * <li>Persist the new vectors, which are now tied to a <em>single DesignElement</em>. This is, strictly speaking,
 * incorrect, but because the design elements used in the vector all point to the same sequence, there is no problem in
 * analyzing this. However, there is a potential loss of information.
 * </ol>
 * <p>
 * The last persisting step is not handled by this class.
 * 
 * @author pavlidis
 * @version $Id$
 * @see ExpressionDataMatrixBuilder
 */
public class VectorMergingService {

    private static Log log = LogFactory.getLog( VectorMergingService.class.getName() );

    /**
     * @param builder
     * @return collection of designElementDataVectors for persisting, which will be empty if problems were encountered.
     */
    public Collection<DesignElementDataVector> mergeVectors( ExpressionDataMatrixBuilder builder ) {

        ExpressionDataDoubleMatrix matrix;
        Collection<DesignElementDataVector> vectors = new ArrayList<DesignElementDataVector>();

        matrix = builder.getPreferredData();
        makeVectors( builder, matrix, vectors );

        matrix = builder.getBackgroundChannelA( null );
        makeVectors( builder, matrix, vectors );

        matrix = builder.getBackgroundChannelB( null );
        makeVectors( builder, matrix, vectors );

        matrix = builder.getSignalChannelA( null );
        makeVectors( builder, matrix, vectors );

        matrix = builder.getSignalChannelB( null );
        makeVectors( builder, matrix, vectors );

        ExpressionDataBooleanMatrix bmatrix = builder.getMissingValueData( null );
        makeVectors( builder, bmatrix, vectors );

        return vectors;

    }

    /**
     * @param builder
     * @param matrix
     * @param vectors
     */
    @SuppressWarnings("unchecked")
    private void makeVectors( ExpressionDataMatrixBuilder builder, ExpressionDataMatrix matrix,
            Collection<DesignElementDataVector> vectors ) {

        if ( matrix == null ) return;

        ByteArrayConverter bac = new ByteArrayConverter();

        BioAssayDimension bad = null;
        try {
            bad = matrix.getBioAssayDimension();
        } catch ( IllegalArgumentException e ) {
            return;
        }

        Collection<QuantitationType> quantitationTypes = matrix.getQuantitationTypes();
        if ( quantitationTypes.size() > 1 ) {
            log.error( "More than one quantitationType for matrix" );
            return;
        }

        QuantitationType qt = quantitationTypes.iterator().next();

        for ( ExpressionDataMatrixRowElement el : ( List<ExpressionDataMatrixRowElement> ) matrix.getRowElements() ) {
            matrix.getRow( el.getIndex() );
            DesignElementDataVector vector = DesignElementDataVector.Factory.newInstance();
            vector.setBioAssayDimension( bad );
            vector.setDesignElement( el.getDesignElements().iterator().next() );
            vector.setExpressionExperiment( builder.getExpressionExperiment() );
            vector.setQuantitationType( qt );
            byte[] data = bac.toBytes( matrix.getRow( el.getIndex() ) );
            vector.setData( data );
            vectors.add( vector );
        }
    }

}
