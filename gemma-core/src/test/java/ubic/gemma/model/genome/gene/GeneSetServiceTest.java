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

import java.io.InputStream;
import java.util.Collection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.genome.gene.service.GeneSetService;
import ubic.gemma.model.association.Gene2GOAssociation;
import ubic.gemma.model.association.Gene2GOAssociationService;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.ontology.providers.GeneOntologyService;
import ubic.gemma.search.GeneSetSearch;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author klc
 * @version $Id$
 */
public class GeneSetServiceTest extends BaseSpringContextTest {

    private Gene g = null;
    private Gene g3 = null;

    static private final String GOTERM_INDB = "GO_0000310";
    static private final String GOTERM_QUERY = "GO:0000310";

    @Autowired
    GeneSetService geneSetService;

    @Autowired
    GeneOntologyService geneOntologyService;

    @Autowired
    GeneSetSearch geneSetSearch;

    @Autowired
    Gene2GOAssociationService gene2GoService;

    @Before
    public void setUp() throws Exception {

        InputStream is = this.getClass().getResourceAsStream( "/data/loader/ontology/molecular-function.test.owl" );
        assert is != null;
        geneOntologyService.loadTermsInNameSpace( is );

        g = this.getTestPeristentGene();
        g3 = this.getTestPeristentGene();

    }

    @After
    public void tearDown() throws Exception {

        gene2GoService.removeAll();
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

    @Test
    public void testFindByName() {
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

        Collection<GeneSet> foundSets = geneSetService.findByName( "Find" );
        assertTrue( foundSets.size() > 0 );

        assertTrue( geneSetService.findByName( "Find", g.getTaxon() ).size() > 0 );

    }

    @Test
    public void testFindByGoId() {

        Gene2GOAssociation g2Go1 = Gene2GOAssociation.Factory.newInstance();
        VocabCharacteristic oe = VocabCharacteristic.Factory.newInstance();
        oe.setValueUri( GeneOntologyService.BASE_GO_URI + GOTERM_INDB );
        oe.setValue( GOTERM_INDB );
        g2Go1.setOntologyEntry( oe );
        g2Go1.setGene( g );
        gene2GoService.create( g2Go1 );

        Gene2GOAssociation g2Go2 = Gene2GOAssociation.Factory.newInstance();
        oe = VocabCharacteristic.Factory.newInstance();
        oe.setValueUri( GeneOntologyService.BASE_GO_URI + GOTERM_INDB );
        oe.setValue( GOTERM_INDB );
        g2Go2.setOntologyEntry( oe );
        g2Go2.setGene( g3 );
        gene2GoService.create( g2Go2 );

        GeneSet gset = this.geneSetSearch.findByGoId( GOTERM_QUERY, g3.getTaxon() );
        assertTrue( gset.getMembers().size() == 2 );
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
