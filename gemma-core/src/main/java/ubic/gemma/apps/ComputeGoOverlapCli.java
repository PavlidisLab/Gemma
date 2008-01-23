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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.Writer;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;

import ubic.basecode.math.RandomChooser;
import ubic.gemma.analysis.coexpression.ProbeLinkCoexpressionAnalyzer;
import ubic.gemma.model.association.Gene2GOAssociationService;
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
    @SuppressWarnings("static-access")
    protected void buildOptions() {
        Option goMetricOption = OptionBuilder.hasArg().withArgName( "Choice of GO Metric" ).withDescription(
                "resnik, lin, jiang; default = simple" ).withLongOpt( "metric" ).create( 'm' );
        addOption( goMetricOption );

        Option maxOption = OptionBuilder.hasArg().withArgName( "Choice of using MAX calculation" ).withDescription(
                "MAX" ).withLongOpt( "max" ).create( 'x' );
        addOption( maxOption );

        Option fileOption = OptionBuilder.withArgName( "fpath" ).isRequired().withLongOpt( "filepath" ).hasArg()
                .withDescription( "Path where file is located" ).create( 'f' );
        addOption( fileOption );

    }

    @Override
    protected void processOptions() {
        super.processOptions();

        if ( hasOption( 'x' ) ) {
            String max = getOptionValue( 'x' );
            if ( max.equalsIgnoreCase( "MAX" ) )
                this.max = true;
            else
                this.max = false;
        }

        if ( hasOption( 'm' ) ) {
            String metricName = getOptionValue( 'm' );
            if ( metricName.equalsIgnoreCase( "resnik" ) )
                this.metric = GoMetric.Metric.resnik;
            else if ( metricName.equalsIgnoreCase( "lin" ) )
                this.metric = GoMetric.Metric.lin;
            else if ( metricName.equalsIgnoreCase( "jiang" ) )
                this.metric = GoMetric.Metric.jiang;
            else {
                this.metric = GoMetric.Metric.simple;
                this.max = false;
            }
        }

        if ( hasOption( 'f' ) ) {
            String file = getOptionValue( 'f' );
            File f = new File( file );
            if ( f.canRead() ) {
                this.file_path = file;
            } else
                System.out.println( "Cannot read from " + file + "!" );
        }

    }

    // A list of service beans
    private GeneService geneService;
    private Gene2GOAssociationService gene2GOAssociationService;
    private GeneOntologyService ontologyEntryService;
    private TaxonService taxonService;
    private GoMetric goMetric;

    private Map<Long, Collection<String>> mouseGeneGOMap = new HashMap<Long, Collection<String>>();
    private Map<String, Gene> geneCache = new HashMap<String, Gene>();

    private Map<String, Integer> rootMap = new HashMap<String, Integer>();
    Map<String, Integer> GOcountMap = new HashMap<String, Integer>();

    private static final String HASH_MAP_RETURN = "HashMapReturn";
    private static final String GO_PROB_MAP = "GoProbMap";
    private static final String HOME_DIR = ConfigUtils.getString( "gemma.appdata.home" );
    private static final String RANDOM_SUBSET = "RandomSubset2";
    private static final String GENE_CACHE = "geneCache";
    private String file_path = "";

    private Metric metric = GoMetric.Metric.simple;
    private boolean max = false;
    private String OUT_FILE = "outPutFile";

    // INCLUDE PARTOF OR CHANGE STRINGENCY
    private boolean partOf = true;
    final int stringincy = 6;

    private String process = "http://purl.org/obo/owl/GO#GO_0008150";
    private String function = "http://purl.org/obo/owl/GO#GO_0003674";
    private String component = "http://purl.org/obo/owl/GO#GO_0005575";
    private int pCount = 0;
    private int fCount = 0;
    private int cCount = 0;
    private ProbeLinkCoexpressionAnalyzer probeLinkCoexpressionAnalyzer;

    protected void initBeans() {
        taxonService = ( TaxonService ) getBean( "taxonService" );
        geneService = ( GeneService ) getBean( "geneService" );
        gene2GOAssociationService = ( Gene2GOAssociationService ) getBean( "gene2GOAssociationService" );
        ontologyEntryService = ( GeneOntologyService ) getBean( "geneOntologyService" );
        goMetric = ( GoMetric ) getBean( "goMetric" );
        probeLinkCoexpressionAnalyzer = ( ProbeLinkCoexpressionAnalyzer ) this
                .getBean( "probeLinkCoexpressionAnalyzer" );
        while ( !ontologyEntryService.isReady() ) {
            log.info( "waiting for ontology load.." );
            try {
                Thread.sleep( 1000 );
            } catch ( InterruptedException e ) {
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#doWork(java.lang.String[])
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Exception doWork( String[] args ) {

        Exception err = processCommandLine( "Computer Go Overlap ", args );
        if ( err != null ) return err;

        initBeans();

        String commonName = "mouse";
        Taxon taxon = taxonService.findByCommonName( commonName );

        log.info( "Checking for Gene2GO Map file..." );

        File f = new File( HOME_DIR + File.separatorChar + HASH_MAP_RETURN );
        if ( f.exists() ) {
            mouseGeneGOMap = ( Map<Long, Collection<String>> ) getCacheFromDisk( f );
            log.info( "Found file!" );
        }

        else {

            Collection<Gene> mouseGenes = geneService.loadKnownGenes( taxon );

            for ( Gene gene : mouseGenes ) {
                Collection<OntologyTerm> GOTerms = ontologyEntryService.getGOTerms( gene, partOf );
                if ( GOTerms == null || GOTerms.isEmpty() ) continue;
                log.info( "Got go terms for " + gene.getName() );

                Collection<String> termString = new HashSet<String>();
                for ( OntologyTerm oe : GOTerms ) {
                    termString.add( oe.getUri() );
                }
                mouseGeneGOMap.put( gene.getId(), termString );

            }
            saveCacheToDisk( ( HashMap ) mouseGeneGOMap, HASH_MAP_RETURN );

        }

        Map<String, Double> GOProbMap = new HashMap<String, Double>();

        File f2 = new File( HOME_DIR + File.separatorChar + GO_PROB_MAP );
        if ( f2.exists() ) {
            GOProbMap = ( HashMap<String, Double> ) getCacheFromDisk( f2 );
            log.info( "Found probability file!" );
        }

        else {
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
            this.saveCacheToDisk( ( HashMap ) GOProbMap, GO_PROB_MAP );
        }

        Map<Collection<Gene>, Double> scoreMap = new HashMap<Collection<Gene>, Double>();
        
        Collection<Collection<Gene>> subsetPairs = new HashSet<Collection<Gene>>();
     
        File f3 = new File( HOME_DIR + File.separatorChar + RANDOM_SUBSET );
        if ( f3.exists() ) {
            subsetPairs = ( HashSet<Collection<Gene>> ) getCacheFromDisk( f3 );
            log.info( "Found cached subset file!" );
        } else {
            Collection<Gene> allGenes = geneService.loadKnownGenes( taxon );
            for (Gene g : allGenes){
                geneService.thaw( g );
            }
            
            subsetPairs = getRandomPairs( 10000, allGenes );
            this.saveCacheToDisk( ( HashSet ) subsetPairs, RANDOM_SUBSET );
        }
        

        StopWatch overallWatch = new StopWatch();
        overallWatch.start();

        for ( Collection<Gene> pair : subsetPairs ) {

            if (pair.size() != 2){
                log.warn( "A pair consists of two objects. More than two is unacceptable. Fix it!!" );
            }
            Iterator<Gene> genePair = pair.iterator();
            Gene masterGene = genePair.next();
            Gene coExpGene = genePair.next();

            double score = 0.0;

            if ( max ) {
                log.info( "getting MAX scores for " + metric );
                score = goMetric.computeMaxSimilarity( masterGene, coExpGene, GOProbMap, metric );
            } else
                score = goMetric.computeSimilarity( masterGene, coExpGene, GOProbMap, metric );

            scoreMap.put( pair, score );
        }

        try {
            Writer write = initOutputFile( OUT_FILE );

            for ( Collection<Gene> pair : scoreMap.keySet() ) {
                Iterator<Gene> genePair = pair.iterator();
                Gene mGene = genePair.next();
                Gene cGene = genePair.next();
                double overlap = scoreMap.get( pair );

                int masterGOTerms;
                int coExpGOTerms;

                if ( !mouseGeneGOMap.containsKey( mGene.getId() ) )
                    masterGOTerms = 0;
                else
                    masterGOTerms = ( mouseGeneGOMap.get( mGene.getId() ) ).size();

                if ( !mouseGeneGOMap.containsKey( cGene.getId() ) )
                    coExpGOTerms = 0;
                else
                    coExpGOTerms = ( mouseGeneGOMap.get( cGene.getId() ) ).size();

                Collection<OntologyTerm> goTerms = getTermOverlap( mGene, cGene );
                writeOverlapLine( write, mGene.getOfficialSymbol(), cGene.getOfficialSymbol(), overlap, goTerms, masterGOTerms, coExpGOTerms );
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
                    pCount += GOcountMap.get( t );
                    break;
                }
                if ( p.getUri().equalsIgnoreCase( function ) ) {
                    rootMap.put( t, 2 );
                    fCount += GOcountMap.get( t );
                    break;
                }
                if ( p.getUri().equalsIgnoreCase( component ) ) {
                    rootMap.put( t, 3 );
                    cCount += GOcountMap.get( t );
                    break;
                }
            }
            if ( !( rootMap.containsKey( t ) ) ) {
                log.warn( "Couldn't get root for term: " + t );
                remove.add( t );
            }
        }

        for ( String s : remove ) {
            GOcountMap.remove( s );
        }
    }

    /**
     * @param take a collection of genes and size of susbset 
     * @return a collection of random gene pairs that have GO annotations
     */
    private Collection<Collection<Gene>> getRandomPairs (int size, Collection<Gene> genes){
        
        Collection<Collection<Gene>> subsetPairs = new HashSet<Collection<Gene>>();
        int i =0;
        
        while (i < size) {
            Collection<Gene> gene1 = RandomChooser.chooseRandomSubset( 1, genes );
            Collection<Gene> gene2 = RandomChooser.chooseRandomSubset( 1, genes );

           if ( gene1.containsAll( gene2 ) ) continue;
           
           Collection<Gene> genePair = new HashSet<Gene>();
           if (subsetPairs.contains( genePair )) continue;
           genePair.addAll( gene1 );
           genePair.addAll( gene2 );
           
           Iterator<Gene> iterator = genePair.iterator();
           boolean noGoTerms = false;
           while (iterator.hasNext()){
               if (! mouseGeneGOMap.containsKey( iterator.next())){
                   noGoTerms = true;
                   break;
               }
           }
           
           if (noGoTerms) continue;
           subsetPairs.add( genePair );
           log.info( "Added pair to subset!" );
           i++;
        }
        
        return subsetPairs;
    }
    
    
    private Map<String, Integer> loadLinks( String filepath, Taxon taxon ) throws IOException {

        File f = new File( filepath );

        log.info( "Loading links from " + filepath );
        BufferedReader in = new BufferedReader( new FileReader( f ) );

        Map<String, Integer> geneMap = new HashMap<String, Integer>();

        String line;
        while ( ( line = in.readLine() ) != null ) {
            line = line.trim();
            if ( line.startsWith( "#" ) ) {
                continue;
            }

            String[] strings = StringUtils.split( line );
            String g1 = strings[0];
            String g2 = strings[1];

            // skip any self links.
            if ( g1.equals( g2 ) ) continue;

            Integer support = Integer.parseInt( strings[2] );// positive only!

            if ( support.equals( "0" ) ) continue;

            Gene gene1 = null;
            Gene gene2 = null;

            if ( geneCache.containsKey( g1 ) ) {
                gene1 = geneCache.get( g1 );
            } else {
                Collection<Gene> genes = geneService.findByOfficialSymbol( g1 );
                for ( Gene gene : genes ) {
                    if ( gene.getTaxon().equals( taxon ) ) {
                        geneCache.put( g1, gene );
                        gene1 = gene;
                        break;
                    }
                }
            }

            if ( !mouseGeneGOMap.containsKey( gene1.getId() ) ) continue;

            if ( geneCache.containsKey( g2 ) ) {
                gene2 = geneCache.get( g2 );
            } else {
                Collection<Gene> genes = geneService.findByOfficialSymbol( g2 );
                for ( Gene gene : genes ) {
                    if ( gene.getTaxon().equals( taxon ) ) {
                        geneCache.put( g2, gene );
                        gene2 = gene;
                        break;
                    }
                }
            }

            if ( !mouseGeneGOMap.containsKey( gene2.getId() ) ) continue;

            String key = g1 + "_" + g2;
            geneMap.put( key, support );
        }

        saveCacheToDisk( ( HashMap ) geneCache, GENE_CACHE );
        return geneMap;
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

    public void saveCacheToDisk( Serializable toSave, String filename ) {

        log.info( "Generating file ... " );

        try {
            // remove file first
            File f = new File( HOME_DIR + File.separatorChar + filename );
            if ( f.exists() ) {
                f.delete();
            }
            FileOutputStream fos = new FileOutputStream( f );
            ObjectOutputStream oos = new ObjectOutputStream( fos );
            oos.writeObject( toSave );
            oos.flush();
            oos.close();
        } catch ( Throwable e ) {
            log.error( "Cannot write to file." );
            return;
        }
        log.info( "Done making report." );
    }

    public Serializable getCacheFromDisk( File f ) {
        Serializable returnObject = null;
        try {
            if ( f.exists() ) {
                FileInputStream fis = new FileInputStream( f );
                ObjectInputStream ois = new ObjectInputStream( fis );
                returnObject = ( Serializable ) ois.readObject();
                ois.close();
                fis.close();
            }
        } catch ( Throwable e ) {
            return null;
        }
        return returnObject;
    }

}
