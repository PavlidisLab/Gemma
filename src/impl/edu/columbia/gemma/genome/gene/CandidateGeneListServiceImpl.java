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
import java.util.ResourceBundle;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import edu.columbia.gemma.genome.Gene;

/**
 * @see edu.columbia.gemma.genome.gene.CandidateGeneListService
 */
public class CandidateGeneListServiceImpl
    extends edu.columbia.gemma.genome.gene.CandidateGeneListServiceBase
{

    // spring will automatically see this and create it for me.  Magic!
    private static CandidateGeneListDao daoCGL;
    private static CandidateGeneDao daoCG;
    
    protected final static BeanFactory ctx;
    protected ResourceBundle rb;
    
    //  This static block ensures that Spring's BeanFactory is only loaded
    // once for all tests
    static {
        ResourceBundle db = ResourceBundle.getBundle( "testResources" );
        String daoType = db.getString( "dao.type" );
        String servletContext = db.getString("servlet.name.0");

        // CAREFUL, these paths are dependent on the classpath for the test.
        String[] paths = { "applicationContext-dataSource.xml", "applicationContext-" + daoType + ".xml"};//, servletContext+"-servlet.xml" };
        ctx = new ClassPathXmlApplicationContext( paths );
        daoCG= ( CandidateGeneDao ) ctx.getBean( "candidateGeneDao" );
        daoCGL= ( CandidateGeneListDao ) ctx.getBean( "candidateGeneListDao" );
    }

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
        
        daoCG.create(cgNew);
        
        // add newly created candidate gene to candidateGeneList and update
        candidateGeneList.addCandidate(cgNew);
        daoCGL.update(candidateGeneList);
        
        return cgNew;
    }
    protected void handleRemoveCandidateFromList(CandidateGeneList candidateGeneList, CandidateGene candidateGene){
        candidateGeneList.removeCandidate(candidateGene);
        daoCGL.update(candidateGeneList);
        daoCG.remove(candidateGene);
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

}