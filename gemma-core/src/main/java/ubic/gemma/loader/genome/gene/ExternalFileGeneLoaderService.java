/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.loader.genome.gene;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.gene.GeneProductType;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.persistence.PersisterHelper;

/**
 * Class to provide functionality to load genes from a tab delimited file. Typical usage is for non model organisms that
 * do not have genes in NCBI. Supports loading genes against a non species taxon such as a family e.g Salmonids. File
 * format is : Optional header which should be appended with a # to indicate not to process this line Then a line
 * containing 3 fields which should be 'Gene Symbol' 'Gene Name' 'UniProt id' separated by tabs. The Class reads the
 * file and looping through each line creates a gene (NCBI id is null) and one associated gene product. The gene is
 * populated with gene symbol, gene name, gene official name (gene symbol) and a description indicating that this gene
 * has been loaded from a text file. Then gene is associated with a gene product bearing the same name as the gene
 * symbol and persisted.
 * 
 * @author ldonnison
 * @version $Id$
 */
@Service
public class ExternalFileGeneLoaderService {
    private static Log log = LogFactory.getLog( ExternalFileGeneLoaderService.class.getName() );
    private int loadedGeneCount = 0;

    @Autowired
    private PersisterHelper persisterHelper;

    @Autowired
    private TaxonService taxonService;

    @Autowired
    private GeneService geneService;

    /**
     * Creates a gene, where gene name and official gene symbol is set to gene symbol(from file) and official name is
     * set to geneName(from file). The gene description is set to a message indicating that the gene was imported from
     * an external file and the associated uniprot id.
     * 
     * @param fields A string array containing gene symbol, gene name and uniprot id.
     * @param taxon Taxon relating to gene
     * @return Gene with associated gene product for loading into Gemma.
     */
    public Gene createGene( String[] fields, Taxon taxon ) {
        String geneSymbol = fields[0];
        String geneName = fields[1];
        String uniProt = fields[2];
        Gene gene = null;
        // need at least the gene symbol and gene name
        if ( !StringUtils.isBlank( geneSymbol ) && !StringUtils.isBlank( geneName ) ) {
            log.debug( "Creating gene " + geneSymbol );
            gene = geneService.findByOfficialSymbol( geneSymbol, taxon );
            if ( gene != null ) return null; // no need to create it.
            gene = Gene.Factory.newInstance();
            gene.setName( geneSymbol );
            gene.setOfficialSymbol( geneSymbol );
            gene.setOfficialName( StringUtils.lowerCase( geneName ) );
            gene.setDescription( "Imported from external annotation file" );
            gene.setTaxon( taxon );
            gene.setProducts( createGeneProducts( gene ) );
        } else {
            log.warn( "Line does not contain valid gene information; GeneSymbol is: " + geneSymbol + "GeneName is: "
                    + geneName + " Uni Prot id is  " + uniProt );
        }
        return gene;
    }

    /**
     * When loading genes with a file each gene will have just 1 gene product. The gene product is a filler taking its
     * details from the gene.
     * 
     * @param gene The gene associated to this gene product
     * @return Collection of gene products in this case just 1.
     */
    public Collection<GeneProduct> createGeneProducts( Gene gene ) {
        Collection<GeneProduct> geneProducts = new HashSet<GeneProduct>();
        GeneProduct geneProduct = GeneProduct.Factory.newInstance();
        geneProduct.setType( GeneProductType.RNA );
        geneProduct.setGene( gene );
        geneProduct.setName( gene.getName() );
        geneProduct.setDescription( "Gene product placeholder" );
        geneProducts.add( geneProduct );
        return geneProducts;
    }

    /**
     * Number of genes successfully loaded.
     * 
     * @return the loadedGeneCount
     */
    public int getLoadedGeneCount() {
        return loadedGeneCount;
    }

