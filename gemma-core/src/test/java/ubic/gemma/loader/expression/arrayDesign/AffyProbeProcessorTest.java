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
package ubic.gemma.loader.expression.arrayDesign;

import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;

import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.DesignElement;

import junit.framework.TestCase;

/**
 * @author pavlidis
 * @version $Id$
 */
public class AffyProbeProcessorTest extends TestCase {

    Collection<? extends DesignElement> designElements = new HashSet<DesignElement>();
    InputStream seqFile;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        InputStream designElementStream = this.getClass().getResourceAsStream(
                "/data/loader/expression/arrayDesign/MG-U74A.txt" );

        CompositeSequenceParser parser = new CompositeSequenceParser();
        parser.parse( designElementStream );
        designElements = parser.getResults();

        seqFile = this.getClass().getResourceAsStream( "/data/loader/expression/arrayDesign/MG-U74A_target" );
    }

    public void testAssignSequencesToDesignElements() throws Exception {
        AffyProbeProcessor app = new AffyProbeProcessor();
        app.assignSequencesToDesignElements( designElements, seqFile );

        for ( DesignElement de : designElements ) {
            assertTrue( ( ( CompositeSequence ) de ).getBiologicalCharacteristic() != null );
        }

    }

    public void testAssignSequencesToDesignElementsMissingSequence() throws Exception {
        AffyProbeProcessor app = new AffyProbeProcessor();

        CompositeSequence doesntExist = CompositeSequence.Factory.newInstance();
        String fakeName = "I'm not real";
        doesntExist.setName( fakeName );

        app.assignSequencesToDesignElements( designElements, seqFile );

        boolean found = false;
        for ( DesignElement de : designElements ) {

            if ( de.getName().equals( fakeName ) && ( ( CompositeSequence ) de ).getBiologicalCharacteristic() != null ) {
                fail( "Shouldn't have found a biological characteristic for this sequence" );
                found = true;
            }

            assertTrue( ( ( CompositeSequence ) de ).getBiologicalCharacteristic() != null );
        }

        assertTrue( found ); // sanity check.

    }
}
