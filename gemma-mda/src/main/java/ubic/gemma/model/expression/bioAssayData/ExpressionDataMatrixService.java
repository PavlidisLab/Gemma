/*
 * The Gemma project
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
package ubic.gemma.model.expression.bioAssayData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.dataStructure.matrix.DoubleMatrix2DNamedFactory;
import ubic.basecode.dataStructure.matrix.DoubleMatrixNamed;
import ubic.basecode.io.ByteArrayConverter;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Produces DoubleMatrix objects for ExpressionExperiments.
 * 
 * @author pavlidis
 * @version $Id$
 * @spring.bean id="expressionDataMatrixService"
 * @spring.property name="designElementDataVectorService" ref = "designElementDataVectorService"
 */
public class ExpressionDataMatrixService {

    private static Log log = LogFactory.getLog( ExpressionDataMatrixService.class.getName() );

    DesignElementDataVectorService designElementDataVectorService;

    /**
     * @param expExp
     * @param assayDimension
     * @return
     */
    @SuppressWarnings("unchecked")
    public DoubleMatrixNamed getMatrix( ExpressionExperiment expExp, QuantitationType qt ) {
        Collection<DesignElementDataVector> vectors = this.designElementDataVectorService.findAllForMatrix( expExp, qt );
        if ( vectors == null || vectors.size() == 0 ) {
            log.warn( "No vectors for " + expExp + " and " + qt );
            return null;
        }
        return vectorsToDoubleMatrix( vectors );
    }

    /**
     * @param designElementDataVectorService The designElementDataVectorService to set.
     */
    public void setDesignElementDataVectorService( DesignElementDataVectorService designElementDataVectorService ) {
        this.designElementDataVectorService = designElementDataVectorService;
    }

    /**
     * Convert {@link DesignElementDataVector}s into a {@link DoubleMatrixNamed}.
     * 
     * @param vectors
     * @return
     */
    private DoubleMatrixNamed vectorsToDoubleMatrix( Collection<DesignElementDataVector> vectors ) {
        if ( vectors == null || vectors.size() == 0 ) {
            return null;
        }

        ByteArrayConverter bac = new ByteArrayConverter();

        List<BioAssay> bioAssays = ( List<BioAssay> ) vectors.iterator().next().getBioAssayDimension().getBioAssays();

        assert bioAssays.size() > 0 : "Empty BioAssayDimension for the vectors";

        DoubleMatrixNamed matrix = DoubleMatrix2DNamedFactory.fastrow( vectors.size(), bioAssays.size() );

        // Use BioMaterial names to represent the column in the matrix (as it can span multiple BioAssays)
        for ( BioAssay assay : bioAssays ) {
            StringBuilder buf = new StringBuilder();
            List<BioMaterial> bms = new ArrayList<BioMaterial>( assay.getSamplesUsed() );
            // Collections.sort( bms ); // FIXME this should use a sort.
            for ( BioMaterial bm : ( Collection<BioMaterial> ) bms ) {
                buf.append( bm.getName() );
            }
            matrix.addColumnName( buf.toString() );
        }

        int rowNum = 0;
        for ( DesignElementDataVector vector : vectors ) {
            String name = vector.getDesignElement().getName();
            matrix.addRowName( name );
            byte[] bytes = vector.getData();
            double[] vals = bac.byteArrayToDoubles( bytes );
            assert vals.length == bioAssays.size() : "Number of values in vector (" + vals.length
                    + ") don't match number of Bioassays (" + bioAssays.size() + ")";
            for ( int i = 0; i < vals.length; i++ ) {
                matrix.setQuick( rowNum, i, vals[i] );
            }
            rowNum++;
        }
        return matrix;
    }
}
