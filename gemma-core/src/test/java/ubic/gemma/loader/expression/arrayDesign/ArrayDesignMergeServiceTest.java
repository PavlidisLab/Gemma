/*
 * The Gemma project
 * 
 * Copyright (c) 2011 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.loader.expression.arrayDesign;

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author paul
 * @version $Id$
 */
public class ArrayDesignMergeServiceTest extends BaseSpringContextTest {

    @Autowired
    ArrayDesignMergeService arrayDesignMergeService;

    @Autowired
    ArrayDesignService arrayDesignService;

    @Test
    public void testMerge() {
        ArrayDesign ad1 = super.getTestPersistentArrayDesign( 10, true );
        ArrayDesign ad2 = super.getTestPersistentArrayDesign( 10, true );
        ArrayDesign ad3 = super.getTestPersistentArrayDesign( 10, true );

        Collection<ArrayDesign> others = new HashSet<ArrayDesign>();
        others.add( ad2 );
        others.add( ad3 );
        ArrayDesign merged = arrayDesignService.thaw( arrayDesignMergeService.merge( ad1, others, "foo1", "bar1"
                + RandomStringUtils.randomAlphabetic( 4 ), false ) );

        assertTrue( merged.getMergees().contains( ad2 ) );
        assertTrue( merged.getMergees().contains( ad3 ) );
        assertNull( merged.getMergedInto() );
        assertTrue( merged.getCompositeSequences().size() == 30 );
        ad3 = arrayDesignService.thawLite( arrayDesignService.load( ad3.getId() ) );
        assertEquals( merged, ad3.getMergedInto() );

        /*
         * Making a new one out of a merged design and an unmerged
         */
        ArrayDesign ad4 = super.getTestPersistentArrayDesign( 10, true );
        others.clear();
        others.add( ad4 );
        ArrayDesign merged2 = arrayDesignService.thaw( arrayDesignMergeService.merge( merged, others, "foo2", "bar2"
                + RandomStringUtils.randomAlphabetic( 4 ), false ) );
        assertTrue( merged2.getMergees().contains( ad4 ) );
        assertTrue( merged2.getMergees().contains( merged ) );
        assertTrue( merged2.getCompositeSequences().size() == 40 );
        assertNull( merged2.getMergedInto() );

        /*
         * Add an array to an already merged design.
         */
        ArrayDesign ad5 = super.getTestPersistentArrayDesign( 10, true );
        others.clear();
        others.add( ad5 );
        ArrayDesign merged3 = arrayDesignService.thaw( arrayDesignMergeService
                .merge( merged2, others, null, null, true ) );

        assertEquals( merged2, merged3 );
        assertTrue( merged3.getMergees().contains( ad5 ) );
        assertTrue( merged3.getMergees().contains( ad4 ) );
        assertTrue( merged3.getMergees().contains( merged ) );
        assertTrue( merged3.getCompositeSequences().size() == 50 );
    }

}
