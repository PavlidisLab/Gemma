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

import ubic.gemma.model.common.auditAndSecurity.Contact;
import ubic.gemma.model.common.auditAndSecurity.ContactService;
import ubic.gemma.testing.BaseTransactionalSpringContextTest;

/**
 * @author Kiran Keshav
 * @version $Id$
 */
public class ExpressionExperimentDaoImplTest extends BaseTransactionalSpringContextTest {

    ExpressionExperimentDao eeDao = null;
    ContactService cs = null;
    ExpressionExperiment ee = null;

    /**
     * @exception Exception
     */
    @Override
    protected void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();

        ee = ExpressionExperiment.Factory.newInstance();
        ee.setName( "Expression Experiment with Contact" );

        Contact c = Contact.Factory.newInstance();
        c.setName( "Foobar Barfoo" );

        cs = ( ContactService ) getBean( "contactService" );
        c = cs.findOrCreate( c );
        ee.setOwner( c );

        eeDao = ( ExpressionExperimentDao ) getBean( "expressionExperimentDao" );

        ee = eeDao.findOrCreate( ee );
        // setComplete();

    }

    /**
     * @throws Exception
     */
    public void testGetOwner() throws Exception {
        ExpressionExperiment expressionExperiment = eeDao.findByName( "Expression Experiment with Contact" );
        log.debug( "Contact: " + expressionExperiment.getOwner() );

    }

}
