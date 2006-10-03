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

import junit.framework.TestCase;

/**
 * @author pavlidis
 * @version $Id$
 */
public class OntologyEntryImplTest extends TestCase {

    OntologyEntry top;
    OntologyEntry middle;
    OntologyEntry child;
    OntologyEntry childsChild;

    /*
     * (non-Javadoc)
     * 
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        // this is our top level one
        top = OntologyEntry.Factory.newInstance();
        top.setId( 1L );
        middle = OntologyEntry.Factory.newInstance();
        middle.setId( 2L );
        top.getAssociations().add( middle );
        child = OntologyEntry.Factory.newInstance();
        child.setId( 3L );
        middle.getAssociations().add( child );
        childsChild = OntologyEntry.Factory.newInstance();
        childsChild.setId( 3L );
        child.getAssociations().add( childsChild );
    }

    /*
     * (non-Javadoc)
     * 
     * @see junit.framework.TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for {@link ubic.gemma.model.common.description.OntologyEntryImpl#getParents()}.
     */
    public final void testGetParents() {
        OntologyEntry actualValue = ( OntologyEntry ) child.getParents().iterator().next();
        assertEquals( middle, actualValue );
    }

    /**
     * Test method for {@link ubic.gemma.model.common.description.OntologyEntryImpl#getChildren()}.
     */
    @SuppressWarnings("unchecked")
    public final void testGetChildren() {
        Collection<OntologyEntry> actualValue = top.getChildren();
        assertEquals( 3, actualValue.size() );
    }

}
