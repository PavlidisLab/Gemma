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

package ubic.gemma.datastructure.matrix;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;

import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.gemma.loader.expression.simple.SimpleExpressionDataLoaderService;
import ubic.gemma.loader.expression.simple.SimpleExpressionDataLoaderServiceImpl;
import ubic.gemma.loader.expression.simple.model.SimpleExpressionExperimentMetaData;
import ubic.gemma.model.common.quantitationtype.GeneralType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;

/**
 * Creates a sample small test matrix, not persistent. Useful for testing algorithms. (This is not a test!)
 * 
 * @author paul
 * @version $Id$
 */
public class ExpressionDataTestMatrix extends ExpressionDataDoubleMatrix {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public ExpressionDataTestMatrix() {
        super();
        Collection<ArrayDesign> ads = new HashSet<ArrayDesign>();
        SimpleExpressionDataLoaderService service = new SimpleExpressionDataLoaderServiceImpl();

        SimpleExpressionExperimentMetaData metaData = new SimpleExpressionExperimentMetaData();

        Taxon taxon = Taxon.Factory.newInstance();
        taxon.setCommonName( "mouse" );
        taxon.setIsGenesUsable( true );
        taxon.setIsSpecies( true );

        ArrayDesign ad = ArrayDesign.Factory.newInstance();
        ad.setName( "new ad" );
        ad.setPrimaryTaxon( taxon );
        ads.add( ad );
        metaData.setArrayDesigns( ads );

        metaData.setTaxon( taxon );
        metaData.setName( "ee" );

        metaData.setQuantitationTypeName( "testing" );
        metaData.setGeneralType( GeneralType.QUANTITATIVE );
        metaData.setScale( ScaleType.LOG2 );
        metaData.setType( StandardQuantitationType.AMOUNT );
        metaData.setIsRatio( true );

        InputStream data = this.getClass().getResourceAsStream(
                "/data/loader/aov.results-2-monocyte-data-bytime.bypat.data.sort" );
        try {
            DoubleMatrix<String, String> matrix = service.parse( data );
            ExpressionExperiment ee = service.convert( metaData, matrix );
            super.init();
            Collection<DesignElementDataVector> selectedVectors = super.selectVectors( ee, ee.getQuantitationTypes()
                    .iterator().next() );
            vectorsToMatrix( selectedVectors );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

}
