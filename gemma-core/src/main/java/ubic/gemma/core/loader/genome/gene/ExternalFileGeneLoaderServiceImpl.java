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
package ubic.gemma.core.loader.genome.gene;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.gemma.persistence.service.genome.gene.GeneService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.gene.GeneProductValueObject;
import ubic.gemma.persistence.persister.Persister;
import ubic.gemma.persistence.service.genome.gene.GeneProductService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import java.io.*;
import java.util.Collection;

/**
 * Class to provide functionality to load genes from a tab delimited file. Typical usage is for non model organisms that
 * do not have genes in NCBI. Supports loading genes against a non species taxon such as a family e.g Salmonids. File
 * format is : Optional header which should be appended with a # to indicate not to process this line Then a line
 * containing 3 fields which should be 'Gene Symbol' 'Gene Name' 'UniProt id' (last is optional) separated by tabs. The
 * Class reads the file and looping through each line creates a gene (NCBI id is null) and one associated gene product.
 * The gene is populated with gene symbol, gene name, gene official name (gene symbol) and a description indicating that
 * this gene has been loaded from a text file. Then gene is associated with a gene product bearing the same name as the
 * gene symbol and persisted.
 *
 * @author ldonnison
 */
@Component
public class ExternalFileGeneLoaderServiceImpl implements ExternalFileGeneLoaderService {
    private static final Log log = LogFactory.getLog( ExternalFileGeneLoaderServiceImpl.class.getName() );

    @Autowired
    private GeneService geneService;

    @Autowired
    private Persister persisterHelper;

    @Autowired
    private TaxonService taxonService;

    @Autowired
    private GeneProductService geneProductService;

    @Override
    public int load( InputStream geneInputStream, String taxonName ) throws Exception {
        BufferedReader b = new BufferedReader( new InputStreamReader( geneInputStream ) );
        Taxon taxon = validateTaxon( taxonName );
        log.info( "Taxon and file validation passed for taxon " + taxonName );
        return load( b, taxon );
    }

    @Override
    public int load( String geneFile, String taxonName ) throws Exception {

        log.info( "Starting loading gene file " + geneFile + " for taxon " + taxonName );
        Taxon taxon = validateTaxon( taxonName );
        log.info( "Taxon and file validation passed for " + geneFile + " for taxon " + taxonName );

        try (BufferedReader bufferedReaderGene = readFile( geneFile )) {

            return load( bufferedReaderGene, taxon );
        }
    }

    /**
     * Creates a gene, where gene name and official gene symbol is set to gene symbol(from file) and official name is
     * set to geneName(from file). The gene description is set to a message indicating that the gene was imported from
     * an external file and the associated uniprot id.
     * If the gene already exists, then it is not modified, unless it lacks a gene product. In that case we add one and
     * return it.
     *
     * @param  fields A string array containing gene symbol, gene name and uniprot id.
     * @param  taxon  Taxon relating to gene
     * @return        Gene with associated gene product for loading into Gemma. Null if no gene was loaded (exists, or
     *                invalid
     *                fields) or modified.
     */
    private Gene createGene( String[] fields, Taxon taxon ) {

        assert fields.length > 1;

        String geneSymbol = fields[0];
        String geneName = fields[1];
        String uniProt = "";
        if ( fields.length > 2 )
            uniProt = fields[2];
        Gene gene;
        // need at least the gene symbol and gene name
        if ( StringUtils.isBlank( geneSymbol ) || StringUtils.isBlank( geneName ) ) {
            log.warn( "Line did not contain valid gene information; GeneSymbol=" + geneSymbol + "GeneName=" + geneName
                    + " UniProt=" + uniProt );
            return null;
        }

        if ( log.isDebugEnabled() )
            log.debug( "Creating gene " + geneSymbol );
        gene = geneService.findByOfficialSymbol( geneSymbol, taxon );

        if ( gene != null ) {
            Collection<GeneProductValueObject> existingProducts = geneService.getProducts( gene.getId() );
            if ( existingProducts.isEmpty() ) {
                log.warn( "Gene " + gene + " exists, but has no products; adding one" );
                gene = geneService.thawOrFail( gene );
                GeneProduct newgp = createGeneProduct( gene );
                newgp = geneProductService.create( newgp );
                gene.getProducts().add( newgp );
                geneService.update( gene );
                return gene;
            }
            log.info( gene + " already exists and is valid, will not update" );
            return null; // no need to create it, though we ignore the name.

        }

        gene = Gene.Factory.newInstance();
        gene.setName( geneSymbol );
        gene.setOfficialSymbol( geneSymbol );
        gene.setOfficialName( StringUtils.lowerCase( geneName ) );
        gene.setDescription( "Imported from external annotation file" );
        gene.setTaxon( taxon );
        gene.getProducts().add( createGeneProduct( gene ) );
        gene = ( Gene ) persisterHelper.persistOrUpdate( gene );
        return gene;
    }

    /**
     * When loading genes with a file each gene will have just 1 gene product. The gene product is a filler taking its
     * details from the gene.
     *
     * @param  gene The gene associated to this gene product
     * @return      Collection of gene products in this case just 1.
     */
    private GeneProduct createGeneProduct( Gene gene ) {
        GeneProduct geneProduct = GeneProduct.Factory.newInstance();
        geneProduct.setGene( gene );
        geneProduct.setName( gene.getName() );
        geneProduct.setDescription( "Gene product placeholder" );
        return geneProduct;
    }

    private int load( BufferedReader bufferedReaderGene, Taxon taxon ) throws IOException {
        int loadedGeneCount = 0;
        String line;
        int linesSkipped = 0;
        while ( ( line = bufferedReaderGene.readLine() ) != null ) {
            String[] lineContents = readLine( line );
            if ( lineContents != null ) {

                Gene gene = createGene( lineContents, taxon );
                if ( gene != null ) {
                    loadedGeneCount++;
                } else {
                    linesSkipped++;
                }
            }
        }
        updateTaxonWithGenesLoaded( taxon );
        log.info( "Genes loaded: " + loadedGeneCount + ", lines skipped: " + linesSkipped );
        return loadedGeneCount;
    }

    /**
     * Creates a bufferedReader for gene file.
     *
     * @param  geneFile    GeneFile including full path
     * @return             BufferedReader The bufferedReader for gene file.
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
     * @param  line        A line from the gene file
     * @return             Array of strings representing a line in a gene file.
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
        if ( fields.length < 2 ) {
            throw new IOException( "Illegal format, expected at least 2 columns, got " + fields.length );
        }
        return fields;

    }

    /**
     * Method to update taxon to indicate that genes have been loaded for that taxon.
     *
     * @param taxon The taxon to update
     */
    private void updateTaxonWithGenesLoaded( Taxon taxon ) {
        if ( !taxon.getIsGenesUsable() ) {
            taxon.setIsGenesUsable( true );
            taxonService.update( taxon );
            log.info( "Updating taxon genes loaded to true for taxon " + taxon );
        }

    }

    /**
     * Method to validate that taxon is held in system.
     *
     * @param  taxonName                Taxon common name
     * @return                          Full Taxon details
     * @throws IllegalArgumentException If taxon is not found in the system.
     */
    private Taxon validateTaxon( String taxonName ) throws IllegalArgumentException {
        Taxon taxon = taxonService.findByCommonName( taxonName );
        if ( taxon == null ) {
            throw new IllegalArgumentException( "No taxon with common name " + taxonName + " found" );
        }
        return taxon;
    }

}
