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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.description.DatabaseEntry;

import java.util.Collection;
import java.util.List;

/**
 * Implementation of {@link DatabaseEntryReadService}.
 * <p>
 * All public methods are {@code @Transactional(readOnly = true)}. ACL enforcement is the
 * responsibility of the facade {@link DatabaseEntryService} interface -- this class is
 * unsecured at the AOP boundary on purpose, so intra-{@code gemma-core} callers can bypass
 * duplicate ACL checks once authenticated.
 *
 * @see DatabaseEntryService
 */
@Service("databaseEntryReadService")
public class DatabaseEntryReadServiceImpl implements DatabaseEntryReadService {

    private final DatabaseEntryDao databaseEntryDao;

    @Autowired
    public DatabaseEntryReadServiceImpl( DatabaseEntryDao databaseEntryDao ) {
        this.databaseEntryDao = databaseEntryDao;
    }

    @Override
    @Transactional(readOnly = true)
    public DatabaseEntry load( Long id ) {
        return databaseEntryDao.load( id );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<DatabaseEntry> loadAll() {
        return databaseEntryDao.loadAll();
    }

    @Override
    @Transactional(readOnly = true)
    public long countAll() {
        return databaseEntryDao.countAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DatabaseEntry> findByAccession( String accession ) {
        return databaseEntryDao.findByAccession( accession );
    }

    @Override
    @Transactional(readOnly = true)
    public DatabaseEntry findLatestByAccession( String accession ) {
        return databaseEntryDao.findByAccession( accession )
                .stream()
                .max( DatabaseEntry.getComparator() )
                .orElse( null );
    }
}
