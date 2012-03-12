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
package ubic.gemma.loader.genome.gene;

import java.util.Collection;

import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneProduct;

/**
 * @author paul
 * @version $Id$
 */
public interface ExternalFileGeneLoaderService {

    /**
     * Creates a gene, where gene name and official gene symbol is set to gene symbol(from file) and official name is
     * set to geneName(from file). The gene description is set to a message indicating that the gene was imported from
     * an external file and the associated uniprot id.
     * 
     * @param fields A string array containing gene symbol, gene name and uniprot id.
     * @param taxon Taxon relating to gene
     * @return Gene with associated gene product for loading into Gemma.
     */
    public abstract Gene createGene( String[] fields, Taxon taxon );

    /**
     * When loading genes with a file each gene will have just 1 gene product. The gene product is a filler taking its
     * details from the gene.
     * 
     * @param gene The gene associated to this gene product
     * @return Collection of gene products in this case just 1.
     */
    public abstract Collection<GeneProduct> createGeneProducts( Gene gene );

    /**
     * Work flow is: The file is first checked to see if readable, and the taxon checked to see it is in Gemma. If
     * validation passes the file is read line by line. Each line equates to a gene and its gene product, which is
     * created from the file details. The gene is then persisted. If successfully loaded taxon flag isGenesLoaded is set
     * to true to indicate that there are genes loaded for this taxon.
     * 
     * @param geneFile Full path to file containing genes details
     * @param taxon taxonName to be associated to this gene, does not have to be a species.
     * @return number of genes loaded
     * @exception Thrown with a file format error or problem in persisting gene to database.
     */
    public abstract int load( String geneFile, String taxonName ) throws Exception;

    /**
     * Method to update taxon to indicate that genes have been loaded for that taxon. If the taxon has children taxa
     * then those child genes should not be used and the flag for those child taxon set to false.
     * 
     * @param taxon The taxon to update
     * @exception Thrown if error accessing updating taxon details
     */
    public abstract void updateTaxonWithGenesLoaded( Taxon taxon ) throws Exception;

    /**
     * Method to validate that taxon is held in system.
     * 
     * @param taxonName Taxon common name
     * @return Full Taxon details
     * @exception If taxon is not found in the system.
     */
    public abstract Taxon validateTaxon( String taxonName ) throws IllegalArgumentException;

}