/*
 * The Gemma project
 *
 * Copyright (c) 2013 University of British Columbia
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
package ubic.gemma.core.security.authorization;

import gemma.gsec.SecurityService;
import gemma.gsec.authentication.UserDetailsImpl;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import ubic.gemma.core.security.authentication.UserManager;
import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author cmcdonald
 */
public class SecureValueObjectAuthorizationTest extends BaseSpringContextTest {

    private final String ownerUsername = RandomStringUtils.randomAlphabetic( 5 );
    private final String aDifferentUsername = RandomStringUtils.randomAlphabetic( 5 );
    @Autowired
    private UserManager userManager;
    @Autowired
    private SecurityService securityService;
    @Autowired
    private ExpressionExperimentService eeService;
    private Long ownersExpressionExperimentId;

    private Long publicExpressionExperimentId;

    private ExpressionExperiment ee;

    @Before
    public void setUp() throws Exception {

        ExpressionExperiment publicEe = super.getTestPersistentBasicExpressionExperiment();
        securityService.makePublic( publicEe );

        publicExpressionExperimentId = publicEe.getId();

        ee = super.getTestPersistentBasicExpressionExperiment();

        ownersExpressionExperimentId = ee.getId();

        this.makeUser( ownerUsername );

        this.securityService.makeOwnedByUser( ee, ownerUsername );

        this.makeUser( aDifferentUsername );

    }

    @Test
    public void testSecuredExpressionExperimentValueObject() {

        ArrayList<Long> eeIds = new ArrayList<>();

        eeIds.add( ownersExpressionExperimentId );
        eeIds.add( publicExpressionExperimentId );

        Collection<ExpressionExperimentValueObject> valueObjects;

        super.runAsUser( aDifferentUsername, false );

        // should be able to see the ones which are
        valueObjects = eeService.loadValueObjectsByIds( eeIds, true );

        assertEquals( 1, valueObjects.size() );

        super.runAsUser( ownerUsername, false );

        valueObjects = eeService.loadValueObjectsByIds( eeIds, true );

        assertTrue( securityService.isReadableByUser( ee, ownerUsername ) );

        assertEquals( 2, valueObjects.size() );

    }

    private void makeUser( String username ) {
        try {
            this.userManager.loadUserByUsername( username );
        } catch ( UsernameNotFoundException e ) {
            this.userManager.createUser( new UserDetailsImpl( "foo", username, true, null,
                    RandomStringUtils.randomAlphabetic( 10 ) + "@gmail.com", "key", new Date() ) );
        }
    }

}
