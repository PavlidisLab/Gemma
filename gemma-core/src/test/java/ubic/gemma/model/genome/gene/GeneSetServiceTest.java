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
package ubic.gemma.model.genome.gene;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.model.genome.Gene;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author klc
 * @version $Id$
 */
public class GeneSetServiceTest extends BaseSpringContextTest {

    private Gene g = null;
    private Gene g2 = null;
    private Gene g3 = null;

    @Autowired
    GeneSetService geneSetService;

    @Before
    public void setUp() throws Exception {
        g = this.getTestPeristentGene();
        g2 = this.getTestPeristentGene();
        g3 = this.getTestPeristentGene();
    }

    @Test
    public void testCreate() {

        Collection<GeneSetMember> gsMembers = new HashSet<GeneSetMember>();
        GeneSetMember gmember = new GeneSetMemberImpl();
        gmember.setGene( g );
        gmember.setScore( 0.22 );

        gsMembers.add( gmember );

        GeneSet gset = new GeneSetImpl();
        gset.setName( "CreateTest" );
        gset.setGeneSetMembers( gsMembers );

        gset = geneSetService.create( gset );

        assert ( gset.equals( geneSetService.load( gset.getId() ).getId() ) );

        geneSetService.remove( gset );
    }

    @Test
    public void testRemove() {

        Collection<GeneSetMember> gsMembers = new HashSet<GeneSetMember>();
        GeneSetMember gmember = new GeneSetMemberImpl();
        gmember.setGene( this.g2 );
        gmember.setScore( 0.33 );

        gsMembers.add( gmember );

        GeneSet gset = new GeneSetImpl();
        gset.setName( "DeleteTest" );
        gset.setGeneSetMembers( gsMembers );

        gset = geneSetService.create( gset );

        assert ( gset.equals( geneSetService.load( gset.getId() ).getId() ) );

        geneSetService.remove( gset );

        assert ( geneSetService.load( gset.getId() ) == null );

    }

    @Test
    public void testUpdate() {

        Collection<GeneSetMember> gsMembers = new HashSet<GeneSetMember>();
        GeneSetMember gmember = new GeneSetMemberImpl();
        gmember.setGene( this.g2 );
        gmember.setScore( 0.33 );

        gsMembers.add( gmember );

        GeneSet gset = new GeneSetImpl();
        gset.setName( "Update Test" );
        gset.setGeneSetMembers( gsMembers );

        gset = geneSetService.create( gset );

        assert ( gset.equals( geneSetService.load( gset.getId() ).getId() ) );

        gmember = new GeneSetMemberImpl();
        gmember.setGene( this.g3 );
        gmember.setScore( 0.66 );
        gset.getGeneSetMembers().add( gmember );

        assert ( geneSetService.load( gset.getId() ).getGeneSetMembers().size() == 1 );

        geneSetService.update( gset );

        assert ( geneSetService.load( gset.getId() ).getGeneSetMembers().size() == 2 );

        geneSetService.remove( gset );

    }

    // FIXME I thought this test would fail but it passes. Our API lets us add the same geneMember to the same set
    // twice.
    // Thought the HashSet would reject this.

    @Test
    public void testUpdateAddingSameGeneMemberTwice() {

        GeneSetMember gmember = GeneSetMember.Factory.newInstance();
        gmember.setGene( this.g2 );
        gmember.setScore( 0.33 );

        GeneSet gset = GeneSet.Factory.newInstance();
        gset.setName( "testUpdateAddingSameGeneMemberTwice" );
        gset.getGeneSetMembers().add( gmember );

        gset = geneSetService.create( gset );

        assertNotNull( gset.getId() );
        assertEquals( 1, gset.getGeneSetMembers().size() );

        GeneSetMember persistedGeneSetMember = gset.getGeneSetMembers().iterator().next();

        assertNotNull( persistedGeneSetMember.getId() );

        Set<GeneSetMember> k = new HashSet<GeneSetMember>();
        k.add( persistedGeneSetMember );
        k.add( persistedGeneSetMember );
        assertEquals( 1, k.size() );

        assertNotNull( persistedGeneSetMember.getId() );

        assertEquals( gset.getGeneSetMembers().iterator().next(), persistedGeneSetMember );

        assertTrue( gset.getGeneSetMembers().contains( persistedGeneSetMember ) );

        // add it again.
        gset.getGeneSetMembers().add( persistedGeneSetMember );

        assertEquals( 1, gset.getGeneSetMembers().size() );

        geneSetService.update( gset );

        assertEquals( 1, gset.getGeneSetMembers().size() );

        geneSetService.remove( gset );

    }

}
