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
import ubic.gemma.model.coexpression.CoexpressionCollectionValueObject;
import ubic.gemma.model.coexpression.CoexpressionValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.ontology.GeneOntologyService;
import ubic.gemma.ontology.OntologyTerm;
import ubic.gemma.util.AbstractSpringAwareCLI;

/**
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
    }

    // A list of service beans
    private GeneService geneService;
    private Gene2GOAssociationService gene2GOAssociationService;
    private GeneOntologyService ontologyEntryService;
    private TaxonService taxonService;

    private Map<Gene, Set<OntologyTerm>> geneOntologyTerms = new HashMap<Gene, Set<OntologyTerm>>();

    // a hashmap for each gene and its GO terms + parents

    protected void initBeans() {
        geneService = ( GeneService ) getBean( "geneService" );
        gene2GOAssociationService = ( Gene2GOAssociationService ) getBean( "gene2GOAssociationService" );
        ontologyEntryService = ( GeneOntologyService ) getBean( "geneOntologyService" );
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

        Set<Gene> coExpGenes = new HashSet<Gene>();
        // a hashset containing all coexpressed genes

        final int stringincy = 6;

        Map<Gene, Collection<Gene>> geneExpMap = new HashMap<Gene, Collection<Gene>>();
        // a hashmap for each gene and its collection of expressed genes

        log.debug( "Total master genes:" + masterGenes.size() );

        for ( Gene gene : masterGenes ) {

            log.debug( "I'm here: " + gene );
            CoexpressionCollectionValueObject coexpressed = ( CoexpressionCollectionValueObject ) geneService
                    .getCoexpressedGenes( gene, null, stringincy );

            Collection<Long> geneIds = new HashSet<Long>();
            for ( CoexpressionValueObject co : coexpressed.getCoexpressionData() ) {
                geneIds.add( co.getGeneId() );
            }

            Collection<Gene> foundGenes = new HashSet<Gene>();
            for ( long id : geneIds ) {
                foundGenes.add( geneService.load( id ) );
            }

            // for each gene get the coexpressed genes and also add the genes to the allGenes Set

            geneExpMap.put( gene, foundGenes );
            allGenes.addAll( foundGenes );
            coExpGenes.addAll( foundGenes );

        }

        log.debug( "Total coexpressed genes:" + coExpGenes.size() );
        // log.debug( "The following genes: " + allGenes.size() );

        // new GOTermOverlap code
        Set<OntologyTerm> allGOTerms = new HashSet<OntologyTerm>();
        Map<OntologyTerm, Double> GOProbMap = new HashMap<OntologyTerm, Double>();

        Taxon mouse = taxonService.findByCommonName( commonName );
        Collection<Gene> mouseGenes = geneService.getGenesByTaxon( mouse );
        Map<Gene, Set<OntologyTerm>> mouseGeneGOMap = new HashMap<Gene, Set<OntologyTerm>>();

        log.debug( "I'm here" );

        for ( Gene gene : mouseGenes ) {
            Set<OntologyTerm> GOTerms = getGOTerms( gene );
            mouseGeneGOMap.put( gene, GOTerms );
            if ( GOTerms != null ) allGOTerms.addAll( GOTerms );
        }

        mouseGenes = null;
        // Calculating the probability of each term
        int total = allGOTerms.size();
        log.info( "Total number of GO terms: " + total );

        for ( OntologyTerm ontE : allGOTerms ) {
            int termCount = 0;

            for ( Gene mouseGene : mouseGeneGOMap.keySet() ) {
                Collection<OntologyTerm> GO = mouseGeneGOMap.get( mouseGene );
                if ( ( GO == null ) || GO.isEmpty() ) continue;

                for ( OntologyTerm ontM : GO ) {
                    if ( ontE.equals( ontM ) ) ++termCount;
                }
            }

            log.info( "The termCount is: " + termCount );
            double termProb = ( ( double ) termCount / ( double ) total );
            log.info( "The probability is: " + termProb );
            GOProbMap.put( ontE, termProb );

        }
        // Map<Gene, Map<Gene, Integer>> masterTermCountMap = new HashMap<Gene, Map<Gene, Integer>>();

        Map<Gene, Map<Gene, Double>> masterTermCountMap = new HashMap<Gene, Map<Gene, Double>>();
        // a hashmap for each gene and its map to each of its expressed
        Set<Gene> set = geneExpMap.keySet();

        for ( Gene masterGene : set ) {
            // log.debug( "I'm here: " + masterGene.getOfficialSymbol() );

            Collection<OntologyTerm> masterGO = geneOntologyTerms.get( masterGene );
            // for each (key master gene) obtain set of Ontology terms
            Collection<Gene> coExpGene = geneExpMap.get( masterGene );
            // for that same key (master gene) obtain collection of coexpressed genes

            // masterTermCountMap.put( masterGene, computeOverlap( masterGO, coExpGene ) );
            masterTermCountMap.put( masterGene, computeResnikOverlap( masterGO, coExpGene, GOProbMap ) );
        }

        try {
            Writer write = initOutputFile( "synaptic_transmission" );
            String masterGene;
            String geneCoexpressed;
            double overlap;
            for ( Gene g : masterTermCountMap.keySet() ) {
                masterGene = g.getOfficialSymbol();
                Map<Gene, Double> coexpressed = masterTermCountMap.get( g );
                int masterGOTerms = ( geneOntologyTerms.get( g ) ).size();

                for ( Gene coexpG : coexpressed.keySet() ) {
                    geneCoexpressed = coexpG.getOfficialSymbol();
                    overlap = coexpressed.get( coexpG );
                    int coExpGOTerms;
                    if ( geneOntologyTerms.get( coexpG ) == null )
                        coExpGOTerms = 0;
                    else
                        coExpGOTerms = ( geneOntologyTerms.get( coexpG ) ).size();

                    Collection<OntologyTerm> goTerms = getTermOverlap( g, coexpG );
                    writeOverlapLine( write, masterGene, geneCoexpressed, overlap, goTerms, masterGOTerms, coExpGOTerms );
                }
            }

            // printResults(masterTermCountMap);
        } catch ( IOException ioe ) {
            log.error( "Couldn't write to file: " + ioe );

        }
        return null;

    }

    private Collection<OntologyTerm> getTermOverlap( Gene g, Gene coexpG ) {

        Collection<OntologyTerm> masterGO = geneOntologyTerms.get( g );
        Collection<OntologyTerm> coExpGO = geneOntologyTerms.get( coexpG );
        Collection<OntologyTerm> overlapTerms = new HashSet<OntologyTerm>();

        if ( ( coExpGO == null ) || coExpGO.isEmpty() ) return null;

        if ( ( masterGO == null ) || masterGO.isEmpty() ) return null;

        for ( OntologyTerm ontologyEntry : masterGO ) {
            for ( OntologyTerm ontologyEntryC : coExpGO ) {

                if ( ontologyEntry.equals( ontologyEntryC ) ) overlapTerms.add( ontologyEntry );
            }
        }

        return overlapTerms;
    }

    // private void printResults( Map<Gene, Map<Gene, Integer>> masterTermCountMap ) {
    //
    // for ( Gene g : masterTermCountMap.keySet() ) {
    // log.info( "Master Gene: " + g.getOfficialSymbol() );
    // Map<Gene, Integer> coexpressed = masterTermCountMap.get( g );
    // for ( Gene coexpG : coexpressed.keySet() ) {
    // log.info( "-------- Coexpressed Gene:" + coexpG.getOfficialSymbol() + " OverLap: "
    // + coexpressed.get( coexpG ) );
    //
    // }
    // log.info( "=============" );
    // }
    // }

    /**
     * @param geneOntologyTerms
     * @param masterGene
     * @param masterGO
     * @param coExpGene
     * @return a map of each gene pair (gene + coexpressed gene) mapped to its GOterm overlap value If the collection of
     *         go terms passed in is null this method returns null.
     */
    private Map<Gene, Integer> computeOverlap( Collection<OntologyTerm> masterGO, Collection<Gene> coExpGene ) {

        Map<Gene, Integer> ontologyTermCount = new HashMap<Gene, Integer>();

        if ( ( masterGO == null ) || ( masterGO.isEmpty() ) ) return null;

        // for each Go term associated with the master gene compare the GO term for each coexpressed gene
        for ( Gene gene : coExpGene ) {
            Collection<OntologyTerm> coExpGO = geneOntologyTerms.get( gene );
            Integer count = 0;

            if ( ( coExpGO == null ) || coExpGO.isEmpty() )
                count = -1;

            else {
                for ( OntologyTerm ontologyEntry : masterGO ) {
                    for ( OntologyTerm ontologyEntryC : coExpGO ) {

                        if ( ontologyEntry.equals( ontologyEntryC ) ) ++count;
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

    private int newmethod( int help ) {

        return 1;

    }

    /**
     * @param masterGO
     * @param coExpGene
     * @param GOProbMap
     * @return This will compute the GO overlap using Resnik's similarity score, using the calculated value of the
     *         probability of the minimum subsumer.
     */
    private Map<Gene, Double> computeResnikOverlap( Collection<OntologyTerm> masterGO, Collection<Gene> coExpGene,
            Map<OntologyTerm, Double> GOProbMap ) {

        Map<Gene, Double> ontologyTermCount = new HashMap<Gene, Double>();

        if ( ( masterGO == null ) || ( masterGO.isEmpty() ) ) return null;

        // // for each Go term associated with the master gene compare the GO term for each coexpressed gene
        for ( Gene gene : coExpGene ) {
            Collection<OntologyTerm> coExpGO = geneOntologyTerms.get( gene );
            Integer count = 0;
            double threshold = 1;

            if ( ( coExpGO == null ) || coExpGO.isEmpty() )
                continue;

            else {

                for ( OntologyTerm ontoM : masterGO ) {
                    for ( OntologyTerm ontoC : coExpGO ) {

                        if ( ontoM.equals( ontoC ) ) {
                            double pValue = GOProbMap.get( ontoM );
                            if ( pValue < threshold ) threshold = pValue;
                        }
                    }
                }
                // // the count value tells us the number of GO term matches of the coexpressed gene with the master
                // gene
                // // put count into a table with the pair of genes
            }

            double score = -1 * ( StrictMath.log10( threshold ) );
            // log.debug( "Term score: " + score );
            ontologyTermCount.put( gene, score );
        }

        return ontologyTermCount;
    }

    /**
     * @param Take a gene and return a set of all GO terms including the parents of each GO term
     * @param geneOntologyTerms
     */
    @SuppressWarnings("unchecked")
    private Set<OntologyTerm> getGOTerms( Gene gene ) {

        // log.debug( "I'm here: " + gene.getOfficialSymbol() );
        Set<OntologyTerm> allGOTermSet = new HashSet<OntologyTerm>();

        Collection<OntologyTerm> terms = gene2GOAssociationService.findByGene( gene );
        // log.debug( "ontology entry: " + ontEntry.size() );

        if ( ( terms == null ) || terms.isEmpty() ) return null;

        Collection<OntologyTerm> parents = ontologyEntryService.getAllParents( terms );
        allGOTermSet.addAll( parents ); // add the parents
        allGOTermSet.addAll( terms ); // add the children

        Set<OntologyTerm> finalGOTermSet = new HashSet<OntologyTerm>();
        for ( OntologyTerm oe : allGOTermSet ) {

            String id = GeneOntologyService.asRegularGoId( oe );

            if ( ( id.equalsIgnoreCase( "GO:0005575" ) ) || ( id.equalsIgnoreCase( "GO:0008150" ) )
                    || ( id.equalsIgnoreCase( "GO:0003674" ) ) ) {
                log.info( "Removing Ontology entry" );
                continue;
            }

            finalGOTermSet.add( oe );
        }

        return finalGOTermSet;
    }

    /**
     * @param ids
     * @return a set of gene objects associated with the query GOID
     */
    @SuppressWarnings("unchecked")
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

    protected void writeOverlapLine( Writer writer, String masterGene, String geneCoexpressed, double overlap,
            Collection<OntologyTerm> goTerms, int masterGOTerms, int coExpGOTerms ) throws IOException {

        if ( log.isDebugEnabled() ) log.debug( "Generating line for annotation file \n" );

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

        for ( OntologyTerm oe : goTerms ) {
            if ( oe == null ) continue;
            if ( wrote )
                writer.write( "|" + GeneOntologyService.asRegularGoId( oe ) );
            else
                writer.write( GeneOntologyService.asRegularGoId( oe ) );
            wrote = true;
        }

        writer.write( "\n" );
        writer.flush();
    }

}
