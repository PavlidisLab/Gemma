/*
 * The Gemma project.
 *
 * Copyright (c) 2006-2007 University of British Columbia
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

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.core.lang.Nullable;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.persistence.service.BaseService;

import java.net.URL;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * @author Gemma
 */
public interface ExternalDatabaseService extends BaseService<ExternalDatabase> {

    @Secured({ "GROUP_ADMIN" })
    Collection<ExternalDatabase> loadAllWithAuditTrail();

    @Override
    @Secured({ "GROUP_ADMIN" })
    Collection<ExternalDatabase> create( Collection<ExternalDatabase> entities );

    @Override
    @Secured({ "GROUP_ADMIN" })
    ExternalDatabase create( ExternalDatabase entity );

    @Nullable
    ExternalDatabase loadWithExternalDatabases( Long id );

    @Nullable
    ExternalDatabase findByName( String name );

    @Nullable
    ExternalDatabase findByNameWithExternalDatabases( String name );

    @Nullable
    @Secured({ "GROUP_AGENT" })
    ExternalDatabase findByNameWithAuditTrail( String name );

    @Override
    @Secured({ "GROUP_ADMIN" })
    ExternalDatabase findOrCreate( ExternalDatabase externalDatabase );

    @Override
    @Secured({ "GROUP_AGENT" })
    void update( ExternalDatabase entity );

    @Override
    @Secured({ "GROUP_AGENT" })
    void update( Collection<ExternalDatabase> entities );

    @Secured({ "GROUP_AGENT" })
    void updateReleaseDetails( ExternalDatabase externalDatabase, String releaseVersion, @Nullable URL releaseUrl, @Nullable String releaseNote, Date lastUpdated );

    @Secured({ "GROUP_AGENT" })
    void updateReleaseLastUpdated( ExternalDatabase externalDatabase, @Nullable String note, Date lastUpdated );

    @Override
    @Secured({ "GROUP_ADMIN" })
    void remove( ExternalDatabase externalDatabase );

    @Override
    @Secured({ "GROUP_ADMIN" })
    void remove( Collection<ExternalDatabase> entities );

    List<ExternalDatabase> findAllByNameIn( List<String> names );
}
