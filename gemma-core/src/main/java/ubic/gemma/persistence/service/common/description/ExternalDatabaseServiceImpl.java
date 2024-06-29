/*
 * The Gemma project.
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
package ubic.gemma.persistence.service.common.description;

import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.lang.Nullable;
import ubic.gemma.core.util.ListUtils;
import ubic.gemma.model.common.auditAndSecurity.eventType.ReleaseDetailsUpdateEvent;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.persistence.service.AbstractService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;

import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author pavlidis
 * @see ExternalDatabaseService
 */
@Service
public class ExternalDatabaseServiceImpl extends AbstractService<ExternalDatabase> implements ExternalDatabaseService {

    private final ExternalDatabaseDao externalDatabaseDao;

    @Autowired
    public ExternalDatabaseServiceImpl( ExternalDatabaseDao mainDao ) {
        super( mainDao );
        externalDatabaseDao = mainDao;
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
        return this.externalDatabaseDao.findByName( name );
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
        return this.externalDatabaseDao.findByNameWithAuditTrail( name );
    }

    @Autowired
    private AuditTrailService auditTrailService;

    @Override
    @Transactional
    public void updateReleaseDetails( ExternalDatabase ed, String releaseVersion, @Nullable URL releaseUrl, @Nullable String releaseNote, Date lastUpdated ) {
        String detail;
        if ( ed.getReleaseVersion() == null ) {
            detail = String.format( "Initial release version set to %s.", releaseVersion );
        } else if ( releaseVersion.equals( ed.getReleaseVersion() ) ) {
            detail = String.format( "Release version has been updated from %s to %s.", ed.getReleaseVersion(), releaseVersion );
        } else {
            detail = null;
        }
        ed.setReleaseVersion( releaseVersion );
        ed.setReleaseUrl( releaseUrl );
        ed.setLastUpdated( lastUpdated );
        auditTrailService.addUpdateEvent( ed, ReleaseDetailsUpdateEvent.class, releaseNote, detail, lastUpdated );
        update( ed );
    }

    @Override
    @Transactional
    public void updateReleaseLastUpdated( ExternalDatabase ed, @Nullable String releaseNote, Date lastUpdated ) {
        ed.setLastUpdated( lastUpdated );
        String detail = "Release last updated moment has been updated.";
        auditTrailService.addUpdateEvent( ed, ReleaseDetailsUpdateEvent.class, releaseNote, detail, lastUpdated );
        update( ed );
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