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
package ubic.gemma.core.security.authentication;

import ubic.gemma.core.security.SecurityService;
import ubic.gemma.core.security.acl.domain.AclService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.util.test.BaseTest5;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.auditAndSecurity.UserGroup;
import ubic.gemma.persistence.service.common.auditAndSecurity.UserDao;
import ubic.gemma.persistence.service.common.auditAndSecurity.UserGroupDao;

import java.util.Collection;
import java.util.HashSet;

import static org.mockito.Mockito.*;

/**
 * @author pavlidis
 */
@ContextConfiguration
public class UserServiceImplTest extends BaseTest5 {

    @Configuration
    @TestComponent
    static class UserServiceImplTestContextConfiguration {

        @Bean
        public UserService userService() {
            return new UserServiceImpl();
        }

        @Bean
        public UserReadService userReadService( UserDao userDao, UserGroupDao userGroupDao ) {
            return new UserReadServiceImpl( userDao, userGroupDao );
        }

        @Bean
        public UserDao userDao() {
            return mock();
        }

        @Bean
        public UserGroupDao userGroupDao() {
            return mock();
        }

        @Bean
        public AclService aclService() {
            return mock();
        }

        @Bean
        public SecurityService securityService() {
            return mock();
        }
    }

    @Autowired
    private UserService userService;

    @Autowired
    private UserDao userDaoMock;

    private final User testUser = User.Factory.newInstance( "foobar" );

    private Collection<UserGroup> userGroups;

    @BeforeEach
    public void setUp() {
        testUser.setId( 1L );
        testUser.setEmail( "foo@bar" );
        testUser.setName( "Foo" );
        testUser.setLastName( "Bar" );
        testUser.setPassword( "aija" );
        testUser.setPasswordHint( "I am an idiot" );

        UserGroup group = UserGroup.Factory.newInstance();
        group.setName( "users" );
        group.getGroupMembers().add( testUser );
        userGroups = new HashSet<>();
        userGroups.add( group );
    }

    @AfterEach
    public void resetMocks() {
        reset( userDaoMock );
    }

    @Test
    public void testHandleGetUser() {
        when( userDaoMock.findByUserName( "foobar" ) ).thenReturn( testUser );
        userService.findByUserName( "foobar" );
        verify( userDaoMock ).findByUserName( "foobar" );
    }

    @Test
    public void testHandleSaveUser() throws Exception {
        userService.create( testUser );
        verify( userDaoMock ).findByUserName( "foobar" );
        verify( userDaoMock ).findByEmail( "foo@bar" );
        verify( userDaoMock ).create( testUser );
    }

    @Test
    public void testHandleRemoveUser() {
        when( userDaoMock.load( testUser.getId() ) ).thenReturn( testUser );
        when( userDaoMock.loadGroups( testUser ) ).thenReturn( userGroups );
        userService.delete( testUser );
        verify( userDaoMock ).remove( testUser );
    }
}
