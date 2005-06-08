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
import edu.columbia.gemma.genome.Gene;
import edu.columbia.gemma.genome.GeneDao;
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
    	
    	AuditTrail auditTrail = this.getAuditTrailDao().create(AuditTrail.Factory.newInstance());
    	auditTrail.start("CandidateGeneList Created");
    	this.getAuditTrailDao().update(auditTrail);
    	
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
    	
    	AuditTrail auditTrail = candidateGeneList.getAuditTrail();
    	AuditEvent auditEvent = this.getAuditEventDao().create(AuditAction.UPDATE, new Date());
    	auditEvent.setPerformer(actor);
    	auditEvent.setNote("CandidateGeneList Saved");
    	auditTrail.addEvent(auditEvent);
    	this.getAuditTrailDao().update(auditTrail);
    	
    	this.getCandidateGeneListDao().update(candidateGeneList);   
    }
    
    // Manipulate CandidateGenes in a list
    
    protected CandidateGene handleAddCandidateToCandidateGeneList(CandidateGeneList candidateGeneList, Gene gene) throws java.lang.Exception{
    	assert(actor!=null);
    	
    	AuditTrail auditTrail = this.getAuditTrailDao().create(AuditTrail.Factory.newInstance());
    	auditTrail.start("CandidateGene Created");
    	this.getAuditTrailDao().update(auditTrail);
    	
    	CandidateGene cg = candidateGeneList.addCandidate(gene);
    	cg.setAuditTrail(auditTrail);
    	cg.setOwner(actor);
    	
        return cg;
    }
    
    protected CandidateGene handleAddCandidateToCandidateGeneList(CandidateGeneList candidateGeneList, long geneID ) throws java.lang.Exception{
    	assert(actor!=null);
    	
    	AuditTrail auditTrail = this.getAuditTrailDao().create(AuditTrail.Factory.newInstance());
    	auditTrail.start("CandidateGene Created");
    	
    	GeneDao gDAO = this.getGeneDao();
    	Gene g = gDAO.findByID(geneID);
    	CandidateGene cg = candidateGeneList.addCandidate(g);
    	cg.setOwner(actor);
    	cg.setAuditTrail(auditTrail);
    	
    	return cg;
    }
    protected void  handleRemoveCandidateFromCandidateGeneList(CandidateGeneList candidateGeneList, CandidateGene candidateGene) throws java.lang.Exception{
    	assert(actor!=null);
    	candidateGeneList.removeCandidate(candidateGene);
    } 
    
    protected void  handleDecreaseCandidateRanking(CandidateGeneList candidateGeneList, CandidateGene candidateGene) throws java.lang.Exception{
    	assert(actor!=null);
    	AuditTrail auditTrail = candidateGeneList.getAuditTrail();
    	assert(auditTrail != null);
    	AuditEvent auditEvent = this.getAuditEventDao().create(AuditAction.UPDATE, new Date());
    	auditEvent.setPerformer(actor);
    	auditEvent.setNote("CandidateList Ranking Modified: ID " + candidateGene.getId().toString() + " rank decreased.");
    	auditTrail.addEvent(auditEvent);
    	
    	candidateGeneList.decreaseRanking(candidateGene);
    }
    
    protected void  handleIncreaseCandidateRanking(CandidateGeneList candidateGeneList, CandidateGene candidateGene) throws java.lang.Exception{
    	assert(actor!=null);
    	AuditTrail auditTrail = candidateGeneList.getAuditTrail();
    	assert(auditTrail != null);
    	AuditEvent auditEvent = this.getAuditEventDao().create(AuditAction.UPDATE, new Date());
    	auditEvent.setPerformer(actor);
    	auditEvent.setNote("CandidateList Ranking Modified: ID " + candidateGene.getId().toString() + " rank increased.");
    	auditTrail.addEvent(auditEvent);
    	
    	candidateGeneList.increaseRanking(candidateGene);
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
}