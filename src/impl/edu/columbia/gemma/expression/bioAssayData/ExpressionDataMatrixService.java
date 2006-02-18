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
package edu.columbia.gemma.expression.bioAssayData;

import java.util.Collection;
import java.util.List;

import baseCode.dataStructure.matrix.DoubleMatrix2DNamedFactory;
import baseCode.dataStructure.matrix.DoubleMatrixNamed;
import baseCode.io.ByteArrayConverter;
import edu.columbia.gemma.common.quantitationtype.QuantitationType;
import edu.columbia.gemma.expression.bioAssay.BioAssay;
import edu.columbia.gemma.expression.biomaterial.BioMaterial;
import edu.columbia.gemma.expression.experiment.ExpressionExperiment;

/**
 * Produces DoubleMatrix objects for ExpressionExperiments.
 * 
 * @author pavlidis
 * @version $Id$
 * @spring.bean id="expressionDataMatrixService"
 * @spring.property name="designElementDataVectorService" ref = "designElementDataVectorService"
 */
public class ExpressionDataMatrixService {

    DesignElementDataVectorService designElementDataVectorService;

    /**
     * @param expExp
     * @param assayDimension
     * @return
     */
    @SuppressWarnings("unchecked")
    public DoubleMatrixNamed getMatrix( ExpressionExperiment expExp, QuantitationType qt ) {
        Collection<DesignElementDataVector> vectors = this.designElementDataVectorService.findAllForMatrix( expExp, qt );
        return vectorsToDoubleMatrix( vectors );
    }

    /**
     * @param designElementDataVectorService The designElementDataVectorService to set.
     */
    public void setDesignElementDataVectorService( DesignElementDataVectorService designElementDataVectorService ) {
        this.designElementDataVectorService = designElementDataVectorService;
    }

    /**
     * @param vectors
     * @return
     */
    private DoubleMatrixNamed vectorsToDoubleMatrix( Collection<DesignElementDataVector> vectors ) {
        if ( vectors == null || vectors.size() == 0 ) return null;

        ByteArrayConverter bac = new ByteArrayConverter();

        List<BioAssay> bioAssays = ( List<BioAssay> ) vectors.iterator().next().getBioAssayDimension()
                .getDimensionBioAssays();

        List<BioMaterial> biomaterials = ( List<BioMaterial> ) vectors.iterator().next().getBioAssayDimension()
                .getBioMaterialDimensions().iterator().next().getBioMaterials();

        DoubleMatrixNamed matrix = DoubleMatrix2DNamedFactory.fastrow( vectors.size(), bioAssays.size() );

        // for ( BioAssay assay : bioAssays ) {
        // matrix.addColumnName( assay.getName() );
        // }

        for ( BioMaterial bioMaterial : biomaterials ) {
            matrix.addColumnName( bioMaterial.getName() );
        }

        int rowNum = 0;
        for ( DesignElementDataVector vector : vectors ) {
            String name = vector.getDesignElement().getName();
            matrix.addRowName( name );
            byte[] bytes = vector.getData();
            double[] vals = bac.byteArrayToDoubles( bytes );
            for ( int i = 0; i < vals.length; i++ ) {
                matrix.setQuick( rowNum, i, vals[i] );
            }
            rowNum++;
        }
        return matrix;
    }
}
