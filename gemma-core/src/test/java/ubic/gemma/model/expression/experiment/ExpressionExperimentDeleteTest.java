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

package ubic.gemma.model.expression.experiment;

import org.hibernate.Session;
import org.hibernate.SessionFactory;

import ubic.gemma.model.common.description.ExternalDatabaseService;
import ubic.gemma.persistence.PersisterHelper;
import ubic.gemma.testing.BaseSpringContextTest;
import ubic.gemma.testing.TestPersistentObjectHelper;

/**
 * @author kelsey
 * @version $Id$
 */
public class ExpressionExperimentDeleteTest extends BaseSpringContextTest {

    private PersisterHelper persisterHelper;
    private ExternalDatabaseService externalDatabaseService;
    private ExpressionExperimentService svc;

    public void testExpressionExperimentDelete() {

        SessionFactory sessf = ( SessionFactory ) this.getBean( "sessionFactory" );
        Session sess = sessf.openSession();
        sess.beginTransaction();

        TestPersistentObjectHelper helper = new TestPersistentObjectHelper();
        helper.setPersisterHelper( this.persisterHelper );
        helper.setExternalDatabaseService( externalDatabaseService );

        ExpressionExperiment ee = helper.getTestExpressionExperimentWithAllDependencies();

        assertNotNull( svc.load( ee.getId() ) );
        svc.delete( ee );
        assertEquals( svc.load( ee.getId() ), null );

        // designElementDataVectors.size(); // lazy-load...
        sess.getTransaction().commit();
        sess.evict( ee );
        sess.close();

    }

    /**
     * @param externalDatabaseService the externalDatabaseService to set
     */
    public void setExternalDatabaseService( ExternalDatabaseService externalDatabaseService ) {
        this.externalDatabaseService = externalDatabaseService;
    }

    /**
     * @param persisterHelper the persisterHelper to set
     */
    @Override
    public void setPersisterHelper( PersisterHelper persisterHelper ) {
        this.persisterHelper = persisterHelper;
    }

    /**
     * @param expressionExperimentService
     */
    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.svc = expressionExperimentService;
    }
}
