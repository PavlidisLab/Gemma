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
package ubic.gemma.web.validation;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.validator.Validator;
import org.apache.commons.validator.ValidatorException;
import org.springframework.security.providers.encoding.ShaPasswordEncoder;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springmodules.validation.commons.DefaultValidatorFactory;

import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.testing.BaseSpringContextTest;
import ubic.gemma.util.ConfigUtils;
import ubic.gemma.web.controller.common.auditAndSecurity.UserUpdateCommand;

/**
 * @author pavlidis
 * @version $Id$
 */
public class ValidationTest extends BaseSpringContextTest {

    public final void testUserValidation() throws Exception {
        UserUpdateCommand c = getBasicValidUser();
        Errors e = validate( c, "user" );
        assertEquals( e.toString(), 0, e.getErrorCount() );
    }

    public final void testUserValidationBadEmail() throws Exception {
        UserUpdateCommand c = getBasicValidUser();
        c.setEmail( "@@@@@.com" );

        Errors e = validate( c, "user" );
        assertEquals( e.toString(), 1, e.getErrorCount() );
        assertNotNull( e.getFieldError( "email" ) );
    }

    public final void testUserValidationNoEmail() throws Exception {
        UserUpdateCommand c = getBasicValidUser();
        c.setEmail( null );

        Errors e = validate( c, "user" );
        assertEquals( e.toString(), 1, e.getErrorCount() );
        assertNotNull( e.getFieldError( "email" ) );
    }

    public final void testUserNameTooShort() throws Exception {
        UserUpdateCommand c = getBasicValidUser();
        c.setUserName( "a" );

        Errors e = validate( c, "user" );
        assertEquals( e.toString(), 1, e.getErrorCount() );
        assertNotNull( e.getFieldError( "userName" ) );
    }

    public final void testUserNameTooLong() throws Exception {
        UserUpdateCommand c = getBasicValidUser();
        c.setUserName( "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" );

        Errors e = validate( c, "user" );
        assertEquals( e.toString(), 1, e.getErrorCount() );
        assertNotNull( e.getFieldError( "userName" ) );
    }

    public final void testUserNameMissing() throws Exception {
        UserUpdateCommand c = getBasicValidUser();
        c.setUserName( null );

        Errors e = validate( c, "user" );
        assertEquals( e.toString(), 1, e.getErrorCount() );
        assertNotNull( e.getFieldError( "userName" ) );
    }

    public final void testUserValidationPasswordMismatch() throws Exception {
        UserUpdateCommand c = getBasicValidUser();

        c.setNewPassword( "onepwd" );
        c.setConfirmNewPassword( "other" );

        Errors e = validate( c, "user" );
        assertEquals( e.toString(), 2, e.getErrorCount() );
        assertNotNull( e.getFieldError( "confirmNewPassword" ) );
    }

    public final void testUserValidationPasswordTooShort() throws Exception {
        UserUpdateCommand c = getBasicValidUser();

        c.setNewPassword( "a" );
        c.setConfirmNewPassword( "a" );

        Errors e = validate( c, "user" );
        assertEquals( e.toString(), 1, e.getErrorCount() );
        assertNotNull( e.getFieldError( "newPassword" ) );
    }

    public final void testUserValidationPasswordNewMissing() throws Exception {
        UserUpdateCommand c = getBasicValidUser();

        c.setConfirmNewPassword( null );

        Errors e = validate( c, "user" );
        assertEquals( e.toString(), 2, e.getErrorCount() );
        assertNotNull( e.getFieldError( "newPassword" ) );
        assertNotNull( e.getFieldError( "confirmNewPassword" ) );
    }

    public final void testUserValidationPasswordConfirmMissing() throws Exception {
        UserUpdateCommand c = getBasicValidUser();

        c.setConfirmNewPassword( null );

        Errors e = validate( c, "user" );
        assertEquals( e.toString(), 2, e.getErrorCount() );
        assertNotNull( e.getFieldError( "confirmNewPassword" ) );
        assertNotNull( e.getFieldError( "newPassword" ) );
    }

    public final void testUserValidationNoPasswordsOK() throws Exception {
        UserUpdateCommand c = getBasicValidUser();

        c.setConfirmNewPassword( null );
        c.setNewPassword( null );

        Errors e = validate( c, "user" );
        assertEquals( e.toString(), 0, e.getErrorCount() );
    }

    /**
     * @param factory
     * @param c
     * @return
     * @throws ValidatorException
     */
    private Errors validate( Object c, String validator ) throws ValidatorException {
        org.springmodules.validation.commons.DefaultValidatorFactory factory = ( DefaultValidatorFactory ) getBean( "validatorFactory" );
        assert factory != null;
        Errors e = new BindException( c, validator );
        Validator v = factory.getValidator( validator, c, e );

        v.validate();
        return e;
    }

    /**
     * @return
     */
    private UserUpdateCommand getBasicValidUser() {
        User testUser = User.Factory.newInstance();

        testUser.setName( "Foo" );
        testUser.setLastName( "Bar" );
        testUser.setEnabled( Boolean.TRUE );
        testUser.setUserName( "trice" );
        testUser.setEmail( RandomStringUtils.randomAlphabetic( 6 ).toLowerCase() + "@gemma.org" );

        ShaPasswordEncoder encoder = new ShaPasswordEncoder();
        String encryptedPassword = encoder.encodePassword( "twice", ConfigUtils.getProperty( "gemma.salt" ) );

        testUser.setPassword( encryptedPassword );
        testUser.setPasswordHint( "I am an idiot" );

        UserUpdateCommand c = new UserUpdateCommand( testUser );

        c.setNewPassword( "apassword" );
        c.setConfirmNewPassword( "apassword" );

        return c;
    }

}
