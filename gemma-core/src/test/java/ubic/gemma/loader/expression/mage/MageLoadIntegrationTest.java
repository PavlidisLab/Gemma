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
package ubic.gemma.loader.expression.mage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.InputStream;
import java.util.Collection;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.basecode.util.FileTools;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * @author pavlidis
 * @version $Id$
 */
public class MageLoadIntegrationTest extends AbstractMageTest {

    ExpressionExperiment ee;

    @Autowired
    ExpressionExperimentService expressionExperimentService;

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.test.AbstractDependencyInjectionSpringContextTests#onTearDown()
     */
    @After
    public void teardown() {

        if ( ee != null && ee.getId() != null ) {
            expressionExperimentService.delete( ee );
        }

    }

    /**
     * A real example of an experimental package.
     * 
     * @throws Exception
     */
    @Test
    public void testCreateCollectionReal() throws Exception {
        log.info( "Parsing MAGE from ArrayExpress (WMIT)" );

        MageMLParser mlp = new MageMLParser();
        xslSetup( mlp, MAGE_DATA_RESOURCE_PATH + "E-WMIT-4.xml" );

        InputStream istMageExamples = MageMLParserTest.class.getResourceAsStream( MAGE_DATA_RESOURCE_PATH
                + "E-WMIT-4.xml" );

        MageMLConverter mageMLConverter = new MageMLConverter();
        mageMLConverter.addLocalExternalDataPath( FileTools.resourceToPath( "/resources" + MAGE_DATA_RESOURCE_PATH
                + "E-WMIT-4" ) );

        mlp.parse( istMageExamples );
        Collection<Object> parseResult = mlp.getResults();

        // getMageMLConverter().setSimplifiedXml( mlp.getSimplifiedXml() );

        Collection<Object> result = mageMLConverter.convert( parseResult );
        log.info( result.size() + " Objects parsed from the MAGE file." );
        if ( log.isDebugEnabled() ) log.debug( "Tally:\n" + mlp );
        istMageExamples.close();

        for ( Object object : result ) {
            if ( object instanceof ExpressionExperiment ) {
                ee = ( ExpressionExperiment ) object;
                ee.setName( RandomStringUtils.randomAlphabetic( 20 ) + "expressionExperiment" );

                /*
                 * FIXME the array design ends up having a null primaryTaxon.
                 */

                ee = ( ExpressionExperiment ) persisterHelper.persist( ee );
                assertNotNull( ee.getId() );
                assertEquals( 12, ee.getBioAssays().size() );

                for ( BioAssay bioAssay : ee.getBioAssays() ) {
                    assertNotNull( bioAssay.getId() );
                    for ( BioMaterial bioMaterial : bioAssay.getSamplesUsed() ) {
                        assertNotNull( bioMaterial.getId() );
                    }
                }

                break;
            }
        }

    }

}
