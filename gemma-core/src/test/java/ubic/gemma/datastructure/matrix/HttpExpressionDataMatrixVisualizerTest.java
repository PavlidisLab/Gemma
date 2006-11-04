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
package ubic.gemma.datastructure.matrix;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.dataStructure.matrix.DoubleMatrixNamed;
import ubic.basecode.gui.ColorMatrix;
import ubic.basecode.io.ByteArrayConverter;
import ubic.basecode.io.StringConverter;
import ubic.gemma.loader.expression.simple.SimpleExpressionDataLoaderService;
import ubic.gemma.loader.expression.simple.model.SimpleExpressionExperimentMetaData;
import ubic.gemma.model.common.quantitationtype.GeneralType;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.testing.BaseSpringContextTest;
import ubic.gemma.visualization.DefaultExpressionDataMatrixVisualizer;
import ubic.gemma.visualization.ExpressionDataMatrixVisualizer;

/**
 * @author keshav
 * @version $Id$
 */
public class HttpExpressionDataMatrixVisualizerTest extends BaseSpringContextTest {
    Log log = LogFactory.getLog( this.getClass() );
    String[] rowNames = null;
    String[] colNames = null;

    ExpressionDataMatrix matrixData = null;
    ByteArrayConverter bconverter = null;
    StringConverter sconverter = null;
    QuantitationType quantitationType = null;

    SimpleExpressionExperimentMetaData metaData = null;

    DoubleMatrixNamed matrix = null;

    ExpressionExperiment ee = null;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.testing.BaseSpringContextTest#onSetUpInTransaction()
     */
    @Override
    protected void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();

        SimpleExpressionDataLoaderService service = ( SimpleExpressionDataLoaderService ) this
                .getBean( "simpleExpressionDataLoaderService" );

        metaData = new SimpleExpressionExperimentMetaData();
        ArrayDesign ad = ArrayDesign.Factory.newInstance();
        ad.setName( "new ad" );
        Collection<ArrayDesign> ads = new HashSet<ArrayDesign>();
        ads.add( ad );
        metaData.setArrayDesigns( ads );

        Taxon taxon = Taxon.Factory.newInstance();
        taxon.setCommonName( "mouse" );
        metaData.setTaxon( taxon );
        metaData.setName( "ee" );
        metaData.setQuantitationTypeName( "testing" );
        metaData.setGeneralType( GeneralType.QUANTITATIVE );
        metaData.setScale( ScaleType.LOG2 );
        metaData.setType( StandardQuantitationType.RATIO );

        InputStream data = this.getClass().getResourceAsStream(
                "/data/loader/aov.results-2-monocyte-data-bytime.bypat.data.sort" );

        matrix = service.parse( data );
        ee = service.convert( metaData, matrix );

        /* create the ExpressionDataDoubleMatrix */
        QuantitationType quantitationType = QuantitationType.Factory.newInstance();
        quantitationType.setName( metaData.getQuantitationTypeName() );
        quantitationType.setDescription( metaData.getQuantitationTypeDescription() );
        quantitationType.setGeneralType( GeneralType.QUANTITATIVE );
        quantitationType.setType( metaData.getType() );
        quantitationType.setRepresentation( PrimitiveType.DOUBLE );
        quantitationType.setScale( metaData.getScale() );
        quantitationType.setIsBackground( false );

        Collection<DesignElementDataVector> designElementDataVectors = ee.getDesignElementDataVectors();
        Collection<DesignElement> designElements = new HashSet<DesignElement>();
        for ( DesignElementDataVector designElementDataVector : designElementDataVectors ) {
            DesignElement de = designElementDataVector.getDesignElement();
            Collection<DesignElementDataVector> vectors = new HashSet<DesignElementDataVector>();
            vectors.add( designElementDataVector ); // associate vectors with design elements in memory
            de.setDesignElementDataVectors( vectors );
            designElements.add( de );
        }

        matrixData = new ExpressionDataDoubleMatrix( ee, designElements, quantitationType );

    }

    /**
     * @throws Exception
     */
    public void testMatrixVisualizationData() throws Exception {

        // vizualizationData.printData();

        File tmp = File.createTempFile( "testOut", ".png" );

        ExpressionDataMatrixVisualizer visualizer = new DefaultExpressionDataMatrixVisualizer( matrixData, tmp
                .getAbsolutePath() );

        ColorMatrix colorMatrix = visualizer.createColorMatrix( matrixData );

        visualizer.saveImage( tmp, colorMatrix );

        tmp.deleteOnExit();

        FileInputStream fis = new FileInputStream( tmp );

        assertNotNull( fis );
        assertTrue( tmp.length() > 0 );
        fis.close();
    }
}
