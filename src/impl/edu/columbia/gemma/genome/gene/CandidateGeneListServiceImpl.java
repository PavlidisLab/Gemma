/*
 * The Gemma project.
 * 
 * Copyright (c) 2005 Columbia University
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

package edu.columbia.gemma.genome.gene;
import java.util.Collection;
import edu.columbia.gemma.common.auditAndSecurity.Person;
import edu.columbia.gemma.common.auditAndSecurity.AuditTrail;
import edu.columbia.gemma.common.auditAndSecurity.AuditEvent;
import edu.columbia.gemma.common.auditAndSecurity.AuditAction;
import java.util.Date;
/**
 *
 * <hr>
 * <p>Copyright (c) 2004, 2005 Columbia University
 * @author daq2101
 * @version $Id$
 */
public class CandidateGeneListServiceImpl
    extends edu.columbia.gemma.genome.gene.CandidateGeneListServiceBase
{
    // CandidateGeneList manipulation
	Person actor = null;
	
	protected void handleSetActor(Person actor){
		this.actor=actor;
	}
	
    protected CandidateGeneList handleCreateCandidateGeneList(String newName ) throws java.lang.Exception{
        assert(this.actor!=null);
        
    	CandidateGeneList cgl = CandidateGeneList.Factory.newInstance();
    	AuditTrail auditTrail = AuditTrail.Factory.newInstance();
    	AuditEvent auditEvent = AuditEvent.Factory.newInstance();
    	auditEvent.setAction(AuditAction.CREATE);
    	auditEvent.setDate(new Date());
    	auditEvent.setNote("CandidateGeneList Created");
    	auditEvent.setPerformer(actor);
    	auditTrail.addEvent(auditEvent);
    	cgl.setAuditTrail(auditTrail);
    	cgl.setName(newName);
        cgl.setOwner(actor);
    	this.getCandidateGeneListDao().create(cgl);
    	return cgl;
    }
    
    protected void  handleRemoveCandidateGeneList(CandidateGeneList candidateGeneList) throws java.lang.Exception{
    	this.getCandidateGeneListDao().remove(candidateGeneList);
    }
    
    protected void handleSaveCandidateGeneList(CandidateGeneList candidateGeneList)throws java.lang.Exception{
    	assert(actor!=null);
    	candidateGeneList.getAuditTrail().update("CandidateGeneList Saved", actor);
    	this.getCandidateGeneListDao().update(candidateGeneList);   
    }
    
    // Finder methods
    
    public Collection handleFindByGeneOfficialName(String officialName){
       return this.getCandidateGeneListDao().findByGeneOfficialName(officialName);
    }
    
    public Collection handleFindByContributer(Person person){
        return this.getCandidateGeneListDao().findByContributer(person);
    }
   
    public Collection handleGetAll(){
        return this.getCandidateGeneListDao().findAll();
    }
    public CandidateGeneList handleFindByID(long id){
    	return this.getCandidateGeneListDao().findByID(id);
    }
    
    public Collection handleFindByListOwner(Person owner){
    	return this.getCandidateGeneListDao().findByListOwner(owner);
    }
}