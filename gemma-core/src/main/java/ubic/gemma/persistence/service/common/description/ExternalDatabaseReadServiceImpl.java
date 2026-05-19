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
package ubic.gemma.persistence.service.common.description;

import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.util.ListUtils;
import ubic.gemma.model.common.description.ExternalDatabase;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation of {@link ExternalDatabaseReadService}.
 * <p>
 * All public methods are {@code @Transactional(readOnly = true)}. ACL enforcement is the
 * responsibility of the facade {@link ExternalDatabaseService} interface -- this class is
 * unsecured at the AOP boundary on purpose, so intra-{@code gemma-core} callers can bypass
 * duplicate ACL checks once authenticated.
 *
 * @see ExternalDatabaseService
 */
@Service("externalDatabaseReadService")
public class ExternalDatabaseReadServiceImpl implements ExternalDatabaseReadService {

    private final ExternalDatabaseDao externalDatabaseDao;

    @Autowired
    public ExternalDatabaseReadServiceImpl( ExternalDatabaseDao externalDatabaseDao ) {
        this.externalDatabaseDao = externalDatabaseDao;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExternalDatabase> loadAllWithAuditTrail() {
        Collection<ExternalDatabase> eds = externalDatabaseDao.loadAll();
        eds.forEach( ed -> Hibernate.initialize( ed.getAuditTrail() ) );
        return eds;
    }

    @Override
    @Transactional(readOnly = true)
    public ExternalDatabase loadWithExternalDatabases( Long id ) {
        ExternalDatabase ed = externalDatabaseDao.load( id );
        if ( ed != null ) {
            Hibernate.initialize( ed.getExternalDatabases() );
        }
        return ed;
    }

    @Override
    @Transactional(readOnly = true)
    public ExternalDatabase findByName( String name ) {
        return externalDatabaseDao.findByName( name );
    }

    @Override
    @Transactional(readOnly = true)
    public ExternalDatabase findByNameWithExternalDatabases( String name ) {
        ExternalDatabase ed = externalDatabaseDao.findByName( name );
        if ( ed != null ) {
            Hibernate.initialize( ed.getExternalDatabases() );
        }
        return ed;
    }

    @Override
    @Transactional(readOnly = true)
    public ExternalDatabase findByNameWithAuditTrail( String name ) {
        return externalDatabaseDao.findByNameWithAuditTrail( name );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExternalDatabase> findAllByNameIn( List<String> names ) {
        // the database is case insensitive...
        Map<String, Integer> namesIndex = ListUtils.indexOfCaseInsensitiveStringElements( names );
        return externalDatabaseDao.findAllByNameIn( names ).stream()
                .sorted( Comparator.comparing( ed -> namesIndex.get( ed.getName() ) ) )
                .collect( Collectors.toList() );
    }
}
