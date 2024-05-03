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

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.annotation.DirtiesContext;
import ubic.gemma.core.genome.gene.service.GeneSetService;
import ubic.gemma.core.ontology.providers.GeneOntologyService;
import ubic.gemma.core.search.GeneSetSearch;
import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.model.association.GOEvidenceCode;
import ubic.gemma.model.association.Gene2GOAssociation;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.service.association.Gene2GOAssociationService;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.zip.GZIPInputStream;

import static org.junit.Assert.*;

/**
 * @author klc
 */
public class GeneSetServiceTest extends BaseSpringContextTest {

    static private final String GOTERM_INDB = "GO_0000310";
    static private final String GOTERM_QUERY = "GO:0000310";
    private Gene g = null;
    private Gene g3 = null;
    @Autowired
    private GeneSetService geneSetService;

    @Autowired
    private GeneOntologyService geneOntologyService;

    @Autowired
    private GeneSetSearch geneSetSearch;

    @Autowired
    private Gene2GOAssociationService gene2GoService;

    @Autowired
    private SessionFactory sessionFactory;

    @Before
    public void setUp() throws Exception {
        g = this.getTestPersistentGene();
        g3 = this.getTestPersistentGene();
    }

    @After
    public void tearDown() {
        geneSetService.removeAll();
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
    @DirtiesContext
    public void testFindByGoId() throws IOException {
        InputStream is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/ontology/molecular-function.test.owl.gz" ).getInputStream() );
        geneOntologyService.initialize( is, false );

        Characteristic oe = Characteristic.Factory.newInstance();
        oe.setValueUri( GeneOntologyService.BASE_GO_URI + GeneSetServiceTest.GOTERM_INDB );
        oe.setValue( GeneSetServiceTest.GOTERM_INDB );
        Gene2GOAssociation g2Go1 = Gene2GOAssociation.Factory.newInstance( g, oe, GOEvidenceCode.EXP );

        gene2GoService.create( g2Go1 );

        oe = Characteristic.Factory.newInstance();
        oe.setValueUri( GeneOntologyService.BASE_GO_URI + GeneSetServiceTest.GOTERM_INDB );
        oe.setValue( GeneSetServiceTest.GOTERM_INDB );
        Gene2GOAssociation g2Go2 = Gene2GOAssociation.Factory.newInstance( g3, oe, GOEvidenceCode.EXP );

        gene2GoService.create( g2Go2 );

        GeneSet gset = this.geneSetSearch.findByGoId( GeneSetServiceTest.GOTERM_QUERY, g3.getTaxon() );
        assertNotNull( gset );
        assertEquals( 2, gset.getMembers().size() );
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

        Session session = sessionFactory.openSession();
        try {
            session.update( gset );
            gmember = gset.getMembers().iterator().next();
            assertNotNull( gmember.getId() );
        } finally {
            session.close();
        }

        // add one.
        gset = geneSetService.loadOrFail( gset.getId() );

        // make sure members collection is initialized
        session = sessionFactory.openSession();
        try {
            session.update( gset );
            Hibernate.initialize( gset.getMembers() );
        } finally {
            session.close();
        }

        gmember = GeneSetMember.Factory.newInstance();
        gmember.setGene( this.g3 );
        gmember.setScore( 0.66 );

        gset.getMembers().add( gmember );
        assertEquals( 2, gset.getMembers().size() );

        // persist.
        geneSetService.update( gset );

        // check
        gset = geneSetService.loadOrFail( gset.getId() );

        // make sure members collection is initialized
        session = sessionFactory.openSession();
        try {
            session.update( gset );
            Hibernate.initialize( gset.getMembers() );
        } finally {
            session.close();
        }

        assertEquals( 2, gset.getMembers().size() );

        // remove one
        gset.getMembers().remove( gmember );
        geneSetService.update( gset );

        // check
        gset = geneSetService.loadOrFail( gset.getId() );
        // make sure members collection is initialized
        session = sessionFactory.openSession();
        try {
            session.update( gset );
            Hibernate.initialize( gset.getMembers() );
        } finally {
            session.close();
        }

        assertEquals( 1, gset.getMembers().size() );

        // clean
        geneSetService.remove( gset );
        assertNull( geneSetService.load( gset.getId() ) );

    }

    @Test
    public void testLoadValueObject() {
        GeneSet gset = GeneSet.Factory.newInstance();
        gset.getMembers().add( GeneSetMember.Factory.newInstance( 1.0, g ) );
        gset = geneSetService.create( gset );
        assertNotNull( gset.getId() );
        assertEquals( 1, gset.getMembers().size() );
        DatabaseBackedGeneSetValueObject vo = geneSetService.loadValueObject( gset );
        assertNotNull( vo );
        assertNotNull( vo.getGeneIds() );
        assertEquals( 1, vo.getGeneIds().size() );
        assertNotNull( geneSetService.loadValueObjectById( gset.getId() ) );
        assertEquals( 1, geneSetService.loadAllValueObjects().size() );
    }

    @Test
    public void testLoadValueObjectLite() {
        GeneSet gset = GeneSet.Factory.newInstance();
        gset.getMembers().add( GeneSetMember.Factory.newInstance( 1.0, g ) );
        gset = geneSetService.create( gset );
        assertNotNull( gset.getId() );
        DatabaseBackedGeneSetValueObject vo = geneSetService.loadValueObjectByIdLite( gset.getId() );
        assertNotNull( vo );
        assertNull( vo.getGeneIds() );
        assertNotNull( geneSetService.loadValueObjectByIdLite( gset.getId() ) );
        assertEquals( 1, geneSetService.loadValueObjectsByIdsLite( Collections.singleton( gset.getId() ) ).size() );
    }
}
