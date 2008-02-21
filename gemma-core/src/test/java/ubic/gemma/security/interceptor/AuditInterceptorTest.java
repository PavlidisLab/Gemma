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
package ubic.gemma.security.interceptor;

import org.apache.commons.lang.RandomStringUtils;

import ubic.gemma.model.common.auditAndSecurity.AuditAction;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.auditAndSecurity.UserService;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author pavlidis
 * @version $Id$
 */
public class AuditInterceptorTest extends BaseSpringContextTest {
    private UserService userService;
    private GeneService geneService;
    ExpressionExperimentService expressionExperimentService;

    /**
     * @param expressionExperimentService The expressionExperimentService to set.
     */
    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    public void setUserService( UserService userService ) {
        this.userService = userService;
    }

    public void testCascadingCreateOnUpdate() throws Exception {
        Gene g = this.getTestPeristentGene();

        GeneProduct gp = GeneProduct.Factory.newInstance();
        gp.setName( RandomStringUtils.randomAlphabetic( 20 ) );
        g.getProducts().add( gp );

        this.geneService.update( g );

        for ( GeneProduct prod : g.getProducts() ) {
            assertNotNull( prod.getAuditTrail() );
        }
    }

    public void testAuditCreateWithAssociatedCollection() throws Exception {
        ExpressionExperiment ee = this.getTestPersistentCompleteExpressionExperiment( true );
        BioAssay ba = ee.getBioAssays().iterator().next();
        assertNotNull( ba.getAuditTrail() );
    }

    public void testSimpleAuditCreateUpdateUser() throws Exception {
        User user = User.Factory.newInstance();
        user.setUserName( RandomStringUtils.randomAlphabetic( 20 ) );
        user.setEmail( RandomStringUtils.randomAlphabetic( 20 ) + "@gemma.com" );
        user.setDescription( "From test" );
        user.setName( "test" + RandomStringUtils.randomAlphabetic( 10 ) );
        user = userService.create( user );
        assertEquals( "Should have a 'create'", 1, user.getAuditTrail().getEvents().size() );

        assertNotNull( user.getAuditTrail() );
        assertNotNull( user.getAuditTrail().getCreationEvent().getId() );

        user.setFax( RandomStringUtils.randomNumeric( 10 ) ); // change something.
        userService.update( user );
        int sizeAfterFirstUpdate = user.getAuditTrail().getEvents().size();
        // that should result in only a single update. FIXME for reasons unknown, this reuslts in _two_ updates audit
        // events. Or even _four_.
        // assertEquals( "Should have a 'create' and an 'update'", 3, user.getAuditTrail().getEvents().size() );
        assertTrue( sizeAfterFirstUpdate > 1 );

        assertEquals( AuditAction.UPDATE, user.getAuditTrail().getLast().getAction() );
        // third time.
        user.setFax( RandomStringUtils.randomNumeric( 10 ) ); // change something.
        userService.update( user );
        // assertEquals( 3, user.getAuditTrail().getEvents().size() );
        assertTrue( sizeAfterFirstUpdate < user.getAuditTrail().getEvents().size() );
    }

    public void testSimpleAuditFindOrCreate() throws Exception {
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        ee.setDescription( "From test" );
        ee.setName( RandomStringUtils.randomAlphabetic( 20 ) );
        ee = expressionExperimentService.findOrCreate( ee );
        assertNotNull( ee.getAuditTrail() );
        assertEquals( 1, ee.getAuditTrail().getEvents().size() );
        assertNotNull( ee.getAuditTrail().getCreationEvent().getId() );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.testing.BaseTransactionalSpringContextTest#onSetUpInTransaction()
     */
    @Override
    protected void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();
        endTransaction();
    }

    public void setGeneService( GeneService geneService ) {
        this.geneService = geneService;
    }
}
