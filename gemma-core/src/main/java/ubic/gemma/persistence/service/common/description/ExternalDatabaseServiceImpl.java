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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.security.audit.payload.ReleaseDetailsUpdatePayload;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.persistence.service.AbstractService;

import org.springframework.lang.Nullable;
import java.net.URL;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * @author pavlidis
 * @see ExternalDatabaseService
 */
@Service
public class ExternalDatabaseServiceImpl extends AbstractService<ExternalDatabase> implements ExternalDatabaseService {

    @Autowired
    private ExternalDatabaseReadService readService;

    /**
     * Co-bean carrying the {@code @Audited} hook for release-details
     * writes. Replaces the imperative 5-arg
     * {@code auditTrailService.addUpdateEvent(ed, ReleaseDetailsUpdateEvent.class,
     * note, detail, lastUpdated)} previously used by
     * {@link #updateReleaseDetails} and {@link #updateReleaseLastUpdated}.
     * The audit row's {@code performedDate} is now {@code now()} rather
     * than the supplied {@code lastUpdated} — the original moment is
     * preserved in the JSON payload and on the entity.
     */
    @Autowired
    private ExternalDatabaseReleaseAuditService releaseAuditService;

    @Autowired
    public ExternalDatabaseServiceImpl( ExternalDatabaseDao mainDao ) {
        super( mainDao );
    }

    // =====================================================================
    // Read methods -- delegate to ExternalDatabaseReadService.
    // ACL @Secured annotations live on the ExternalDatabaseService interface
    // and apply at the facade proxy boundary.
    // =====================================================================

    @Override
    public Collection<ExternalDatabase> loadAllWithAuditTrail() {
        return readService.loadAllWithAuditTrail();
    }

    @Override
    public ExternalDatabase loadWithExternalDatabases( Long id ) {
        return readService.loadWithExternalDatabases( id );
    }

    @Override
    public ExternalDatabase findByName( String name ) {
        return readService.findByName( name );
    }

    @Override
    public ExternalDatabase findByNameWithExternalDatabases( String name ) {
        return readService.findByNameWithExternalDatabases( name );
    }

    @Override
    public ExternalDatabase findByNameWithAuditTrail( String name ) {
        return readService.findByNameWithAuditTrail( name );
    }

    @Override
    public List<ExternalDatabase> findAllByNameIn( List<String> names ) {
        return readService.findAllByNameIn( names );
    }

    // =====================================================================
    // Write methods -- stay on the facade.
    // =====================================================================

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
        update( ed );
        // Audit emission routed through a helper bean so the @Audited
        // aspect can intercept; the legacy 5-arg form's explicit
        // performedDate=lastUpdated is now carried in the JSON payload
        // (the audit row's own performedDate is now() under the aspect).
        // Inventory #9.
        releaseAuditService.recordReleaseDetailsUpdate( ed, releaseNote,
                new ReleaseDetailsUpdatePayload( releaseVersion,
                        releaseUrl != null ? releaseUrl.toString() : null,
                        lastUpdated, detail ) );
    }

    @Override
    @Transactional
    public void updateReleaseLastUpdated( ExternalDatabase ed, @Nullable String releaseNote, Date lastUpdated ) {
        ed.setLastUpdated( lastUpdated );
        String detail = "Release last updated moment has been updated.";
        update( ed );
        // Audit emission routed through a helper bean; see the note on
        // updateReleaseDetails above. Inventory #10.
        releaseAuditService.recordReleaseDetailsUpdate( ed, releaseNote,
                new ReleaseDetailsUpdatePayload( ed.getReleaseVersion(),
                        ed.getReleaseUrl() != null ? ed.getReleaseUrl().toString() : null,
                        lastUpdated, detail ) );
    }
}
