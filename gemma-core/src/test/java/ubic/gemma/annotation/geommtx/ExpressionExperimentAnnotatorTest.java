/*
 * The Gemma project
 * 
 * Copyright (c) 2009 University of British Columbia
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
package ubic.gemma.annotation.geommtx;

import static org.junit.Assert.*;

import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.ontology.OntologyService;
import ubic.gemma.testing.BaseSpringContextTest;
import ubic.gemma.util.ConfigUtils;

/**
 * @author paul
 * @version $Id$
 */
public class ExpressionExperimentAnnotatorTest extends BaseSpringContextTest {

    @Autowired
    ExpressionExperimentAnnotator expressionExperimentAnnotator;

    @Autowired
    OntologyService ontologyService;

    /*
     * For this test to work, at least birnlex must be loaded.
     */
    @Before
    public void setup() throws Exception {

        boolean activated = ConfigUtils.getBoolean( ExpressionExperimentAnnotator.MMTX_ACTIVATION_PROPERTY_KEY );
        if ( !activated ) {
            return;
        }

        ontologyService.getBirnLexOntologyService().startInitializationThread( true );

        while ( !ontologyService.getBirnLexOntologyService().isOntologyLoaded() ) {
            Thread.sleep( 5000 );
            log.info( ".... waiting for Birnlex..." );
        }

        while ( !ExpressionExperimentAnnotator.ready() ) {
            Thread.sleep( 5000 );
            log.info( ".... Waiting for MMTX ..." );
        }
        log.info( "Everybody is ready!" );
    }

    @Test
    public void testAnnotate() {
        boolean activated = ConfigUtils.getBoolean( ExpressionExperimentAnnotator.MMTX_ACTIVATION_PROPERTY_KEY );
        if ( !activated ) {
            log.warn( "MMTx is not available, skipping test" );
            return;
        }

        ExpressionExperiment ee = super.getTestPersistentBasicExpressionExperiment();

        ee.setName( "mouse brain leg kidney lung" );
        ee.setDescription( ee.getDescription()
                + " I have a brain, a leg, and a hippocampus. Please give me some asprin " );

        Collection<Characteristic> annotations = this.expressionExperimentAnnotator.annotate( ee, false );

        assertTrue( annotations.size() > 0 );

    }

}
