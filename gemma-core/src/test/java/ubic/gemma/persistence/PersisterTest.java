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
package ubic.gemma.persistence;

import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.testing.BaseSpringContextTest;
import ubic.gemma.testing.TestPersistentObjectHelper;

/**
 * @author pavlidis
 * @version $Id$
 */
public class PersisterTest extends BaseSpringContextTest {
    ArrayDesign ad;
    TestPersistentObjectHelper helper;
    PersisterHelper persisterHelper;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.testing.BaseSpringContextTest#onSetUp()
     */
    @Override
    protected void onSetUp() throws Exception {
        super.onSetUp();
        persisterHelper = ( PersisterHelper ) this.getBean( "persisterHelper" );
        helper = new TestPersistentObjectHelper();
        helper.setPersisterHelper( persisterHelper );
    }

    /**
     *  
     */
    public void testPersistNewArrayDesign() throws Exception {

        ad = helper.getTestPersistentArrayDesign( 20, true );
        assertNotNull( ad.getId() );
    }

    /**
     * @throws Exception
     */
    // public void testPersistBioSequences() throws Exception {
    // Collection<BioSequence> seqs = new HashSet<BioSequence>();
    // for ( int i = 0; i < 2000; i++ ) {
    // seqs.add( helper.getTestNonPersistentBioSequence() );
    // }
    //
    // persisterHelper.persist( seqs );
    //
    // }
    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.test.AbstractDependencyInjectionSpringContextTests#onTearDown()
     */
    @Override
    protected void onTearDown() throws Exception {
        if ( ad != null && ad.getId() != null ) {
            ArrayDesignService ads = ( ArrayDesignService ) this.getBean( "arrayDesignService" );
            ads.remove( ad );
        }
    }

}
