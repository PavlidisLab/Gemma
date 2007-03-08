/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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


package ubic.gemma.apps;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import ubic.gemma.model.association.Gene2GOAssociationService;
import ubic.gemma.model.common.description.OntologyEntry;
import ubic.gemma.model.common.description.OntologyEntryService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.util.AbstractSpringAwareCLI;

/**
 * <hr>
 * <p>
 * Copyright (c) 2006 UBC Pavlab
 * 
 * @author meeta
 * @version $Id$
 */
public class ComputeGoOverlapCli extends AbstractSpringAwareCLI {

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#buildOptions()
     */

    protected void buildOptions() {
        // TODO Auto-generated method stub

    }

    // A list of service beans
    private GeneService geneService;
    private Gene2GOAssociationService gene2GOAssociationService;
    private OntologyEntryService ontologyEntryService;
    private TaxonService taxonService;

    private Map<Gene, HashSet<OntologyEntry>> geneOntologyTerms = new HashMap<Gene, HashSet<OntologyEntry>>();

    // a hashmap for each gene and its GO terms + parents

    protected void initBeans() {
        geneService = ( GeneService ) getBean( "geneService" );
        gene2GOAssociationService = ( Gene2GOAssociationService ) getBean( "gene2GOAssociationService" );
        ontologyEntryService = ( OntologyEntryService ) getBean( "ontologyEntryService" );
        taxonService = ( TaxonService ) getBean( "taxonService" );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#doWork(java.lang.String[])
     */
    @Override
    protected Exception doWork( String[] args ) {

        Exception err = processCommandLine( "Computer Go Overlap ", args );
        if ( err != null ) return err;

        initBeans();

        String goID = "GO:0007268";
        String commonName = "mouse";

        Collection<Gene> masterGenes = getGeneObject( goID, commonName );

        Set<Gene> allGenes = new HashSet<Gene>( masterGenes );
        // a hashset containing all genes (master genes and coexpressed genes)

        final int stringincy = 6;

        Map<Gene, Collection<Gene>> geneExpMap = new HashMap<Gene, Collection<Gene>>();
        // a hashmap for each gene and its collection of expressed genes

        for ( Gene gene : masterGenes ) {

            log.debug( "I'm here: " + gene );
            Map<Long, Collection<Long>> geneMap = geneService.getCoexpressedGeneMap( stringincy, gene );

            Collection<Gene> foundGenes = new HashSet<Gene>();
            for ( long id : geneMap.keySet() ) {
                foundGenes.add( geneService.load( id ) );
            }

            // for each gene get the coexpressed genes and also add the genes to the allGenes Set

            geneExpMap.put( gene, foundGenes );
            allGenes.addAll( foundGenes );

        }

        // log.debug( "The following genes: " + allGenes );

        for ( Gene gene : allGenes ) {

            HashSet<OntologyEntry> GOTermSet = new HashSet<OntologyEntry>();
            GOTermSet = getGOTerms( gene );
            geneOntologyTerms.put( gene, GOTermSet );

        }

        Map<Gene, Map<Gene, Integer>> masterTermCountMap = new HashMap<Gene, Map<Gene, Integer>>();
        // a hashmap for each gene and its map to each of its expressed
        Set<Gene> set = geneExpMap.keySet();

        for ( Gene masterGene : set ) {
            log.debug( "I'm here: " + masterGene.getOfficialSymbol() );

            Collection<OntologyEntry> masterGO = geneOntologyTerms.get( masterGene );
            // for each (key master gene) obtain set of Ontology terms

            Collection<Gene> coExpGene = geneExpMap.get( masterGene );
            // for that same key (master gene) obtain collection of coexpressed genes

            masterTermCountMap.put( masterGene, computeOverlap( masterGO, coExpGene ) );

        }

        try {
            Writer write = initOutputFile( "synaptic_transmission[2]" );
            String masterGene;
            String geneCoexpressed;
            int overlap;

            for ( Gene g : masterTermCountMap.keySet() ) {
                masterGene = g.getOfficialSymbol();
                Map<Gene, Integer> coexpressed = masterTermCountMap.get( g );
                int masterGOTerms = ( geneOntologyTerms.get( g ) ).size();

                for ( Gene coexpG : coexpressed.keySet() ) {
                    geneCoexpressed = coexpG.getOfficialSymbol();
                    overlap = coexpressed.get( coexpG );

                    int coExpGOTerms;
                    if ( geneOntologyTerms.get( coexpG ) == null )
                        coExpGOTerms = 0;
                    else
                        coExpGOTerms = ( geneOntologyTerms.get( coexpG ) ).size();

                    Collection<OntologyEntry> goTerms = getTermOverlap( g, coexpG );

                    writeOverlapLine( write, masterGene, geneCoexpressed, overlap, goTerms, masterGOTerms, coExpGOTerms );

                }

            }

            // printResults(masterTermCountMap);
        } catch ( IOException ioe ) {
            log.error( "Couldn't write to file: " + ioe );

        }
        return null;

    }

    private Collection<OntologyEntry> getTermOverlap( Gene g, Gene coexpG ) {

        Collection<OntologyEntry> masterGO = geneOntologyTerms.get( g );
        Collection<OntologyEntry> coExpGO = geneOntologyTerms.get( coexpG );
        Collection<OntologyEntry> overlapTerms = new HashSet<OntologyEntry>();

        if ( ( coExpGO == null ) || coExpGO.isEmpty() ) return null;

        if ( ( masterGO == null ) || masterGO.isEmpty() ) return null;

        for ( OntologyEntry ontologyEntry : masterGO ) {
            for ( OntologyEntry ontologyEntryC : coExpGO ) {

                if ( ontologyEntry.getAccession().equalsIgnoreCase( ontologyEntryC.getAccession() ) )
                    overlapTerms.add( ontologyEntry );
            }
        }

        return overlapTerms;
    }

    private void printResults( Map<Gene, Map<Gene, Integer>> masterTermCountMap ) {

        for ( Gene g : masterTermCountMap.keySet() ) {
            log.info( "Master Gene: " + g.getOfficialSymbol() );
            Map<Gene, Integer> coexpressed = masterTermCountMap.get( g );
            for ( Gene coexpG : coexpressed.keySet() ) {
                log.info( "-------- Coexpressed Gene:" + coexpG.getOfficialSymbol() + "   OverLap: "
                        + coexpressed.get( coexpG ) );

            }
            log.info( "=============" );
        }
    }

    /**
     * @param geneOntologyTerms
     * @param masterGene
     * @param masterGO
     * @param coExpGene
     * @return a map of each gene pair (gene + coexpressed gene) mapped to its GOterm overlap value If the collection of
     *         go terms passed in is null this method returns null.
     */
    private Map<Gene, Integer> computeOverlap( Collection<OntologyEntry> masterGO, Collection<Gene> coExpGene ) {

        Map<Gene, Integer> ontologyTermCount = new HashMap<Gene, Integer>();

        if ( ( masterGO == null ) || ( masterGO.isEmpty() ) ) return null;

        // for each Go term associated with the master gene compare the GO term for each coexpressed gene
        for ( Gene gene : coExpGene ) {
            Collection<OntologyEntry> coExpGO = geneOntologyTerms.get( gene );
            Integer count = 0;

            if ( ( coExpGO == null ) || coExpGO.isEmpty() )
                count = -1;

            else {
                for ( OntologyEntry ontologyEntry : masterGO ) {
                    for ( OntologyEntry ontologyEntryC : coExpGO ) {

                        if ( ontologyEntry.getAccession().equalsIgnoreCase( ontologyEntryC.getAccession() ) ) ++count;
                    }
                }
                // the count value tells us the number of GO term matches of the coexpressed gene with the master gene
                // put count into a table with the pair of genes
            }

            log.debug( "Term overlap: " + count );
            ontologyTermCount.put( gene, count );
        }

        return ontologyTermCount;
    }

    /**
     * @param Take a gene and return a set of all GO terms including the parents of each GO term
     * @param geneOntologyTerms
     */
    private HashSet<OntologyEntry> getGOTerms( Gene gene ) {

        // log.debug( "I'm here: " + gene.getOfficialSymbol() );
        HashSet<OntologyEntry> allGOTermSet = new HashSet<OntologyEntry>();

        Collection<OntologyEntry> ontEntry = gene2GOAssociationService.findByGene( gene );
        // log.debug( "ontology entry: " + ontEntry.size() );

        if ( ( ontEntry == null ) || ontEntry.isEmpty() ) return null;

        Map<OntologyEntry, Collection<OntologyEntry>> parentMap = ontologyEntryService.getAllParents( ontEntry );

        for ( OntologyEntry oe : parentMap.keySet() ) {
            allGOTermSet.addAll( parentMap.get( oe ) ); // add the parents
            allGOTermSet.add( oe ); // add the child
        }

        // log.debug( "The GOTerm Set: " + allGOTermSet );

        return allGOTermSet;
    }

    /**
     * @param ids
     * @return a set of gene objects associated with the query GOID
     */
    private Collection<Gene> getGeneObject( String goID, String commonName ) {

        Taxon taxon = taxonService.findByCommonName( commonName );
        return gene2GOAssociationService.findByGOTerm( goID, taxon );
        
    }

    public static void main( String[] args ) {
        ComputeGoOverlapCli p = new ComputeGoOverlapCli();
        try {
            Exception ex = p.doWork( args );
            if ( ex != null ) {
                ex.printStackTrace();
            }
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * Opens a file for writing anda adds the header.
     * 
     * @param fileName if Null, output will be written to standard output.
     * @throws IOException
     */
    protected Writer initOutputFile( String fileName ) throws IOException {

        Writer writer;
        if ( StringUtils.isBlank( fileName ) ) {
            log.info( "Output to stdout" );
            writer = new PrintWriter( System.out );
        } else {

            // write into file
            log.info( "Creating new annotation file " + fileName + " \n" );

            File f = new File( fileName + ".txt" );

            if ( f.exists() ) {
                log.warn( "Will overwrite existing file " + f );
                f.delete();
            }

            f.createNewFile();
            writer = new FileWriter( f );
        }

        writer.write( "Master Gene \t GO Terms \t Coexpressed Gene \t GO Terms \t Term Overlap \t GO Terms\n" );

        return writer;
    }

    protected void writeOverlapLine( Writer writer, String masterGene, String geneCoexpressed, int overlap,
            Collection<OntologyEntry> goTerms, int masterGOTerms, int coExpGOTerms ) throws IOException {

        if ( log.isDebugEnabled() ) log.debug( "Generating line for annotation file  \n" );

        if ( masterGene == null ) masterGene = "";

        if ( geneCoexpressed == null ) geneCoexpressed = "";

        writer.write( masterGene + "\t" + masterGOTerms + "\t" + geneCoexpressed + "\t" + coExpGOTerms + "\t" + overlap
                + "\t" );

        if ( ( goTerms == null ) || goTerms.isEmpty() ) {
            writer.write( "\n" );
            writer.flush();
            return;
        }

        boolean wrote = false;

        for ( OntologyEntry oe : goTerms ) {

            if ( oe == null ) continue;

            if ( wrote )
                writer.write( "|" + oe.getAccession() );
            else
                writer.write( oe.getAccession() );

            wrote = true;

        }

        writer.write( "\n" );
        writer.flush();

    }

}
