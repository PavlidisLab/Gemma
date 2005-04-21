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
import edu.columbia.gemma.common.auditAndSecurity.Person;

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
    
    protected void  handleCreateCandidateGeneList(CandidateGeneList candidateGeneList) throws java.lang.Exception{
        this.getCandidateGeneListDao().create(candidateGeneList);
    }
    
    protected void  handleRemoveCandidateGeneList(CandidateGeneList candidateGeneList) throws java.lang.Exception{
        this.getCandidateGeneListDao().remove(candidateGeneList);
    }
    
    protected void handleSaveCandidateGeneList(CandidateGeneList candidateGeneList)throws java.lang.Exception{
        this.getCandidateGeneListDao().update(candidateGeneList);   
    }
    
    // Manipulate CandidateGenes in a list
    
    protected CandidateGene handleAddCandidateToCandidateGeneList(CandidateGeneList candidateGeneList, Gene gene) throws java.lang.Exception{
        CandidateGene cg = candidateGeneList.addCandidate(gene);
        return cg;
    }
        
    protected void  handleRemoveCandidateFromCandidateGeneList(CandidateGeneList candidateGeneList, CandidateGene candidateGene) throws java.lang.Exception{
        candidateGeneList.removeCandidate(candidateGene);
    } 
    
    protected void  handleDecreaseCandidateRanking(CandidateGeneList candidateGeneList, CandidateGene candidateGene) throws java.lang.Exception{
        candidateGeneList.decreaseRanking(candidateGene);
    }
    
    protected void  handleIncreaseCandidateRanking(CandidateGeneList candidateGeneList, CandidateGene candidateGene) throws java.lang.Exception{
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
}