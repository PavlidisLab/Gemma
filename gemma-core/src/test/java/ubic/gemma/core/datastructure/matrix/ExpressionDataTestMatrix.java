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

package ubic.gemma.core.datastructure.matrix;

import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.gemma.core.loader.expression.simple.SimpleExpressionDataLoaderService;
import ubic.gemma.core.loader.expression.simple.SimpleExpressionDataLoaderServiceImpl;
import ubic.gemma.core.loader.expression.simple.model.SimpleExpressionExperimentMetaData;
import ubic.gemma.model.common.quantitationtype.GeneralType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssayData.BulkExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;

/**
 * Creates a sample small test matrix, not persistent. Useful for testing algorithms. (This is not a test!)
 *
 * @author paul
 */
public class ExpressionDataTestMatrix extends ExpressionDataDoubleMatrix {

    private static final long serialVersionUID = 1L;

    public ExpressionDataTestMatrix() throws IOException {
        super();
        Collection<ArrayDesign> ads = new HashSet<>();
        SimpleExpressionDataLoaderService service = new SimpleExpressionDataLoaderServiceImpl();

        SimpleExpressionExperimentMetaData metaData = new SimpleExpressionExperimentMetaData();

        Taxon taxon = Taxon.Factory.newInstance();
        taxon.setCommonName( "mouse" );
        taxon.setIsGenesUsable( true );

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

        try ( InputStream data = this.getClass()
                .getResourceAsStream( "/data/loader/aov.results-2-monocyte-data-bytime.bypat.data.sort" ) ) {
            DoubleMatrix<String, String> matrix = service.parse( data );
            ExpressionExperiment ee = service.convert( metaData, matrix );
            super.init();
            Collection<BulkExpressionDataVector> selectedVectors = super.selectVectors( ee, ee.getQuantitationTypes().iterator().next() );
            this.vectorsToMatrix( selectedVectors );
        }

    }

}
