/*
 * The Gemma project.
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.core.security.authentication;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.auditAndSecurity.GroupAuthority;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.auditAndSecurity.UserGroup;
import ubic.gemma.persistence.service.common.auditAndSecurity.UserDao;
import ubic.gemma.persistence.service.common.auditAndSecurity.UserGroupDao;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Read-only implementation of the {@link UserService} read cluster.
 * <p>
 * Depends only on {@link UserDao} and {@link UserGroupDao}; orchestrates nothing.
 * All methods are {@code @Transactional(readOnly = true)}.
 *
 * @author paul
 * @see UserReadService
 */
@Service("userReadService")
public class UserReadServiceImpl implements UserReadService {

    private final UserDao userDao;
    private final UserGroupDao userGroupDao;

    @Autowired
    public UserReadServiceImpl( UserDao userDao, UserGroupDao userGroupDao ) {
        this.userDao = userDao;
        this.userGroupDao = userGroupDao;
    }

    @Override
    @Transactional(readOnly = true)
    public User load( Long id ) {
        return this.userDao.load( id );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<User> loadAll() {
        return new ArrayList<>( this.userDao.loadAll() );
    }

    @Override
    @Transactional(readOnly = true)
    public User findByUserName( String userName ) {
        return this.userDao.findByUserName( userName );
    }

    @Override
    @Transactional(readOnly = true)
    public User findByEmail( String email ) {
        return this.userDao.findByEmail( email );
    }

    @Override
    @Transactional(readOnly = true)
    public UserGroup findGroupByName( String name ) {
        return this.userGroupDao.findByName( name );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean groupExists( String name ) {
        return this.userGroupDao.findByName( name ) != null;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<UserGroup> findGroupsForUser( User user ) {
        return new ArrayList<>( this.userGroupDao.findGroupsForUser( user ) );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<UserGroup> listAvailableGroups() {
        return new ArrayList<>( this.userGroupDao.loadAll() );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<GroupAuthority> loadGroupAuthorities( User user ) {
        return new ArrayList<>( this.userDao.loadGroupAuthorities( user ) );
    }
}
