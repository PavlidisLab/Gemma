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
package ubic.gemma.visualization;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.SessionFactory;

import ubic.basecode.gui.ColorMatrix;
import ubic.gemma.loader.util.converter.SimpleExpressionExperimentConverter;
import ubic.gemma.loader.util.parser.TabDelimParser;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.PersisterHelper;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author keshav
 * @version $Id$
 */
public class VisualizeDataSetIntegrationTest extends BaseSpringContextTest {
    private static Log log = LogFactory.getLog( VisualizeDataSetIntegrationTest.class );

    private String filename = "aov.results-2-monocyte-data-bytime.bypat.data.sort";

    private ExpressionExperiment expressionExperiment;

    private SimpleExpressionExperimentConverter converter = null;

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.test.AbstractTransactionalSpringContextTests#onSetUpInTransaction()
     */
    @Override
    protected void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();
        converter = new SimpleExpressionExperimentConverter( "Expression experiment " + filename,
                "An expression experiment based on data from " + filename, "Pappapanou", "Pavlidis collaboration" );
    }

    /**
     * @param headerExists
     * @return
     * @throws IOException
     */
    private Collection<String[]> parseData( boolean headerExists ) throws IOException {

        InputStream is = VisualizeDataSetIntegrationTest.class.getResourceAsStream( "/data/loader/" + filename );

        TabDelimParser parser = new TabDelimParser();
        parser.parse( is );

        Collection<String[]> results = parser.getResults();

        return results;
    }

    /**
     * @param dataCol
     * @return Collection<String[]>
     */
    private Collection<String[]> prepareData( Collection<String[]> dataCol ) {

        if ( converter == null ) {
            log.info( "Null converter.  Creating a new one" );
            converter = new SimpleExpressionExperimentConverter( "Expression experiment " + filename,
                    "An expression experiment based on data from " + filename, "Pappapanou", "Pavlidis collaboration" );
        }
        Collection<String[]> rawDataCol = converter.prepareData( dataCol, true, true );
        return rawDataCol;

    }

    /**
     * Composition relationships
     * 
     * @param dataCol
     * @param header
     */
    private void convertData( Collection<String[]> dataCol, boolean header ) {

        if ( header )
            expressionExperiment = converter.convert( dataCol, true, true );

        else
            expressionExperiment = converter.convert( dataCol );
    }

    /**
     * 
     *
     */
    private void loadData() {

        PersisterHelper persister = new PersisterHelper();
        persister
                .setExpressionExperimentService( ( ExpressionExperimentService ) getBean( "expressionExperimentService" ) );
        persister.setSessionFactory( ( SessionFactory ) getBean( "sessionFactory" ) );
        persister.persist( expressionExperiment );
        // persister.persist( arrayDesign );

    }

    /**
     * 
     *
     */
    private double[][] parsePrepareAndConvertRawData() {

        try {
            Collection<String[]> results = parseData( true );
            assertEquals( 201, results.size() );

            Collection<String[]> data = prepareData( results );
            assertEquals( 200, data.size() );

            return converter.convertRawData( data );

            // TODO move this.
            // convertData( results, true );
            // assertNotNull( expressionExperiment );
            //
            // convertData( rawData, false );
            // assertNotNull( expressionExperiment );

            // loadData();
        } catch ( IOException e ) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 
     *
     */
    public void testVisualize() {

        double[][] rawData = parsePrepareAndConvertRawData();
        assertNotNull( rawData );

        DataMatrixVisualizer visualizer = new DefaultDataMatrixVisualizer( "outFile.png" );

        ColorMatrix colorMatrix = visualizer.createColorMatrix( rawData, Arrays.asList( converter.getRowNames() ),
                Arrays.asList( converter.getHeader() ) );

        try {
            visualizer.saveImage( colorMatrix );
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }
}
