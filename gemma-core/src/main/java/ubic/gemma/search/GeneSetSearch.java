/*
 * The Gemma project
 * 
 * Copyright (c) 2012 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.search;

import java.util.Collection;

import ubic.gemma.genome.gene.GOGroupValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneSet;
import ubic.gemma.model.genome.gene.GeneSetValueObject;

/**
 * @author paul
 * @version $Id$
 */
public interface GeneSetSearch {

    /**
     * @param gene
     * @return
     * @see ubic.gemma.genome.gene.service.GeneSetService#findByGene(ubic.gemma.model.genome.Gene)
     */
    public abstract Collection<GeneSet> findByGene( Gene gene );

    /**
     * Finds gene sets by exact match to goTermId eg: GO:0000002 Note: the gene set returned is not persistent.
     * 
     * @param goId
     * @param taxon
     * @return a GeneSet or null if nothing is found
     */
    public abstract GOGroupValueObject findGeneSetValueObjectByGoId( String goId, Long taxonId );

    /**
     * Finds gene sets by exact match to goTermId eg: GO:0000002 Note: the gene set returned is not persistent.
     * 
     * @param goId
     * @param taxon
     * @return a GeneSet or null if nothing is found
     */
    public abstract GeneSet findByGoId( String goId, Taxon taxon );

    /**
     * finds genesets by go term name eg: "trans-hexaprenyltranstransferase activity" Note: the gene sets returned are
     * not persistent
     * 
     * @param goTermName
     * @param taxon
     * @return a collection with the hits
     */
    public abstract Collection<GeneSet> findByGoTermName( String goTermName, Taxon taxon );

    /**
     * finds genesets by go term name eg: "trans-hexaprenyltranstransferase activity" Note: the gene sets returned are
     * not persistent
     * 
     * @param goTermName
     * @param taxon
     * @param maxGoTermsProcessed
     * @return a collection with the hits
     */
    public abstract Collection<GeneSet> findByGoTermName( String goTermName, Taxon taxon, Integer maxGoTermsProcessed, Integer maxGeneSetSize );

    /**
     * @param name
     * @return
     * @see ubic.gemma.genome.gene.service.GeneSetService#findByName(java.lang.String)
     */
    public abstract Collection<GeneSet> findByName( String name );

    /**
     * @param name
     * @param taxon
     * @return
     */
    public abstract Collection<GeneSet> findByName( String name, Taxon taxon );

    /**
     * Similar to method of same name in GeneSetController.java but here: - no taxon needed - GO groups always searched
     * - GeneSet objects returned instead of GeneSetValueObjects
     * 
     * @param query string to match to a gene set.
     * @param taxonId
     * @return collection of GeneSet
     */
    public abstract Collection<GeneSet> findGeneSetsByName( String query, Long taxonId );

    public abstract Collection<GeneSetValueObject> findByPhenotypeName( String phenotypeQuery, Taxon taxon );

}