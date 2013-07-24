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

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.annotation.ExpectedException;

import ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionService;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.security.SecurityService;
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
    private Probe2ProbeCoexpressionService ppcs;

    ExpressionExperiment one;
    ExpressionExperiment two;

    Gene g;

    Collection<BioAssaySet> ees;

    String userName = randomName();

    @Before
    public final void setup() {

        g = super.getTestPeristentGene();
        one = super.getTestPersistentBasicExpressionExperiment();
        two = super.getTestPersistentBasicExpressionExperiment();

        ees = new HashSet<BioAssaySet>();
        ees.add( one );
        ees.add( two );

        try {
            userManager.loadUserByUsername( userName );
        } catch ( UsernameNotFoundException e ) {
            userManager.createUser( new UserDetailsImpl( "foo", userName, true, null, RandomStringUtils
                    .randomAlphabetic( 10 ) + "@gmail.com", "key", new Date() ) );
        }

    }

    @Test
    @ExpectedException(AccessDeniedException.class)
    public final void testAclCollectionEntryVoter() {
        securityService.makePrivate( one );

        super.runAsUser( userName );

        ppcs.getExpressionExperimentsLinkTestedIn( g, ees, false );

    }

    @Test
    public final void testAclCollectionEntryVoterOK() {

        super.runAsUser( userName );

        Collection<BioAssaySet> r = ppcs.getExpressionExperimentsLinkTestedIn( g, ees, false );

        // lack of an exception here is what we're really interested in.
        assertNotNull( r );

    }

}
