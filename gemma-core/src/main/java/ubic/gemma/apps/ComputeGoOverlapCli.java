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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;

import cern.jet.stat.Descriptive;
import cern.colt.list.DoubleArrayList;

import ubic.basecode.math.RandomChooser;
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

    private int firstGeneColumn = 0;
    private int secondGeneColumn = 1;
    private Taxon taxon;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#buildOptions()
     */

    @Override
    @SuppressWarnings("static-access")
    protected void buildOptions() {
        Option goMetricOption = OptionBuilder.hasArg().withArgName( "Choice of GO Metric" ).withDescription(
                "resnik, lin, jiang, percent; default = simple" ).withLongOpt( "metric" ).create( 'm' );
        addOption( goMetricOption );

        Option maxOption = OptionBuilder.hasArg().withArgName( "Choice of using MAX calculation" ).withDescription(
                "MAX" ).withLongOpt( "max" ).create( 'x' );
        addOption( maxOption );

        Option dataOption = OptionBuilder.hasArg().withArgName(
                "Choice of generating random gene pairs OR Input data file" ).withDescription( "dataType" )
                .isRequired().create( 'd' );
        addOption( dataOption );

        Option taxonOption = OptionBuilder.hasArg().withArgName( "Choice of taxon" ).withDescription(
                "human, rat, mouse" ).isRequired().create( 't' );
        addOption( taxonOption );

        Option firstGeneOption = OptionBuilder.hasArg().withArgName( "colindex" ).withDescription(
                "Column index with gene1 (starting from 0; default=0)" ).create( "g1col" );
        Option secondGeneOption = OptionBuilder.hasArg().withArgName( "colindex" ).withDescription(
                "Column index with gene1 (starting from 0; default=1)" ).create( "g2col" );

        addOption( firstGeneOption );
        addOption( secondGeneOption );
    }

    @Override
    protected void processOptions() {
        super.processOptions();

        if ( hasOption( 't' ) ) {
            this.commonName = getOptionValue( 't' );
            if ( StringUtils.isBlank( commonName ) ) {
                System.out.println( "MUST enter a valid taxon!" );
                System.exit( 0 );
            }
            if ( !StringUtils.equalsIgnoreCase( "mouse", commonName )
                    && !StringUtils.equalsIgnoreCase( "rat", commonName )
                    && !StringUtils.equalsIgnoreCase( "human", commonName ) ) {
                System.out.println( "MUST enter a valid taxon!" );
                System.exit( 0 );
            }

        }

        if ( hasOption( 'd' ) ) {

            String input = getOptionValue( 'd' );

            if ( StringUtils.isNumeric( input ) ) {
                SET_SIZE = Integer.parseInt( input );
                this.random = true;
                System.out.println( "Will create a set of " + SET_SIZE + " random gene pairs!" );
            } else {
                File f = new File( input );
                if ( f.canRead() ) {
                    this.file_path = input;
                    this.random = false;
                } else {
                    System.out.println( input
                            + "is NOT a valid filename! You MUST enter a valid filename OR size of dataset" );
                    System.exit( 0 );
                }
            }

        }

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
            else if ( metricName.equalsIgnoreCase( "percent" ) )
                this.metric = GoMetric.Metric.percent;
            else {
                this.metric = GoMetric.Metric.simple;
                this.max = false;
            }
        }

        if ( this.hasOption( "g1col" ) ) {
            this.firstGeneColumn = getIntegerOptionValue( "g1col" );
        }

        if ( this.hasOption( "g2col" ) ) {
            this.secondGeneColumn = getIntegerOptionValue( "g2col" );
        }

    }

    // A list of service beans
    private GeneService geneService;
    private Gene2GOAssociationService gene2GOAssociationService;
    private GeneOntologyService ontologyEntryService;
    private TaxonService taxonService;
    private GoMetric goMetric;

    private Map<Long, Collection<String>> geneGoMap = new HashMap<Long, Collection<String>>();
    private Map<String, Gene> geneCache = new HashMap<String, Gene>();

    private Map<String, Integer> rootMap = new HashMap<String, Integer>();
    Map<String, Integer> GOcountMap = new HashMap<String, Integer>();
    Map<String, Double> GOProbMap = new HashMap<String, Double>();

    private static final String HASH_MAP_RETURN = "HashMapReturn";
    private static final String GO_PROB_MAP = "GoProbMap";
    private static final String HOME_DIR = ConfigUtils.getString( "gemma.appdata.home" );
    private static final String RANDOM_SUBSET = "RandomSubset1K";
    private static final String GENE_CACHE = "geneCache";
    private static Integer SET_SIZE = 0;
    private String file_path = "";
    private String commonName = "";

    private Metric metric = GoMetric.Metric.simple;
    private boolean max = false;
    private boolean random = false;
    private String OUT_FILE = "1K_percent";

    // INCLUDE PARTOF OR CHANGE STRINGENCY
    private boolean partOf = true;

    private String process = "http://purl.org/obo/owl/GO#GO_0008150";
    private String function = "http://purl.org/obo/owl/GO#GO_0003674";
    private String component = "http://purl.org/obo/owl/GO#GO_0005575";
    private int pCount = 0;
    private int fCount = 0;
    private int cCount = 0;

    protected void initBeans() {
        taxonService = ( TaxonService ) getBean( "taxonService" );
        geneService = ( GeneService ) getBean( "geneService" );
        gene2GOAssociationService = ( Gene2GOAssociationService ) getBean( "gene2GOAssociationService" );
        ontologyEntryService = ( GeneOntologyService ) getBean( "geneOntologyService" );
        goMetric = ( GoMetric ) getBean( "goMetric" );

        /*
         * Initialize the Gene Ontology.
         */
        this.ontologyEntryService.init( true );

        while ( !ontologyEntryService.isReady() ) {
            log.info( "waiting for ontology load.." );
            try {
                Thread.sleep( 10000 );
                if ( !ontologyEntryService.isRunning() ) break;
            } catch ( InterruptedException e ) {
                throw new RuntimeException( e );
            }
        }

        if ( !ontologyEntryService.isReady() ) {
            throw new RuntimeException( "Gene Ontology was not loaded successfully." );
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

        this.taxon = taxonService.findByCommonName( commonName );
        if ( taxon == null ) {
            System.out.println( "Taxon " + commonName + " not found." );
            System.exit( 0 );
        }

        log.info( "Checking for Gene2GO Map file..." );
        File f = new File( HOME_DIR + File.separatorChar + HASH_MAP_RETURN );
        if ( f.exists() ) {
            geneGoMap = ( Map<Long, Collection<String>> ) getCacheFromDisk( f );
            log.info( "Found file!" );
        }

        else {
            cacheGeneGoInformationToDisk( taxon );
        }

        if ( !metric.equals( GoMetric.Metric.simple ) && !metric.equals( GoMetric.Metric.percent ) ) {
            computeTermProbabilities();
        }

        // results go here
        Map<GenePair, Double> scoreMap = new HashMap<GenePair, Double>();

        Collection<GenePair> genePairs = new HashSet<GenePair>();

        if ( random ) {
            genePairs = loadRandomPairs();
        } else if ( StringUtils.isNotBlank( file_path ) ) {
            try {
                File links = new File( file_path );
                genePairs = loadLinks( links, taxon );
                log.info( "Done loading data..." );
            } catch ( IOException e ) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        } else {
            log.error( "What should I do?" );
            bail( ErrorCode.INVALID_OPTION );
        }

        StopWatch overallWatch = new StopWatch();
        overallWatch.start();

        for ( GenePair pair : genePairs ) {

            List<Gene> genes1 = pair.getFirstGenes();
            List<Gene> genes2 = pair.getSecondGenes();

            List<Double> scores = new ArrayList<Double>();

            for ( Gene g1 : genes1 ) {
                for ( Gene g2 : genes2 ) {
                    if ( g1.equals( g2 ) ) continue;
                    double score = 0.0;

                    if ( max ) {
                        log.info( "getting MAX scores for " + metric );
                        score = goMetric.computeMaxSimilarity( g1, g2, GOProbMap, metric );
                    } else {
                        score = goMetric.computeSimilarity( g1, g2, GOProbMap, metric );
                    }
                    scores.add( score );
                }
            }
            DoubleArrayList dlist = new DoubleArrayList();
            dlist.addAllOf( scores );

            scoreMap.put( pair, Descriptive.median( dlist ) );
        }

        writeGoSimilarityResults( scoreMap, overallWatch );
        return null;

    }

    /**
     * @param scoreMap
     * @param overallWatch
     */
    private void writeGoSimilarityResults( Map<GenePair, Double> scoreMap, StopWatch overallWatch ) {
        try {
            Writer write = initOutputFile( OUT_FILE );

            for ( GenePair pair : scoreMap.keySet() ) {
                List<Gene> firstGenes = pair.getFirstGenes();
                List<Gene> secondGenes = pair.getSecondGenes();

                String pairString = pair.toString();
                double score = scoreMap.get( pair );
                // ---------------------NOT WRITING TO FILE--------------------------------
                if ( firstGenes.size() == 1 && secondGenes.size() == 1 ) {
                    Gene mGene = firstGenes.iterator().next();
                    Gene cGene = secondGenes.iterator().next();
                    int masterGOTerms;
                    int coExpGOTerms;

                    if ( !geneGoMap.containsKey( mGene.getId() ) )
                        masterGOTerms = 0;
                    else
                        masterGOTerms = ( geneGoMap.get( mGene.getId() ) ).size();

                    if ( !geneGoMap.containsKey( cGene.getId() ) )
                        coExpGOTerms = 0;
                    else
                        coExpGOTerms = ( geneGoMap.get( cGene.getId() ) ).size();

                    Collection<OntologyTerm> goTerms = getTermOverlap( mGene, cGene );
                    writeOverlapLine( write, pairString, score, goTerms, masterGOTerms, coExpGOTerms );
                } else {
                    writeOverlapLine( write, pairString, score, null, 0, 0 );
                }

            }
            overallWatch.stop();
            log.info( "Compute GoOverlap takes " + overallWatch.getTime() + "ms" );

            // printResults(masterTermCountMap);
        } catch ( IOException ioe ) {
            log.error( "Couldn't write to file: " + ioe );

        }
    }

    /**
     * @return
     */
    private Collection<GenePair> loadRandomPairs() {
        Collection<GenePair> randomPairs;
        try {
            File f3 = new File( HOME_DIR + File.separatorChar + RANDOM_SUBSET );
            if ( f3.exists() ) {
                randomPairs = ( HashSet<GenePair> ) loadLinks( f3, this.taxon );
                log.info( "Found cached subset file!" );
            } else {
                Collection<Gene> allGenes = new HashSet<Gene>();
                for ( Long g : geneGoMap.keySet() ) {
                    allGenes.add( geneService.load( g ) );
                }

                randomPairs = getRandomPairs( SET_SIZE, allGenes );

                Writer w = new FileWriter( f3 );
                this.writeLinks( randomPairs, w );

            }
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
        return randomPairs;
    }

    /**
     * 
     */
    private void computeTermProbabilities() {
        File f2 = new File( HOME_DIR + File.separatorChar + GO_PROB_MAP );
        if ( f2.exists() ) {
            GOProbMap = ( HashMap<String, Double> ) getCacheFromDisk( f2 );
            log.info( "Found probability file!" );
        }

        else {
            log.info( "Calculating probabilities... " );

            GOcountMap = goMetric.getTermOccurrence( geneGoMap );
            makeRootMap( GOcountMap.keySet() );
            GOProbMap = makeProbMap( GOcountMap );

            this.saveCacheToDisk( ( HashMap ) GOProbMap, GO_PROB_MAP );
        }
    }

    /**
     * @param taxon
     */
    private void cacheGeneGoInformationToDisk( Taxon taxon ) {
        Collection<Gene> mouseGenes = geneService.loadKnownGenes( taxon );

        for ( Gene gene : mouseGenes ) {
            Collection<OntologyTerm> GOTerms = ontologyEntryService.getGOTerms( gene, partOf );
            if ( GOTerms == null || GOTerms.isEmpty() ) continue;
            log.info( "Got go terms for " + gene.getName() );

            Collection<String> termString = new HashSet<String>();
            for ( OntologyTerm oe : GOTerms ) {
                termString.add( oe.getUri() );
            }
            geneGoMap.put( gene.getId(), termString );
        }
        saveCacheToDisk( ( HashMap ) geneGoMap, HASH_MAP_RETURN );
    }

    class GeneComparator implements Comparator {

        public int compare( Object o1, Object o2 ) {

            if ( o1 instanceof Gene && o2 instanceof Gene ) {
                Long g1 = ( ( Gene ) o1 ).getId();
                Long g2 = ( ( Gene ) o2 ).getId();

                if ( g1 > g2 )
                    return 1;
                else if ( g1 < g2 ) return -1;
            }
            return 0;
        }

    }

    private Map<String, Double> makeProbMap( Map<String, Integer> GOcountMap ) {

        for ( String uri : GOcountMap.keySet() ) {
            int total = 0;
            log.info( "Counting children for " + uri );
            int count = goMetric.getChildrenOccurrence( GOcountMap, uri );
            if ( rootMap.get( uri ) == 1 ) total = pCount;
            if ( rootMap.get( uri ) == 2 ) total = fCount;
            if ( rootMap.get( uri ) == 3 ) total = cCount;

            GOProbMap.put( uri, ( double ) count / total );
        }

        return GOProbMap;
    }

    private Collection<OntologyTerm> getTermOverlap( Gene g, Gene coexpG ) {

        Collection<String> masterGO = geneGoMap.get( g.getId() );
        Collection<String> coExpGO = geneGoMap.get( coexpG.getId() );
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
    private Collection<GenePair> getRandomPairs( int size, Collection<Gene> genes ) {

        Collection<GenePair> subsetPairs = new HashSet<GenePair>();
        int i = 0;

        while ( i < size ) {
            List<Gene> twoGenes = new ArrayList( RandomChooser.chooseRandomSubset( 2, genes ) );

            if ( twoGenes.size() != 2 ) {
                log.warn( "A pair consists of two objects. More than two is unacceptable. Fix it!!" );
            }
            Collections.sort( twoGenes, new GeneComparator() );

            GenePair genePair = new GenePair();

            Gene g1 = twoGenes.get( 0 );
            Gene g2 = twoGenes.get( 1 );

            genePair.addFirstGene( g1 );
            genePair.addFirstGene( g2 );

            // Collections.sort( genePair );

            if ( subsetPairs.contains( genePair ) ) continue;

            Iterator<Gene> iterator = twoGenes.iterator();
            boolean noGoTerms = false;
            while ( iterator.hasNext() ) {
                if ( !geneGoMap.containsKey( iterator.next().getId() ) ) {
                    noGoTerms = true;
                    break;
                }
            }

            if ( noGoTerms ) continue;
            subsetPairs.add( genePair );
            log.info( "Added pair to subset!" );
            i++;
        }

        return subsetPairs;
    }

    private void writeLinks( Collection<GenePair> pairs, Writer writer ) {
        /*
         * print the pairs out in the same format that we can read in.
         */
    }

    private Set<GenePair> loadLinks( File f, Taxon taxon ) throws IOException {

        log.info( "Loading data from " + f );
        BufferedReader in = new BufferedReader( new FileReader( f ) );

        Set<GenePair> geneMap = new HashSet<GenePair>();

        String line;
        boolean alreadyWarned = false;
        while ( ( line = in.readLine() ) != null ) {
            line = line.trim();
            if ( line.startsWith( "#" ) ) {
                continue;
            }

            String[] fields = StringUtils.split( line );

            if ( fields.length < 2 ) {
                if ( !alreadyWarned ) {
                    log.warn( "Bad field on line: " + line + " (subsequent errors suppressed)" );
                    alreadyWarned = true;
                }
                continue;
            }

            String g1 = fields[firstGeneColumn];
            String g2 = fields[secondGeneColumn];
            // skip any self links.
            // if ( g1.equals( g2 ) ) continue;

            String[] gene1Strings = StringUtils.split( g1, "," );
            String[] gene2Strings = StringUtils.split( g2, "," );

            GenePair genePair = new GenePair();

            for ( String gene1string : gene1Strings ) {

                Gene gene1 = null;
                if ( geneCache.containsKey( gene1string ) ) {
                    gene1 = geneCache.get( gene1string );
                } else {
                    Collection<Gene> genes = geneService.findByOfficialSymbol( gene1string );
                    for ( Gene gene : genes ) {
                        if ( gene.getTaxon().equals( taxon ) ) {
                            geneCache.put( gene1string, gene );
                            gene1 = gene;
                            break;
                        }
                    }
                }

                if ( !geneGoMap.containsKey( gene1.getId() ) ) continue;
                genePair.addFirstGene( gene1 );

                for ( String gene2string : gene2Strings ) {
                    if ( gene1string.equals( gene2string ) ) {
                        continue;
                    }

                    Gene gene2 = null;

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

                    if ( !geneGoMap.containsKey( gene2.getId() ) ) continue;
                    genePair.addSecondGene( gene2 );

                    // Collections.sort( genePair, new GeneComparator() );
                    geneMap.add( genePair );

                }
            }

            // compute the median of gooverlaps and do something with the result.

        }

        saveCacheToDisk( ( HashMap ) geneCache, GENE_CACHE );
        return geneMap;
    }

    class GenePair extends ArrayList<List<Gene>> implements Comparable<GenePair> {

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            final int PRIME = 31;
            int result = 1;
            for ( Gene g : getFirstGenes() ) {
                result = PRIME * result + ( ( g == null ) ? 0 : g.hashCode() );
            }
            for ( Gene g : getSecondGenes() ) {
                result = PRIME * result + ( ( g == null ) ? 0 : g.hashCode() );
            }
            return result;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals( Object obj ) {
            if ( this == obj ) return true;
            if ( obj == null ) return false;
            if ( getClass() != obj.getClass() ) return false;
            final GenePair other = ( GenePair ) obj;

            for ( Gene g : other.getFirstGenes() ) {
                if ( !this.getFirstGenes().contains( g ) ) return false;
            }
            for ( Gene g : other.getSecondGenes() ) {
                if ( !this.getSecondGenes().contains( g ) ) return false;
            }
            return true;
        }

        public GenePair() {
            super( 0 );
            this.add( new ArrayList<Gene>() );
            this.add( new ArrayList<Gene>() );
        }

        public void addFirstGene( Gene g ) {
            if ( this.get( 0 ).contains( g ) ) return;
            this.get( 0 ).add( g );
        }

        public void addSecondGene( Gene g ) {
            if ( this.get( 1 ).contains( g ) ) return;
            this.get( 1 ).add( g );
        }

        /**
         * @return the firstGenes
         */
        public List<Gene> getFirstGenes() {
            return this.get( 0 );
        }

        /**
         * @param firstGenes the firstGenes to set
         */
        public void setFirstGenes( List<Gene> firstGenes ) {
            this.set( 0, firstGenes );
        }

        /**
         * @return the secondGenes
         */
        public List<Gene> getSecondGenes() {
            return this.get( 1 );
        }

        /**
         * @param secondGenes the secondGenes to set
         */
        public void setSecondGenes( List<Gene> secondGenes ) {
            this.set( 1, secondGenes );
        }

        @Override
        public String toString() {
            StringBuilder buf = new StringBuilder();
            Collections.sort( this.get( 0 ), new GeneComparator() );
            Collections.sort( this.get( 1 ), new GeneComparator() );
            for ( Iterator<Gene> it = this.get( 0 ).iterator(); it.hasNext(); ) {
                buf.append( it.next().getName() );
                if ( it.hasNext() ) buf.append( "," );
            }
            buf.append( "\t" );
            for ( Iterator<Gene> it = this.get( 1 ).iterator(); it.hasNext(); ) {
                buf.append( it.next().getName() );
                if ( it.hasNext() ) buf.append( "," );
            }
            return buf.toString();
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Comparable#compareTo(java.lang.Object)
         */
        public int compareTo( GenePair o2 ) {
            return this.getFirstGenes().iterator().next().getName().compareTo(
                    o2.getFirstGenes().iterator().next().getName() );
        }

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

        writer.write( "Gene1\tGene2\tScore\tG1GOTerms\tG2GOTerms\tTermOverlap\tGOTerms\n" );

        return writer;
    }

    /**
     * @param writer
     * @param pairString
     * @param overlap
     * @param goTerms
     * @param masterGOTerms
     * @param coExpGOTerms
     * @throws IOException
     */
    protected void writeOverlapLine( Writer writer, String pairString, double score, Collection<OntologyTerm> goTerms,
            int masterGOTerms, int coExpGOTerms ) throws IOException {

        if ( log.isDebugEnabled() ) log.debug( "Generating line for annotation file \n" );

        writer.write( pairString + "\t" + score + "\t" + masterGOTerms + "\t" + coExpGOTerms + "\t" );

        if ( goTerms == null || goTerms.isEmpty() ) {
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
