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
/**
*
*
* <hr>
* <p>Copyright (c) 2004, 2005 Columbia University
* @author daq2101
* @version $Id$
*/
package edu.columbia.gemma.genome.gene;


import java.util.Iterator;
import java.util.Collection;

import edu.columbia.gemma.genome.Gene;

/**
 * @see edu.columbia.gemma.genome.gene.CandidateGeneListService
 */
public class CandidateGeneListServiceImpl
    extends edu.columbia.gemma.genome.gene.CandidateGeneListServiceBase
{
    private CandidateGeneDao candidateGeneDao;
    private CandidateGeneListDao candidateGeneListDao;
    
    protected CandidateGene handleAddCandidateToList(CandidateGeneList candidateGeneList, Gene gene){
        Collection candidates = candidateGeneList.getCandidates();
        
        // figure out the highest rank in this candidate list 
        // note that if the list is empty the first item has rank of 0
        int maxRank=-1;
        if(candidates!=null){
            Iterator iter = candidates.iterator();
            while(iter.hasNext()){
                CandidateGene cg = (CandidateGene) iter.next();
                if(cg.getRank().intValue()>maxRank)
                    maxRank=cg.getRank().intValue();
            }
        }
    
        // new candidate gene comes at end of list
        maxRank = maxRank+1;
        
        // create new candidate gene and set rank accordingly
        CandidateGene cgNew = CandidateGene.Factory.newInstance();
        cgNew.setGene(gene);
        cgNew.setRank(new Integer(maxRank));
               
        candidateGeneDao.create(cgNew);
        
        // add newly created candidate gene to candidateGeneList and update
        candidateGeneList.addCandidate(cgNew);
        candidateGeneListDao.update(candidateGeneList);
        
        return cgNew;
    }
    protected void handleRemoveCandidateFromList(CandidateGeneList candidateGeneList, CandidateGene candidateGene){
        candidateGeneList.removeCandidate(candidateGene);
        getCandidateGeneListDao().update(candidateGeneList);
        getCandidateGeneDao().remove(candidateGene);
    }
    /**
     * @see edu.columbia.gemma.genome.gene.CandidateGeneListService#FindByGeneOfficialName(edu.columbia.gemma.genome.Gene)
     */
    protected java.util.Collection handleFindByGeneOfficialName(edu.columbia.gemma.genome.Gene gene)
        throws java.lang.Exception
    {
        //@todo implement protected java.util.Collection handleFindByGeneOfficialName(edu.columbia.gemma.genome.Gene gene)
        return null;
    }

    /**
     * @see edu.columbia.gemma.genome.gene.CandidateGeneListService#FindByContributer(edu.columbia.gemma.common.auditAndSecurity.Person)
     */
    protected java.util.Collection handleFindByContributer(edu.columbia.gemma.common.auditAndSecurity.Person person)
        throws java.lang.Exception
    {
        //@todo implement protected java.util.Collection handleFindByContributer(edu.columbia.gemma.common.auditAndSecurity.Person person)
        return null;
    }

    public void setDaoCG( CandidateGeneDao daoCG ) {
        this.candidateGeneDao = daoCG;
    }
    public void setDaoCGL( CandidateGeneListDao daoCGL ) {
        this.candidateGeneListDao = daoCGL;
    }
}