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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;

import ubic.gemma.model.association.Gene2GOAssociationService;
import ubic.gemma.model.coexpression.CoexpressionCollectionValueObject;
import ubic.gemma.model.coexpression.CoexpressionValueObject;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.ontology.GeneOntologyService;
import ubic.gemma.ontology.OntologyTerm;
import ubic.gemma.util.AbstractSpringAwareCLI;
import ubic.gemma.util.ConfigUtils;

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

    private Map<Long, Collection<String>> mouseGeneGOMap = new HashMap<Long, Collection<String>>();
    // a hashmap for each mouse gene and its GO terms + parents

    private Map<String, Integer> rootMap = new HashMap<String, Integer>();
    Map<String, Integer> GOcountMap = new HashMap<String, Integer>();

    private String HASH_MAP_RETURN = "HashMapReturn";
    private String HOME_DIR = ConfigUtils.getString( "gemma.appdata.home" );
    private boolean partOf = true;

    private String proccess = "GO:0008150";
    private String function = "GO:0003674";
    private String component = "GO:0005575";
    private int pCount = 0;
    private int fCount = 0;
    private int cCount = 0;

    protected void initBeans() {
        geneService = ( GeneService ) getBean( "geneService" );
        gene2GOAssociationService = ( Gene2GOAssociationService ) getBean( "gene2GOAssociationService" );
        ontologyEntryService = ( GeneOntologyService ) getBean( "geneOntologyService" );
        taxonService = ( TaxonService ) getBean( "taxonService" );

        try {
            while ( !ontologyEntryService.isReady() ) {
                log.info( "waiting for ontology load.." );
                Thread.sleep( 1000 );
            }
        } catch ( Exception e ) {
            log.error( "Was unable to suspend this thread and wait for the gene ontology service to ready itself" );
        }

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

        StopWatch overallWatch = new StopWatch();
        overallWatch.start();

        String goID = "GO:0007268";
        String commonName = "mouse";

        Collection<Gene> masterGenes = getGeneObject( goID, commonName );

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

            if ( ( foundGenes == null ) || ( foundGenes.isEmpty() ) )
                continue;
            else {
                geneExpMap.put( gene, foundGenes );
            }
        }

        // new GOTermOverlap code
        Set<String> allGOTerms = new HashSet<String>();
        Map<String, Double> GOProbMap = new HashMap<String, Double>();

        log.info( "Checking for file..." );

        File f = new File( HOME_DIR + File.separatorChar + HASH_MAP_RETURN );
        if ( f.exists() ) {
            mouseGeneGOMap = getMapFromDisk( f );
            log.info( "Found file!" );

            for ( Long gene : mouseGeneGOMap.keySet() ) {
                Collection<String> goIds = mouseGeneGOMap.get( gene );
                allGOTerms.addAll( goIds );
            }
            log.info( "Got allGOTerm set!" );

            makeRootMap( allGOTerms );
        }

        else {

            Taxon mouse = taxonService.findByCommonName( commonName );
            Collection<Gene> mouseGenes = geneService.loadGenes( mouse );

            for ( Gene gene : mouseGenes ) {
                Set<OntologyTerm> GOTerms = getGOTerms( gene );
                if ( GOTerms == null || GOTerms.isEmpty() ) continue;
                log.info( "Got go terms for " + gene.getName() );

                Collection<String> termString = new HashSet<String>();
                for ( OntologyTerm oe : GOTerms ) {
                    termString.add( oe.getUri() );
                }
                allGOTerms.addAll( termString );
                mouseGeneGOMap.put( gene.getId(), termString );

            }
            saveMapToDisk( mouseGeneGOMap, HASH_MAP_RETURN );

        }

        // Calculating the probability of each term
        log.info( "Entering GO Map...." );
        for ( Long gene : mouseGeneGOMap.keySet() ) {
            Collection<String> GO = mouseGeneGOMap.get( gene );
            addTermOccurrence( GO );
        }

        Long overallElapsed = overallWatch.getTime();
        log.info( "Creating GOcountMap took: " + overallElapsed / 1000 + "s " );

        Map<String, Integer> newCountMap = new HashMap<String, Integer>();


        log.info( "Calculating probabilities... " );
        for ( String term : GOcountMap.keySet() ) {
            newCountMap.put( term, countChildren(term) );
        }
        
        log.info( "The total process associations: " + pCount);
        log.info( "The total function associations: " + fCount);
        log.info( "The total component associations: " + cCount);
        
        GOProbMap = getProb( newCountMap );

        this.saveMapToDisk( GOProbMap, "GoProbMap" );
        Long Elapsed = overallWatch.getTime();
        log.info( "Creating GO probability map took: " + Elapsed / 1000 + "s " );

        Map<Gene, Map<Gene, Double>> masterTermCountMap = new HashMap<Gene, Map<Gene, Double>>();
        // a hashmap for each gene and its map to each of its coexpressed gene and the term with min probabaility

        Set<Gene> set = geneExpMap.keySet();

        for ( Gene masterGene : set ) {
            // log.debug( "I'm here: " + masterGene.getOfficialSymbol() );

            Collection<VocabCharacteristic> masterVoc = gene2GOAssociationService.findByGene( masterGene );
            // for each (key master gene) obtain set of Ontology terms
            Collection<Gene> coExpGene = geneExpMap.get( masterGene );
            // for that same key (master gene) obtain collection of coexpressed genes

            // masterTermCountMap.put( masterGene, computeOverlap( masterGO, coExpGene ) );
            masterTermCountMap.put( masterGene, computeLinOverlap( masterVoc, coExpGene, GOProbMap ) );
        }

        try {
            Writer write = initOutputFile( "Lin_overlapResults" );
            String masterGene;
            String geneCoexpressed;
            double overlap;
            for ( Gene g : masterTermCountMap.keySet() ) {
                masterGene = g.getOfficialSymbol();
                Map<Gene, Double> coexpressed = masterTermCountMap.get( g );
                int masterGOTerms = ( mouseGeneGOMap.get( g.getId() ) ).size();

                for ( Gene coexpG : coexpressed.keySet() ) {
                    geneCoexpressed = coexpG.getOfficialSymbol();
                    overlap = coexpressed.get( coexpG );
                    int coExpGOTerms;
                    if ( mouseGeneGOMap.get( coexpG.getId() ) == null )
                        coExpGOTerms = 0;
                    else
                        coExpGOTerms = ( mouseGeneGOMap.get( coexpG.getId() ) ).size();

                    Collection<OntologyTerm> goTerms = getTermOverlap( g, coexpG );
                    writeOverlapLine( write, masterGene, geneCoexpressed, overlap, goTerms, masterGOTerms, coExpGOTerms );
                }
            }
            overallWatch.stop();
            log.info( "Compute GoOverlap takes " + overallWatch.getTime() + "ms" );

            // printResults(masterTermCountMap);
        } catch ( IOException ioe ) {
            log.error( "Couldn't write to file: " + ioe );

        }
        return null;

    }

    private Collection<OntologyTerm> getTermOverlap( Gene g, Gene coexpG ) {

        Collection<String> masterGO = mouseGeneGOMap.get( g.getId() );
        Collection<String> coExpGO = mouseGeneGOMap.get( coexpG.getId() );
        Collection<OntologyTerm> overlapTerms = new HashSet<OntologyTerm>();

        if ( ( coExpGO == null ) || coExpGO.isEmpty() ) return null;

        if ( ( masterGO == null ) || masterGO.isEmpty() ) return null;

        for ( String ontologyEntry : masterGO ) {
            for ( String ontologyEntryC : coExpGO ) {

                if ( ontologyEntry.equalsIgnoreCase( ontologyEntryC ) )
                    overlapTerms.add( GeneOntologyService.getTermForURI( ontologyEntry ) );
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
     * @param masterGO
     * @param coExpGene
     * @param GOProbMap
     * @return This will compute the GO overlap using Lin's similarity score, using the calculated value of the
     *         probability of the minimum subsumer.
     */
    private Map<Gene, Double> computeLinOverlap( Collection<VocabCharacteristic> masterVoc, Collection<Gene> coExpGene,
            Map<String, Double> GOProbMap ) {

        Map<Gene, Double> scoreMap = new HashMap<Gene, Double>();
        Collection<OntologyTerm> masterGO = new HashSet<OntologyTerm>();

        if ( ( masterVoc == null ) || ( masterVoc.isEmpty() ) ) return null;

        for ( VocabCharacteristic characteristic : masterVoc ) {
            OntologyTerm term = GeneOntologyService.getTermForId( characteristic.getValue() );
            if ( ( term != null ) ) masterGO.add( term );
        }

        // // for each Go term associated with the master gene compare the GO term for each coexpressed gene
        for ( Gene gene : coExpGene ) {
            Collection<VocabCharacteristic> coExpVoc = gene2GOAssociationService.findByGene( gene );
            Collection<OntologyTerm> coExpGO = new HashSet<OntologyTerm>();

            for ( VocabCharacteristic characteristic : coExpVoc ) {
                OntologyTerm term = GeneOntologyService.getTermForId( characteristic.getValue() );
                if ( ( term != null ) ) coExpGO.add( term );
            }

            double avgScore = 0;

            if ( ( coExpGO == null ) || coExpGO.isEmpty() ) continue;

            log.info( "calculating overlap for... " + gene );
            double total = 0;
            double score = 0;
            int count = 0;

            for ( OntologyTerm ontoM : masterGO ) {
                double probM = GOProbMap.get( ontoM.getUri() );

                for ( OntologyTerm ontoC : coExpGO ) {
                    if ( !GOProbMap.containsKey( ontoC.getUri() ) ) {
                        log.info( "Go probe map doesn't contain " + ontoC );
                        continue;
                    }
                    Double probC = GOProbMap.get( ontoC.getUri() );
                    Double pmin = 1.0;
                    String pterm = "";

                    if ( ontoM == ontoC ) {
                        pmin = GOProbMap.get( ontoM.getUri() );
                        pterm = ontoM.getTerm();

                    } else {
                        Collection<OntologyTerm> parentM = ontologyEntryService.getAllParents( ontoM, partOf );
                        parentM.add( ontoM );
                        Collection<OntologyTerm> parentC = ontologyEntryService.getAllParents( ontoC, partOf );
                        parentC.add( ontoC );

                        for ( OntologyTerm termM : parentM ) {
                            String id = GeneOntologyService.asRegularGoId( termM );
                            if ( ( id.equalsIgnoreCase( proccess ) ) || ( id.equalsIgnoreCase( function ) )
                                    || ( id.equalsIgnoreCase( component ) ) ) continue;

                            for ( OntologyTerm termC : parentC ) {
                                String id2 = GeneOntologyService.asRegularGoId( termM );
                                if ( ( id2.equalsIgnoreCase( proccess ) ) || ( id2.equalsIgnoreCase( function ) )
                                        || ( id2.equalsIgnoreCase( component ) ) ) continue;

                                if ( ( termM.getUri().equalsIgnoreCase( termC.getUri() ) )
                                        && ( GOProbMap.get( termM.getUri() ) != null ) ) {

                                    double value = GOProbMap.get( termM.getUri() );
                                    if ( value < pmin ) {
                                        pmin = value;
                                        pterm = termM.getTerm();
                                    }
                                }
                            }
                        }
                    }

                    if ( pmin < 1 ) {
                        score = ( 2 * ( StrictMath.log10( pmin ) ) )
                                / ( ( StrictMath.log10( probM ) ) + ( StrictMath.log10( probC ) ) );
                    } else
                        score = -1;

                    if ( score > 0 ) {
                        log.info( "score for " + ontoM + " and " + ontoC + " is " + score );
                        log.info( "The minimum subsumer term is: " + pterm );
                        total += score;
                        count++;
                    }

                }

            }

            if ( total > 0 ) {
                avgScore = total / count;
                scoreMap.put( gene, avgScore );
                log.info( "Average score for gene " + gene + " is " + avgScore );
            } else {
                scoreMap.put( gene, 1000.0 );
                log.info( "No overlapping terms" );
            }

        }
        return scoreMap;
    }

    /**
     * @param Take a gene and return a set of all GO terms including the parents of each GO term
     * @param geneOntologyTerms
     */
    @SuppressWarnings("unchecked")
    private Set<OntologyTerm> getGOTerms( Gene gene ) {

        Set<OntologyTerm> allGOTermSet = new HashSet<OntologyTerm>();

        Collection<VocabCharacteristic> stringTerms = gene2GOAssociationService.findByGene( gene );
        Set<String> uriTerms = new HashSet<String>();

        for ( VocabCharacteristic characteristic : stringTerms ) {
            String term = characteristic.getValueUri();
            if ( ( term != null ) ) {
                allGOTermSet.add( GeneOntologyService.getTermForURI( term ) );
                uriTerms.add( term );
            }
        }

        allGOTermSet.addAll( makeRootMap( uriTerms ) );

        return allGOTermSet;
    }

    /**
     * @param take a collection of GOTerm URIs
     * @return Identify the root of each term and put it in the rootMap and return GOTermSet containing all parents NOT
     *         including root terms
     */
    private Set<OntologyTerm> makeRootMap( Collection<String> terms ) {

        Set<OntologyTerm> allGOTermSet = new HashSet<OntologyTerm>();
        for ( String t : terms ) {
            Collection<OntologyTerm> parents = ontologyEntryService.getAllParents( GeneOntologyService
                    .getTermForURI( t ), partOf );

            for ( OntologyTerm p : parents ) {
                String id = GeneOntologyService.asRegularGoId( p );
                if ( id.equalsIgnoreCase( proccess ) ) {
                    rootMap.put( t, 1 );
                    continue;
                }
                if ( id.equalsIgnoreCase( function ) ) {
                    rootMap.put( t, 2 );
                    continue;
                }
                if ( id.equalsIgnoreCase( component ) ) {
                    rootMap.put( t, 3 );
                    continue;
                }

                allGOTermSet.add( p );
            }
        }
        return allGOTermSet;
    }

    private Map<String, Integer> addTermOccurrence( Collection<String> GO ) {

        for ( String ontM : GO ) {

            String ontId = GeneOntologyService.asRegularGoId( GeneOntologyService.getTermForURI( ontM ) );

            // skip roots
            if ( ontId.equalsIgnoreCase( proccess ) || ontId.equalsIgnoreCase( function )
                    || ontId.equalsIgnoreCase( component ) ) continue;

            if ( GOcountMap.containsKey( ontM ) ) {
                int value = GOcountMap.get( ontM );
                value++;
                GOcountMap.put( ontM, value );
                continue;
            }

            GOcountMap.put( ontM, 1 );
        }
        return GOcountMap;
    }

    /**
     * @param GOcountMap of terms and their occurrence
     * @return a new countMap of each term and how many times it occurs and its children occur
     */
    private Integer countChildren (String term) {

          
            int termCount = GOcountMap.get( term );

            if ( rootMap.get( term ) == null ) {
                log.warn( term + " was not in the root map" );
                return null;
            }

            int root = rootMap.get( term );

            switch ( root ) {
                case 1:
                    pCount += termCount;
                    break;
                case 2:
                    fCount += termCount;
                    break;
                case 3:
                    cCount += termCount;
                    break;
            }

            Collection<OntologyTerm> children = ontologyEntryService.getAllChildren( GeneOntologyService
                    .getTermForURI( term ), partOf );
            log.info( "got " + children.size() + " children terms for " + term );

            if ( children.isEmpty() || children == null ) {
                log.info( "No children for term " + term + " termcount is " + termCount );
                return termCount;
            }

            for ( OntologyTerm child : children ) {
                if ( GOcountMap.containsKey( child.getUri() ) ) {
                    int count = GOcountMap.get( child.getUri() );
                    termCount += count;
                }
            }
            
            log.info( "The term count for term " + term + " is " + termCount );
            return termCount;
            
        }

    /**
     * @param newCountMap
     * @return the probability for each GO term based on its occurrence
     */
    private Map<String, Double> getProb( Map<String, Integer> newCountMap ) {

        Map<String, Double> ProbMap = new HashMap<String, Double>();

        for ( String term : newCountMap.keySet() ) {

            if ( rootMap.get( term ) == null ) {
                log.warn( term + " was not in the root map" );
                continue;
            }
            int root = rootMap.get( term );
            int total = 0;

            switch ( root ) {
                case 1:
                    total = pCount;
                    break;
                case 2:
                    total = fCount;
                    break;
                case 3:
                    total = cCount;
                    break;
            }
            double prob = ( double ) ( newCountMap.get( term ) ) / total;

            ProbMap.put( term, prob );
            log.info( "The probability for " + term + "is " + prob );
        }

        return ProbMap;

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

    public void saveMapToDisk( Map toSaveMap, String filename ) {

        log.info( "Generating file from HashMap... " );

        try {
            // remove file first
            File f = new File( HOME_DIR + File.separatorChar + filename );
            if ( f.exists() ) {
                f.delete();
            }
            FileOutputStream fos = new FileOutputStream( f );
            ObjectOutputStream oos = new ObjectOutputStream( fos );
            oos.writeObject( toSaveMap );
            oos.flush();
            oos.close();
        } catch ( Throwable e ) {
            log.error( "Cannot write to file." );
            return;
        }
        log.info( "Done making report." );
    }

    public Map getMapFromDisk( File f ) {
        Map<Long, Collection<String>> returnMap = null;
        try {
            if ( f.exists() ) {
                FileInputStream fis = new FileInputStream( f );
                ObjectInputStream ois = new ObjectInputStream( fis );
                returnMap = ( Map ) ois.readObject();
                ois.close();
                fis.close();
            }
        } catch ( Throwable e ) {
            return null;
        }
        return returnMap;
    }

}
