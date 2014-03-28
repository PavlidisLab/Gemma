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
package ubic.gemma.security.authorization.acl;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import gemma.gsec.SecurityService;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import ubic.gemma.model.analysis.Investigation;
import ubic.gemma.model.analysis.expression.coexpression.CoexpressionAnalysis;
import ubic.gemma.model.analysis.expression.coexpression.CoexpressionAnalysisService;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.security.authentication.UserDetailsImpl;
import ubic.gemma.security.authentication.UserManager;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * Test the AclCollectionEntryVoter.
 * 
 * @author paul
 * @version $Id$
 */
public class AclCollectionBeforeTest extends BaseSpringContextTest {

    @Autowired
    private UserManager userManager;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private CoexpressionAnalysisService coexpressionAnalysisService;

    private ExpressionExperiment one;
    private ExpressionExperiment two;

    private Collection<BioAssaySet> ees;

    private String userName = randomName();

    @Before
    public final void setup() {

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
            userManager.createUser( new UserDetailsImpl( "foo", userName, true, null, RandomStringUtils
                    .randomAlphabetic( 10 ) + "@gmail.com", "key", new Date() ) );
        }

    }

    @Test(expected = AccessDeniedException.class)
    public final void testAclCollectionEntryVoter() {
        securityService.makePrivate( one );
        super.runAsUser( userName );
        assertTrue( securityService.isPrivate( one ) );
        coexpressionAnalysisService.findByInvestigations( ees );
        super.runAsUser( userName );
    }

    @Test
    public final void testAclCollectionEntryVoterOK() {
        // both data sets are public here.
        super.runAsUser( userName );
        Map<Investigation, Collection<CoexpressionAnalysis>> r = coexpressionAnalysisService.findByInvestigations( ees );
        assertNotNull( r );

    }
}
