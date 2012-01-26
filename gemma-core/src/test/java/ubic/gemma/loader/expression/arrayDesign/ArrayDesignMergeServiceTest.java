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

import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignMergeEventImpl;
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

        ArrayDesign ad1ad2ad3 = arrayDesignMergeService.merge( ad1, others,
                "ad1ad2ad3_" + RandomStringUtils.randomAlphabetic( 4 ),
                "ad1ad2ad3_" + RandomStringUtils.randomAlphabetic( 4 ), false );

        /*
         * merged contains all three.
         */
        assertTrue( ad1ad2ad3.getMergees().contains( ad1 ) );
        assertTrue( ad1ad2ad3.getMergees().contains( ad2 ) );
        assertTrue( ad1ad2ad3.getMergees().contains( ad3 ) );
        assertNull( ad1ad2ad3.getMergedInto() );
        assertEquals( 30, ad1ad2ad3.getCompositeSequences().size() );
        assertEquals( ad1ad2ad3, ad1.getMergedInto() );
        assertEquals( ad1ad2ad3, ad2.getMergedInto() );
        assertEquals( ad1ad2ad3, ad3.getMergedInto() );
        assertEquals( ArrayDesignMergeEventImpl.class, ad1.getAuditTrail().getLast().getEventType().getClass() );
        assertEquals( ArrayDesignMergeEventImpl.class, ad2.getAuditTrail().getLast().getEventType().getClass() );
        assertEquals( ArrayDesignMergeEventImpl.class, ad3.getAuditTrail().getLast().getEventType().getClass() );

        /*
         * Making a new one out of a merged design and an unmerged
         */
        ArrayDesign ad4 = super.getTestPersistentArrayDesign( 10, true );
        others.clear();
        others.add( ad4 );
        ArrayDesign ad1ad2ad3ad4 = arrayDesignMergeService.merge( ad1ad2ad3, others,
                "foo2" + RandomStringUtils.randomAlphabetic( 4 ), "bar2" + RandomStringUtils.randomAlphabetic( 4 ),
                false );

        assertTrue( ad1ad2ad3ad4.getMergees().contains( ad1ad2ad3 ) );
        assertTrue( ad1ad2ad3ad4.getMergees().contains( ad4 ) );
        assertEquals( 40, ad1ad2ad3ad4.getCompositeSequences().size() );
        assertNull( ad1ad2ad3ad4.getMergedInto() );

        /*
         * Add an array to an already merged design.
         */
        ArrayDesign ad5 = super.getTestPersistentArrayDesign( 10, true );
        others.clear();
        others.add( ad5 );
        ArrayDesign merged3 = arrayDesignMergeService.merge( ad1ad2ad3ad4, others, null, null, true );

        assertEquals( ad1ad2ad3ad4, merged3 );
        assertTrue( merged3.getMergees().contains( ad1ad2ad3 ) ); // from before.
        assertTrue( merged3.getMergees().contains( ad4 ) ); // from before
        assertTrue( merged3.getMergees().contains( ad5 ) ); // the extra one.
        assertTrue( merged3.getMergees().contains( ad1ad2ad3 ) );
        assertEquals( 50, merged3.getCompositeSequences().size() );

    }

}
