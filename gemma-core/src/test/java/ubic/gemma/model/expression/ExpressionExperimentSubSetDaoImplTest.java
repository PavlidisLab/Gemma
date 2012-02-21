/*
 * The Gemma project
 * 
 * Copyright (c) 2010 University of British Columbia
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
package ubic.gemma.model.expression;

import static org.junit.Assert.*;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSetDao;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author paul
 * @version $Id$
 */
public class ExpressionExperimentSubSetDaoImplTest extends BaseSpringContextTest {

    @Autowired
    private ExpressionExperimentSubSetDao expressionExperimentSubSetDao;

    @Autowired
    SessionFactory sessionFactory;
    
    @Test
    public final void testFind() throws Exception {

        ExpressionExperiment ee = super.testHelper.getTestPersistentBasicExpressionExperiment();

        ExpressionExperimentSubSet subset = ExpressionExperimentSubSet.Factory.newInstance();

        subset.setSourceExperiment( ee );
        subset.getBioAssays().addAll( ee.getBioAssays() );
        subset.setName( "foo" );
        
       // Transaction tx = session.beginTransaction();
        ExpressionExperimentSubSet persisted = expressionExperimentSubSetDao.create( subset );
       // tx.commit();

        assertNotNull( persisted );
                
        
        Session session = sessionFactory.openSession();
        session.update( persisted );
        
        ExpressionExperimentSubSet hit = expressionExperimentSubSetDao.find( persisted );
        
        session.close();
        
        
        //Session session = this.hibernateSupport.getHibernateTemplate().getSessionFactory().openSession();
        //Transaction tx = session.beginTransaction();
        //tx.commit();

        assertEquals( persisted, hit );

    }

}
