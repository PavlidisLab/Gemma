/*
 * The Gemma project
 * 
 * Copyright (c) 2007 Columbia University
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
package ubic.gemma.analysis.ontology;

import java.util.Collection;

import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.ExternalDatabaseService;
import ubic.gemma.model.common.description.OntologyEntry;
import ubic.gemma.model.common.description.OntologyEntryService;
import ubic.gemma.persistence.PersisterHelper;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author Paul
 * @version $Id$
 */
public class GeneOntologyServiceTest extends BaseSpringContextTest {

    ExternalDatabase go = null;
    OntologyEntry root;
    GeneOntologyService gos;
    OntologyEntry one;
    OntologyEntry two;
    OntologyEntry three;
    PersisterHelper ph;

    @Override
    protected void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();
        endTransaction();

        // populate the DB with a mini-GO.
        go = ( ( ExternalDatabaseService ) this.getBean( "externalDatabaseService" ) ).find( "GO" );
        assert go != null;
        ph = ( PersisterHelper ) this.getBean( "persisterHelper" );

        root = make( "all" );
        one = make( "1" );
        two = make( "2" );
        three = make( "3" );

        OntologyEntryService oes = ( OntologyEntryService ) this.getBean( "ontologyEntryService" );
        oes.thaw( root );
        oes.thaw( one );
        oes.thaw( two );
        oes.thaw( three );

        root.getAssociations().add( one );
        one.getAssociations().add( two );
        two.getAssociations().add( three );

        oes.update( two );
        oes.update( one );
        oes.update( root );

        gos = ( GeneOntologyService ) this.getBean( "geneOntologyService" );
        flushAndClearSession();

        gos.init();
        Thread.sleep( 100 );
    }

    private OntologyEntry make( String name ) {
        OntologyEntry newOe = OntologyEntry.Factory.newInstance( go );
        newOe.setCategory( "whatever" );
        newOe.setAccession( name );
        newOe.setValue( name );
        return ( OntologyEntry ) ph.persist( newOe );
    }

    @Override
    protected void onTearDownInTransaction() throws Exception {
        super.onTearDownInTransaction();
        OntologyEntryService oes = ( OntologyEntryService ) this.getBean( "ontologyEntryService" );
        two.getAssociations().clear();
        one.getAssociations().clear();
        root.getAssociations().clear();
        oes.update( two );
        oes.update( one );
        oes.update( root );
        if ( three != null ) oes.remove( three );
        if ( two != null ) oes.remove( two );
        if ( one != null ) oes.remove( one );
        if ( root != null ) oes.remove( root );
        flushAndClearSession();
    }

    /**
     * Test method for
     * {@link ubic.gemma.analysis.ontology.GeneOntologyService#getParents(ubic.gemma.model.common.description.OntologyEntry)}.
     */
    public void testGetParents() {
        int actualValue = gos.getParents( two ).size();
        assertEquals( 1, actualValue );
    }

    /**
     * Test method for
     * {@link ubic.gemma.analysis.ontology.GeneOntologyService#getAllParents(ubic.gemma.model.common.description.OntologyEntry)}.
     */
    public void testGetAllParents() {
        int actualValue = gos.getAllParents( three ).size();
        assertEquals( 2, actualValue );
    }

    /**
     * Test method for
     * {@link ubic.gemma.analysis.ontology.GeneOntologyService#getAllChildren(ubic.gemma.model.common.description.OntologyEntry)}.
     */
    public void testGetAllChildren() {
        Collection<OntologyEntry> actualValue = gos.getAllChildren( root );
        assertEquals( actualValue.toString(), 3, actualValue.size() );
    }

    /**
     * Test method for
     * {@link ubic.gemma.analysis.ontology.GeneOntologyService#isAParentOf(ubic.gemma.model.common.description.OntologyEntry, ubic.gemma.model.common.description.OntologyEntry)}.
     */
    public void testIsAParentOf() {
        Boolean actualValue = gos.isAParentOf( two, one );
        assertTrue( actualValue );
    }

    /**
     * Test method for
     * {@link ubic.gemma.analysis.ontology.GeneOntologyService#isAChildOf(ubic.gemma.model.common.description.OntologyEntry, ubic.gemma.model.common.description.OntologyEntry)}.
     */
    public void testIsAChildOf() {
        boolean actualValue = gos.isAChildOf( root, three );
        assertTrue( actualValue );
    }

    /**
     * Test method for
     * {@link ubic.gemma.analysis.ontology.GeneOntologyService#getChildren(ubic.gemma.model.common.description.OntologyEntry)}.
     */
    public void testGetChildren() {
        int actualValue = gos.getChildren( one ).size();
        assertEquals( 1, actualValue );
    }

}
