/*
 * The Gemma project
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

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ubic.gemma.model.genome.Gene;

/**
 * Service for managing gene sets
 * 
 * @author kelsey
 * @version $Id: GeneSetService.java,
 */
@Service
public class GeneSetServiceImpl implements GeneSetService {

    @Autowired
    private GeneSetDao geneSetDao = null;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.gene.GeneSetService#create(java.util.Collection)
     */
    @SuppressWarnings("unchecked")
    public Collection<GeneSet> create( Collection<GeneSet> sets ) {
        return ( Collection<GeneSet> ) this.geneSetDao.create( sets );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.gene.GeneSetService#create(ubic.gemma.model.genome.gene.GeneSet)
     */
    public GeneSet create( GeneSet geneset ) {
        return this.geneSetDao.create( geneset );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.gene.GeneSetService#findByGene(ubic.gemma.model.genome.Gene)
     */
    public Collection<GeneSet> findByGene( Gene gene ) {
        return this.geneSetDao.findByGene( gene );
    }

    
    /**
     * @param gene
     * @return
     */
    public Collection<GeneSet> findByName( String name ) {
        return this.geneSetDao.findByName( name );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.gene.GeneSetService#load(java.util.Collection)
     */
    @SuppressWarnings("unchecked")
    public Collection<GeneSet> load( Collection<Long> ids ) {
        return ( Collection<GeneSet> ) this.geneSetDao.load( ids );

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.gene.GeneSetService#load(java.lang.Long)
     */
    public GeneSet load( Long id ) {
        return this.geneSetDao.load( id );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.gene.GeneSetService#loadAll()
     */
    @SuppressWarnings("unchecked")
    public Collection<GeneSet> loadAll() {
        return ( Collection<GeneSet> ) this.geneSetDao.loadAll();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.gene.GeneSetService#remove(java.util.Collection)
     */
    public void remove( Collection<GeneSet> sets ) {
        this.geneSetDao.remove( sets );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.gene.GeneSetService#remove(ubic.gemma.model.genome.gene.GeneSet)
     */
    public void remove( GeneSet geneset ) {
        this.geneSetDao.remove( geneset );
    }

    public void setGeneSetDao( GeneSetDao geneSetDao ) {
        this.geneSetDao = geneSetDao;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.gene.GeneSetService#update(java.util.Collection)
     */
    public void update( Collection<GeneSet> sets ) {
        this.geneSetDao.update( sets );

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.gene.GeneSetService#update(ubic.gemma.model.genome.gene.GeneSet)
     */
    public void update( GeneSet geneset ) {
        this.geneSetDao.update( geneset );

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.gene.GeneSetService#loadMyGeneSets()
     */
    public Collection<GeneSet> loadMyGeneSets() {
        return loadAll();
    }

    public Collection<GeneSet> loadMySharedGeneSets() {
        return loadAll();
    }
}
