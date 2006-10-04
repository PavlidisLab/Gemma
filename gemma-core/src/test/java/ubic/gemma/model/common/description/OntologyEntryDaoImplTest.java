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
package ubic.gemma.model.common.description;

import java.util.Collection;

import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author pavlidis
 * @version $Id$
 */
public class OntologyEntryDaoImplTest extends BaseSpringContextTest {

    OntologyEntry top;
    OntologyEntry middle;
    OntologyEntry child;
    OntologyEntry childsChild;

    OntologyEntryDao oed;
    ExternalDatabaseDao edd;
    ExternalDatabase ed = ExternalDatabase.Factory.newInstance();

    /*
     * (non-Javadoc)
     * 
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void onSetUp() throws Exception {
        super.onSetUp();

        oed = ( OntologyEntryDao ) this.getBean( "ontologyEntryDao" );
        edd = ( ExternalDatabaseDao ) this.getBean( "externalDatabaseDao" );

        ed.setName( "foo" );

        ed = ( ExternalDatabase ) edd.create( ed );

        // this is our top level one
        top = OntologyEntry.Factory.newInstance();
        top.setAccession( "fred" );
        top.setExternalDatabase( ed );
        middle = OntologyEntry.Factory.newInstance();
        middle.setAccession( "mary" );
        middle.setExternalDatabase( ed );
        child = OntologyEntry.Factory.newInstance();
        child.setAccession( "jeff" );
        child.setExternalDatabase( ed );
        childsChild = OntologyEntry.Factory.newInstance();
        childsChild.setAccession( "jane" );
        childsChild.setExternalDatabase( ed );

        childsChild = ( OntologyEntry ) oed.create( childsChild );
        child.getAssociations().add( childsChild );
        child = ( OntologyEntry ) oed.create( child );
        middle.getAssociations().add( child );
        middle = ( OntologyEntry ) oed.create( middle );
        top.getAssociations().add( middle );
        top = ( OntologyEntry ) oed.create( top );

    }

    @Override
    protected void onTearDown() throws Exception {
        super.onTearDown();
        oed.remove( top );
        oed.remove( middle );
        oed.remove( child );
        oed.remove( childsChild );
        edd.remove( ed );
    }

    /**
     * Test method for {@link ubic.gemma.model.common.description.OntologyEntryImpl#getParents()}.
     */
    public final void testGetParents() {
        OntologyEntry actualValue = ( OntologyEntry ) oed.getParents( child ).iterator().next();
        assertEquals( middle.getAccession(), actualValue.getAccession() );
    }

    /**
     * Test method for {@link ubic.gemma.model.common.description.OntologyEntryImpl#getChildren()}.
     */
    @SuppressWarnings("unchecked")
    public final void testGetChildren() {
        Collection<OntologyEntry> actualValue = oed.getAllChildren( top );
        assertEquals( 3, actualValue.size() );
    }

}
