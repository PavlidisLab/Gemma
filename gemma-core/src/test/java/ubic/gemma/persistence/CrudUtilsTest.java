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

import org.apache.commons.collections.Predicate;

import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.biosequence.PolymerType;
import ubic.gemma.model.genome.biosequence.SequenceType;
import ubic.gemma.testing.BaseSpringContextTest;

;

/**
 * @author pavlidis
 * @version $Id$
 */
public class CrudUtilsTest extends BaseSpringContextTest {

    public final void testProcessAssociationsCreateWithCascade() throws Exception {
        CrudUtils c = ( CrudUtils ) getBean( "crudUtils" );
        assert c != null;
        ArrayDesign entity = getLittleArrayDesign();
        Predicate predicate = c.getCreatePredicate();

        entity = ( ArrayDesign ) c.invokeCreate( entity );
        c.processAssociations( entity, null, predicate, false );

        assertNotNull( entity.getId() );
        assertNotNull( entity.getCompositeSequences().iterator().next().getId() );

        // because we expect a cascade, this should not be filled in.
        assertNull( entity.getCompositeSequences().iterator().next().getBiologicalCharacteristic().getId() );

    }

    public final void testProcessAssociationsCreateWithCascadeBidirectionalNotFilledIn() throws Exception {
        CrudUtils c = ( CrudUtils ) getBean( "crudUtils" );
        assert c != null;
        ArrayDesign entity = getLittleArrayDesignBidirectionalNotFilledIn();
        try {
            entity = ( ArrayDesign ) c.invokeCreate( entity );
            fail( "Should get an exception, bidirectional association is not filled in" );
        } catch ( Exception expected ) {
            // expected
        }

    }

    public final void testProcessAssociationsIgnoreCascade() throws Exception {

        CrudUtils c = ( CrudUtils ) getBean( "crudUtils" );
        assert c != null;

        final StringBuilder sb = new StringBuilder();
        Predicate predicate = new Predicate() {
            public boolean evaluate( Object arg0 ) {
                sb.append( arg0.toString() + "\n" );
                return true;
            }
        };

        assert predicate != null;

        ArrayDesign entity = getLittleArrayDesign();

        c.processAssociations( entity, null, predicate, true );

        assertEquals( "CompositeSequenceImpl Name=bar\nBioSequenceImpl Name=fooby\n", sb.toString() );

    }

    /**
     * @return
     */
    private ArrayDesign getLittleArrayDesign() {
        ArrayDesign entity = ArrayDesign.Factory.newInstance();
        entity.setName( "foo" );
        CompositeSequence cs = CompositeSequence.Factory.newInstance( "bar", "aha", null, null, null, entity, null );
        BioSequence bs = BioSequence.Factory.newInstance( new Long( 10 ), "ATCGTCCC", Boolean.FALSE, Boolean.FALSE,
                PolymerType.DNA, SequenceType.AFFY_PROBE, "fooby", "ooby", null, null, null );
        cs.setBiologicalCharacteristic( bs );
        entity.getCompositeSequences().add( cs );
        return entity;
    }

    /**
     * @return
     */
    private ArrayDesign getLittleArrayDesignBidirectionalNotFilledIn() {
        ArrayDesign entity = ArrayDesign.Factory.newInstance();
        entity.setName( "foo" );
        CompositeSequence cs = CompositeSequence.Factory.newInstance( "bar", "aha", null, null, null, null, null );
        BioSequence bs = BioSequence.Factory.newInstance( new Long( 10 ), "ATCGTCCC", Boolean.FALSE, Boolean.FALSE,
                PolymerType.DNA, SequenceType.AFFY_PROBE, "fooby", "ooby", null, null, null );
        cs.setBiologicalCharacteristic( bs );
        entity.getCompositeSequences().add( cs );
        return entity;
    }
}