    /**
     * Work flow is: The file is first checked to see if readable, and the taxon checked to see it is in Gemma. If
     * validation passes the file is read line by line. Each line equates to a gene and its gene product, which is
     * created from the file details. The gene is then persisted. If successfully loaded taxon flag isGenesLoaded is set
     * to true to indicate that there are genes loaded for this taxon.
     * 
     * @param geneFile Full path to file containing genes details
     * @param taxon taxonName to be associated to this gene, does not have to be a species.
     * @exception Thrown with a file format error or problem in persisting gene to database.
     */
    public void load( String geneFile, String taxonName ) throws Exception {
        String line = null;
        int linesSkipped = 0;
        log.info( "Starting loading gene file " + geneFile + " for taxon " + taxonName );
        BufferedReader bufferedReaderGene = readFile( geneFile );
        Taxon taxon = validateTaxon( taxonName );
        log.info( "Taxon and file validation passed for " + geneFile + " for taxon " + taxonName );
        while ( ( line = bufferedReaderGene.readLine() ) != null ) {
            String[] lineContents = readLine( line );
            if ( lineContents != null ) {
                Gene gene = createGene( lineContents, taxon );
                if ( gene != null ) {
                    persisterHelper.persistOrUpdate( gene );
                    loadedGeneCount++;
                } else {
                    linesSkipped++;
                }
            }
        }
        updateTaxonWithGenesLoaded( taxon );
        log.info( "Finished loading gene file " + geneFile + " for taxon " + taxon + ". " + " Genes loaded: "
                + loadedGeneCount + " ,Lines skipped: " + linesSkipped );
    }

    /**
     * PersisterHelper bean.
     * 
     * @param persisterHelper the persisterHelper to set
     */
    public void setPersisterHelper( PersisterHelper persisterHelper ) {
        this.persisterHelper = persisterHelper;
    }

    /**
     * Taxon bean
     * 
     * @param bean
     */
    public void setTaxonService( TaxonService bean ) {
        this.taxonService = bean;

    }

    /**
     * Method to update taxon to indicate that genes have been loaded for that taxon. If the taxon has children taxa
     * then those child genes should not be used and the flag for those child taxon set to false.
     * 
     * @param taxon The taxon to update
     * @exception Thrown if error accessing updating taxon details
     */
    public void updateTaxonWithGenesLoaded( Taxon taxon ) throws Exception {
        Collection<Taxon> childTaxa = taxonService.findChildTaxaByParent( taxon );
        // if this taxon has children flag not to use their genes
        if ( childTaxa != null && !childTaxa.isEmpty() ) {
            for ( Taxon childTaxon : childTaxa ) {
                if ( childTaxon != null && childTaxon.getIsGenesUsable() ) {
                    childTaxon.setIsGenesUsable( false );
                    taxonService.update( childTaxon );
                    log.warn( "Child taxa" + childTaxon + " genes have been loaded parent taxa should superseed" );
                }
            }
        }
        // set taxon flag indicating that use these taxons genes
        if ( !taxon.getIsGenesUsable() ) {
            taxon.setIsGenesUsable( true );
            taxonService.update( taxon );
            log.info( "Updating taxon genes loaded to true for taxon " + taxon );
        }

    }

    /**
     * Method to validate that taxon is held in system.
     * 
     * @param taxonName Taxon common name
     * @return Full Taxon details
     * @exception If taxon is not found in the system.
     */
    public Taxon validateTaxon( String taxonName ) throws IllegalArgumentException {
        Taxon taxon = taxonService.findByCommonName( taxonName );
        if ( taxon == null ) {
            throw new IllegalArgumentException( "No taxon with common name " + taxonName + " found" );
        }
        return taxon;
    }

    /**
     * Creates a bufferedReader for gene file.
     * 
     * @param geneFile GeneFile including full path
     * @return BufferedReader The bufferedReader for gene file.
     * @throws IOException File can not be opened for reading such as does not exist.
     */
    private BufferedReader readFile( String geneFile ) throws IOException {
        File f = new File( geneFile );
        if ( !f.canRead() ) {
            throw new IOException( "Cannot read from " + geneFile );
        }
        BufferedReader b = new BufferedReader( new FileReader( geneFile ) );
        log.info( "File " + geneFile + " read successfully" );
        return b;

    }

    /**
     * Read a gene file line, splitting the line into 3 strings.
     * 
     * @param line A line from the gene file
     * @return Array of strings representing a line in a gene file.
     * @throws IOException Thrown if file is not readable
     */
    private String[] readLine( String line ) throws IOException {
        if ( StringUtils.isBlank( line ) ) {
            return null;
        }
        if ( line.startsWith( "#" ) ) {
            return null;
        }

        String[] fields = StringUtils.splitPreserveAllTokens( line, '\t' );
        if ( fields.length != 3 ) {
            throw new IOException( "Illegal format, expected three columns, got " + fields.length );
        }
        return fields;

    }

}
