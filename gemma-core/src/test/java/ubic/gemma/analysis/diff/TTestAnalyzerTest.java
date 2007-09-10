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
package ubic.gemma.analysis.diff;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.datastructure.matrix.ExpressionDataMatrix;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author keshav
 * @version $Id$
 */
public class TTestAnalyzerTest extends BaseSpringContextTest {

    private Log log = LogFactory.getLog( this.getClass() );

    TTestAnalyzer analyzer = new TTestAnalyzer();

    ExpressionDataMatrix matrix = null;

    Collection<BioMaterial> biomaterials = new ArrayList<BioMaterial>();

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.testing.BaseSpringContextTest#onSetUpInTransaction()
     */
    @Override
    protected void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();

        ExpressionExperiment ee = this.getTestPersistentCompleteExpressionExperiment();

        Collection<DesignElementDataVector> updatedVectors = new ArrayList<DesignElementDataVector>();

        Collection<DesignElementDataVector> dedvs = ee.getDesignElementDataVectors();

        QuantitationType quantitationTypeToUse = null;

        for ( DesignElementDataVector vector : dedvs ) {

            // FIXME maybe the test ee should only have one QT
            if ( quantitationTypeToUse == null
                    && vector.getQuantitationType().getType() == StandardQuantitationType.AMOUNT )
                quantitationTypeToUse = vector.getQuantitationType();

            vector.setQuantitationType( quantitationTypeToUse );

            updatedVectors.add( vector );
        }
        matrix = new ExpressionDataDoubleMatrix( updatedVectors );

        Collection<BioAssay> assays = ee.getBioAssays();
        for ( BioAssay assay : assays ) {
            log.debug( assay.getName() );
            Collection<BioMaterial> materials = assay.getSamplesUsed();
            if ( materials.size() != 1 )
                throw new RuntimeException( "Only supporting 1 biomaterial/bioassay at this time" );

            biomaterials.addAll( materials );
        }
    }

    /**
     * 
     *
     */
    public void testTTest() {

        analyzer.tTest( matrix, biomaterials );
    }

}
