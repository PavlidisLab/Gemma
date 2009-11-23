/*
 * The Gemma_sec1 project
 * 
 * Copyright (c) 2009 University of British Columbia
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
package ubic.gemma.model.genome.gene;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author paul
 * @version $Id$
 */
@Service
public class CandidateGeneServiceImpl implements CandidateGeneService {

    @Autowired
    CandidateGeneDao candidateGeneDao;

    /*
     * (non-Javadoc)
     * @see ubic.gemma.model.genome.gene.CandidateGeneService#create(ubic.gemma.model.genome.gene.CandidateGene)
     */
    public CandidateGene create( CandidateGene candidateGene ) {
        return this.candidateGeneDao.create( candidateGene );
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.model.genome.gene.CandidateGeneService#load(java.lang.Long)
     */
    public CandidateGene load( Long id ) {
        return this.candidateGeneDao.load( id );
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.model.genome.gene.CandidateGeneService#remove(ubic.gemma.model.genome.gene.CandidateGene)
     */
    public void remove( CandidateGene candidateGene ) {
        this.candidateGeneDao.remove( candidateGene );
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.model.genome.gene.CandidateGeneService#update(ubic.gemma.model.genome.gene.CandidateGene)
     */
    public void update( CandidateGene candidateGene ) {
        this.candidateGeneDao.update( candidateGene );
    }

}
