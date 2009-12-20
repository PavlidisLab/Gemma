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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;

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
    private Gene g3 = null;

    @Autowired
    GeneSetService geneSetService;

    @Before
    public void setUp() throws Exception {
        g = this.getTestPeristentGene();
        g3 = this.getTestPeristentGene();
    }

    /**
     * Test of cascade create
     */
    @Test
    public void testCreateLoadAndRemove() {

        GeneSetMember gmember = GeneSetMember.Factory.newInstance();
        gmember.setGene( g );
        gmember.setScore( 0.22 );

        GeneSet gset = GeneSet.Factory.newInstance();
        gset.setName( "CreateTest" );
        gset.getMembers().add( gmember );

        gset = geneSetService.create( gset );

        Long id = gset.getId();
        assertNotNull( id );

        geneSetService.remove( gset );

        assertNull( geneSetService.load( id ) );

    }

    /**
     * 
     */
    @Test
    public void testFindByGene() {

        GeneSetMember gmember = GeneSetMember.Factory.newInstance();
        gmember.setGene( g );
        gmember.setScore( 0.22 );

        GeneSet gset = GeneSet.Factory.newInstance();
        gset.setName( "FindTest" );
        gset.getMembers().add( gmember );

        gset = geneSetService.create( gset );
        assertNotNull( gset.getId() );
        assertNotNull( gset.getMembers().iterator().next().getId() );

        assertEquals( g, gset.getMembers().iterator().next().getGene() );

        Collection<GeneSet> foundSets = geneSetService.findByGene( g );
        assertTrue( foundSets.size() > 0 );
    }

    /**
     * Test adding a new member via a cascade.
     */
    @Test
    public void testUpdate() {

        GeneSetMember gmember = GeneSetMember.Factory.newInstance();
        gmember.setGene( g );
        gmember.setScore( 0.22 );

        GeneSet gset = GeneSet.Factory.newInstance();
        gset.setName( "UpdateTest" );
        gset.getMembers().add( gmember );

        gset = geneSetService.create( gset );
        assertNotNull( gset.getId() );
        gmember = gset.getMembers().iterator().next();
        assertNotNull( gmember.getId() );

        // add one.
        gset = geneSetService.load( gset.getId() );
        gmember = GeneSetMember.Factory.newInstance();
        gmember.setGene( this.g3 );
        gmember.setScore( 0.66 );
        gset.getMembers().add( gmember );

        assertEquals( 2, gset.getMembers().size() );

        // persist.
        geneSetService.update( gset );

        // check
        gset = geneSetService.load( gset.getId() );
        assertEquals( 2, gset.getMembers().size() );

        // remove one
        gset.getMembers().remove( gmember );
        geneSetService.update( gset );

        // check
        gset = geneSetService.load( gset.getId() );
        assertEquals( 1, gset.getMembers().size() );

        // clean
        geneSetService.remove( gset );
        assertNull( geneSetService.load( gset.getId() ) );

    }

}
