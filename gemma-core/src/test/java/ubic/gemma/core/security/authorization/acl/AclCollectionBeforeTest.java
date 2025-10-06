/*
 * The Gemma_sec1 project
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
package ubic.gemma.core.security.authorization.acl;

import gemma.gsec.SecurityService;
import gemma.gsec.authentication.UserDetailsImpl;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import ubic.gemma.core.security.authentication.UserManager;
import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.model.analysis.expression.coexpression.CoexpressionAnalysis;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.analysis.expression.coexpression.CoexpressionAnalysisService;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test the AclCollectionEntryVoter.
 *
 * @author paul
 */
public class AclCollectionBeforeTest extends BaseSpringContextTest {

    private final String userName = this.randomName();
    @Autowired
    private UserManager userManager;
    @Autowired
    private SecurityService securityService;
    @Autowired
    private CoexpressionAnalysisService coexpressionAnalysisService;
    private ExpressionExperiment one;
    @SuppressWarnings("FieldCanBeLocal")
    private ExpressionExperiment two;
    private Collection<ExpressionExperiment> ees;

    @Before
    public final void setUp() throws Exception {

        one = super.getTestPersistentBasicExpressionExperiment();
        two = super.getTestPersistentBasicExpressionExperiment();

        securityService.makePublic( one );
        securityService.makePublic( two );

        ees = new HashSet<>();
        ees.add( one );
        ees.add( two );

        try {
            userManager.loadUserByUsername( userName );
        } catch ( UsernameNotFoundException e ) {
            userManager.createUser( new UserDetailsImpl( "foo", userName, true, null,
                    RandomStringUtils.insecure().nextAlphabetic( 10 ) + "@gmail.com", "key", new Date() ) );
        }

    }

    @Test(expected = AccessDeniedException.class)
    public final void testAclCollectionEntryVoter() {
        securityService.makePrivate( one );
        super.runAsUser( userName );
        assertTrue( securityService.isPrivate( one ) );
        coexpressionAnalysisService.findByExperimentsAnalyzed( ees );
        super.runAsUser( userName );
    }

    @Test
    public final void testAclCollectionEntryVoterOK() {
        // both data sets are public here.
        super.runAsUser( userName );
        Map<ExpressionExperiment, Collection<CoexpressionAnalysis>> r = coexpressionAnalysisService
                .findByExperimentsAnalyzed( ees );
        assertNotNull( r );
    }
}
