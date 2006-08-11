/*
 * The Gemma project
 * 
 * Copyright (c) 2006 Columbia University
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
package ubic.gemma.model.expression.arrayDesign;

import java.util.Collection;

import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * Tests the CompositeSequenceService.
 * 
 * @author keshav
 * @version $Id$
 */
public class CompositeSequenceServiceImplTest extends BaseSpringContextTest {

    CompositeSequenceService compositeSequenceService = null;
    CompositeSequence cs = null;

    /**
     * Asserts the true if getAssociatedGenes returns null when there are no composite sequences associated with this
     * gene.
     */
    public void testHandleGetAssociatedGenes() {// TODO add mocks
        compositeSequenceService = ( CompositeSequenceService ) this.getBean( "compositeSequenceService" );

        cs = CompositeSequence.Factory.newInstance();
        Collection genes = compositeSequenceService.getAssociatedGenes( cs );

        assertTrue( genes == null );
    }
}
