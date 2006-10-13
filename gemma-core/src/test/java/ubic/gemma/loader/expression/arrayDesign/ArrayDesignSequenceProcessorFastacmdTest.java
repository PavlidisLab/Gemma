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

import java.util.Collection;

import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.util.ConfigUtils;

/**
 * @author pavlidis
 * @version $Id$
 */
public class ArrayDesignSequenceProcessorFastacmdTest extends AbstractArrayDesignProcessingTest {

    ArrayDesignSequenceProcessingService app;

    @Override
    protected void onSetUp() throws Exception {
        super.onSetUp();
        app = ( ArrayDesignSequenceProcessingService ) getBean( "arrayDesignSequenceProcessingService" );

    }

    @SuppressWarnings("unchecked")
    public void testProcessArrayDesignWithFastaCmdFetch() throws Exception {
        try {
            // finally the real business. There are 243 sequences on the array, but one is not going to be found ()
            Collection<BioSequence> res = app.processArrayDesign( ad, new String[] { "testblastdb",
                    "testblastdbPartTwo" }, ConfigUtils.getString( "gemma.home" )
                    + "/gemma-core/src/test/resources/data/loader/genome/blast" );
            assertEquals( 242, res.size() );
        } catch ( IllegalStateException e ) {
            if ( e.getMessage().startsWith( "No fastacmd executable:" ) ) {
                return;
            }
        }

    }

}
