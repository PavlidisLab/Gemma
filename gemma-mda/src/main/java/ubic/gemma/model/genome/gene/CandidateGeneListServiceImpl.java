/*
 * The Gemma project.
 * 
 * Copyright (c) 2006 University of British Columbia
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */

package ubic.gemma.model.genome.gene;

import java.util.Collection;
import java.util.Date;

import ubic.gemma.model.common.auditAndSecurity.AuditAction;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.AuditTrail;
import ubic.gemma.model.common.auditAndSecurity.Person;
import ubic.gemma.model.common.auditAndSecurity.User;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004, 2006 University of British Columbia
 * 
 * @author daq2101
 * @version $Id$
 */
public class CandidateGeneListServiceImpl extends ubic.gemma.model.genome.gene.CandidateGeneListServiceBase {

    User actor = null;

    @Override
    public Collection handleFindAll() {
        return this.getCandidateGeneListDao().loadAll();
    }

    @Override
    public Collection handleFindByContributer( Person person ) {
        return this.getCandidateGeneListDao().findByContributer( person );
    }

    @Override
    public Collection handleFindByGeneOfficialName( String officialName ) {
        return this.getCandidateGeneListDao().findByGeneOfficialName( officialName );
    }

    @Override
    public CandidateGeneList handleFindByID( Long id ) {
        return this.getCandidateGeneListDao().findByID( id );
    }

    @Override
    public Collection handleFindByListOwner( Person owner ) {
        return this.getCandidateGeneListDao().findByListOwner( owner );
    }

    @Override
    protected CandidateGeneList handleCreateByName( String newName ) throws java.lang.Exception {
        assert ( this.actor != null );

        CandidateGeneList cgl = CandidateGeneList.Factory.newInstance();
        AuditTrail auditTrail = AuditTrail.Factory.newInstance();
        AuditEvent auditEvent = AuditEvent.Factory.newInstance();
        auditEvent.setAction( AuditAction.CREATE );
        auditEvent.setDate( new Date() );
        auditEvent.setPerformer( actor );
        auditTrail.addEvent( auditEvent );
        cgl.setAuditTrail( auditTrail );
        cgl.setName( newName );
        cgl.setOwner( actor );
        return this.saveCandidateGeneList( cgl );
    }

    @Override
    protected void handleRemoveCandidateGeneList( CandidateGeneList candidateGeneList ) throws java.lang.Exception {
        this.getCandidateGeneListDao().remove( candidateGeneList );
    }

    @Override
    protected CandidateGeneList handleSaveCandidateGeneList( CandidateGeneList candidateGeneList ) throws Exception {
        return ( CandidateGeneList ) this.getCandidateGeneListDao().create( candidateGeneList );
    }

    @Override
    protected void handleSetActor( User actor1 ) {
        this.actor = actor1;
    }

    @Override
    protected void handleUpdateCandidateGeneList( CandidateGeneList candidateGeneList ) throws java.lang.Exception {
        assert ( actor != null );
        candidateGeneList.getAuditTrail().update( "CandidateGeneList Saved", actor );
        this.getCandidateGeneListDao().update( candidateGeneList );
    }

}