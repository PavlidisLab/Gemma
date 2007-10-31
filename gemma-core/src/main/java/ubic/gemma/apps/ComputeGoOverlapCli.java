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
import ubic.gemma.ontology.GoMetric;
import ubic.gemma.ontology.OntologyTerm;
import ubic.gemma.ontology.GoMetric.Metric;
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

    @Override
    protected void buildOptions() {
    }

    // A list of service beans
    private GeneService geneService;
    private Gene2GOAssociationService gene2GOAssociationService;
    private GeneOntologyService ontologyEntryService;
    private TaxonService taxonService;
    private GoMetric goMetric;

    private Map<Long, Collection<String>> mouseGeneGOMap = new HashMap<Long, Collection<String>>();

    private Map<String, Integer> rootMap = new HashMap<String, Integer>();
    Map<String, Integer> GOcountMap = new HashMap<String, Integer>();

    private String HASH_MAP_RETURN = "HashMapReturn";
    private String GO_PROB_MAP = "GoProbMap";
    private String HOME_DIR = ConfigUtils.getString( "gemma.appdata.home" );
    
    //CHANGE METRIC AND OUTPUT FILE NAME
    private Metric metric = GoMetric.Metric.simple;
    private String OUT_FILE = "SimpleOverlap";
    
    //INCLUDE PARTOF OR CHANGE STRINGENCY
    private boolean partOf = true;
    final int stringincy = 6;

    private String process = "http://purl.org/obo/owl/GO#GO_0008150";
    private String function = "http://purl.org/obo/owl/GO#GO_0003674";
    private String component = "http://purl.org/obo/owl/GO#GO_0005575";
    private int pCount = 0;
    private int fCount = 0;
    private int cCount = 0;

    protected void initBeans() {
        geneService = ( GeneService ) getBean( "geneService" );
        gene2GOAssociationService = ( Gene2GOAssociationService ) getBean( "gene2GOAssociationService" );
        ontologyEntryService = ( GeneOntologyService ) getBean( "geneOntologyService" );
        taxonService = ( TaxonService ) getBean( "taxonService" );
        goMetric = (GoMetric) getBean("goMetric");

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

        
       Map<Gene, Collection<Gene>> geneExpMap = new HashMap<Gene, Collection<Gene>>();

        log.debug( "Total master genes:" + masterGenes.size() );

        for ( Gene gene : masterGenes ) {

            Collection<Gene> foundGenes = getCoexpressedGenes( gene, stringincy );
            if ( ( foundGenes == null ) || ( foundGenes.isEmpty() ) )
                continue;
            else {
                geneExpMap.put( gene, foundGenes );
            }
        }

        log.info( "Checking for file..." );

        File f = new File( HOME_DIR + File.separatorChar + HASH_MAP_RETURN );
        if ( f.exists() ) {
            mouseGeneGOMap = getMapFromDisk( f );
            log.info( "Found file!" );
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
                mouseGeneGOMap.put( gene.getId(), termString );

            }
            saveMapToDisk( mouseGeneGOMap, HASH_MAP_RETURN );

        }

        Map<String, Double> GOProbMap = new HashMap<String, Double>();
        
        File f2 = new File( HOME_DIR + File.separatorChar + GO_PROB_MAP );
        if ( f2.exists() ) {
            GOProbMap  = getMapFromDisk( f2 );
            log.info( "Found probability file!" );
        }
        
        else{
            log.info( "Calculating probabilities... " );
            
            GOcountMap = goMetric.getTermOccurrence( mouseGeneGOMap );
            makeRootMap( GOcountMap.keySet() );

            for ( String uri : GOcountMap.keySet() ) {
                int total = 0;
                log.info( "Counting children for " + uri );
                int count = goMetric.getChildrenOccurrence( GOcountMap, uri );
                if ( rootMap.get( uri ) == 1 ) total = pCount;
                if ( rootMap.get( uri ) == 2 ) total = fCount;
                if ( rootMap.get( uri ) == 3 ) total = cCount;

                GOProbMap.put( uri, ( double ) count / total );
            }

            this.saveMapToDisk( GOProbMap, GO_PROB_MAP );
            Long Elapsed = overallWatch.getTime();
            log.info( "Creating GO probability map took: " + Elapsed / 1000 + "s " );
        }

        Map<Gene, Map<Gene, Double>> masterTermCountMap = new HashMap<Gene, Map<Gene, Double>>();

        for ( Gene masterGene : geneExpMap.keySet() ) {
            Map<Gene, Double> scoreMap = new HashMap<Gene, Double>();
            Collection<Gene> coExpGene = geneExpMap.get( masterGene );

            for ( Gene cGene : coExpGene ) {
                Double score = goMetric.computeSimilarity( masterGene, cGene, GOProbMap, metric);
                if ( score != null )
                    scoreMap.put( cGene, score );
                else
                    scoreMap.put( cGene, 1000.00 );
            }
            masterTermCountMap.put( masterGene, scoreMap );
        }

        try {
            Writer write = initOutputFile( OUT_FILE );
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
            if ( ontologyEntry.equalsIgnoreCase( process ) || ontologyEntry.equalsIgnoreCase( function )
                    || ontologyEntry.equalsIgnoreCase( component ) ) continue;
            for ( String ontologyEntryC : coExpGO ) {

                if ( ontologyEntry.equalsIgnoreCase( process ) || ontologyEntry.equalsIgnoreCase( function )
                        || ontologyEntry.equalsIgnoreCase( component ) ) continue;

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

    private Collection<Gene> getCoexpressedGenes( Gene gene, Integer stringincy ) {

        CoexpressionCollectionValueObject coexpressed = ( CoexpressionCollectionValueObject ) geneService
                .getCoexpressedGenes( gene, null, stringincy );

        Collection<Long> geneIds = new HashSet<Long>();
        for ( CoexpressionValueObject co : coexpressed.getCoexpressionData() ) {
            geneIds.add( co.getGeneId() );
        }

        Collection<Gene> cGenes = new HashSet<Gene>();
        for ( long id : geneIds ) {
            cGenes.add( geneService.load( id ) );
        }
        if ( cGenes.isEmpty() || cGenes == null ) return null;

        return cGenes;

    }

    /**
     * @param Take a gene and return a set of all GO terms including the parents of each GO term
     * @param geneOntologyTerms
     */
    @SuppressWarnings("unchecked")
    private Set<OntologyTerm> getGOTerms( Gene gene ) {

        Set<OntologyTerm> termSet = new HashSet<OntologyTerm>();

        Collection<VocabCharacteristic> stringTerms = gene2GOAssociationService.findByGene( gene );

        for ( VocabCharacteristic characteristic : stringTerms ) {
            String term = characteristic.getValueUri();
            if ( ( term != null ) ) {
                termSet.add( GeneOntologyService.getTermForURI( term ) );
            }
        }

        termSet.addAll( ontologyEntryService.getAllParents( termSet, partOf ) );

        return termSet;
    }


    /**
     * @param take a collection of GOTerm URIs
     * @return Identify the root of each term and put it in the rootMap
     */
    private void makeRootMap( Collection<String> terms ) {

        Collection<String> remove = new HashSet<String>();
        
        for ( String t : terms ) {
            Collection<OntologyTerm> parents = ontologyEntryService.getAllParents( GeneOntologyService
                    .getTermForURI( t ), partOf );

            for ( OntologyTerm p : parents ) {
                if ( p.getUri().equalsIgnoreCase( process ) ) {
                    rootMap.put( t, 1 );
                    pCount+=GOcountMap.get( t );
                    break;
                }
                if ( p.getUri().equalsIgnoreCase( function ) ) {
                    rootMap.put( t, 2 );
                    fCount+=GOcountMap.get( t );
                    break;
                }
                if ( p.getUri().equalsIgnoreCase( component ) ) {
                    rootMap.put( t, 3 );
                    cCount+= GOcountMap.get( t );
                    break;
                }
            }
            if ( !( rootMap.containsKey( t ) ) ){
                log.warn( "Couldn't get root for term: " + t );
                remove.add( t );
            }
        }
        
        for (String s : remove){
            GOcountMap.remove( s );
        }      
    }

    /**
     * @param GOcountMap of terms and their occurrence
     * @return a new countMap of each term and how many times it occurs and its children occur
     */

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
