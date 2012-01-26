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
import java.util.Random;
import java.util.Set;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;

import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.dataStructure.matrix.SparseRaggedDoubleMatrix;
import ubic.basecode.math.RandomChooser;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.PredictedGene;
import ubic.gemma.model.genome.ProbeAlignedRegion;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.ontology.GoMetric;
import ubic.gemma.ontology.GoMetric.Metric;
import ubic.gemma.ontology.providers.GeneOntologyService;
import ubic.gemma.ontology.providers.GeneOntologyServiceImpl;
import ubic.gemma.ontology.providers.GeneOntologyServiceImpl.GOAspect;
import ubic.gemma.util.AbstractSpringAwareCLI;
import ubic.gemma.util.ConfigUtils;
import cern.colt.list.DoubleArrayList;
import cern.jet.stat.Descriptive;

/**
 * @author meeta
 * @version $Id$
 */
public class LinkEvalCli extends AbstractSpringAwareCLI {

    private static class GeneComparator implements Comparator<Gene> {

        @Override
        public int compare( Gene g1, Gene g2 ) {

            Long i1 = g1.getId();
            Long i2 = g2.getId();

            if ( i1 > i2 )
                return 1;
            else if ( i1 < i2 ) return -1;

            return 0;
        }

    }

    private static class GenePair extends ArrayList<List<Gene>> implements Comparable<GenePair> {

        /**
         * 
         */
        private static final long serialVersionUID = 1L;

        public GenePair() {
            super( 0 );
            this.add( new ArrayList<Gene>() );
            this.add( new ArrayList<Gene>() );
        }

        public void addFirstGene( Gene g ) {
            List<Gene> firstGenes = this.get( 0 );
            for ( Gene firstGene : firstGenes ) {
                if ( firstGene.getId().equals( g.getId() ) ) {
                    return;
                }
            }
            this.get( 0 ).add( g );
        }

        public void addSecondGene( Gene g ) {
            List<Gene> secondGenes = this.get( 1 );
            for ( Gene secondGene : secondGenes ) {
                if ( secondGene.getId().equals( g.getId() ) ) {
                    return;
                }
            }
            this.get( 1 ).add( g );
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Comparable#compareTo(java.lang.Object)
         */
        @Override
        public int compareTo( GenePair o2 ) {
            return this.getFirstGenes().iterator().next().getName().compareTo(
                    o2.getFirstGenes().iterator().next().getName() );
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

            boolean eqOrdered = true;
            boolean eqAlternate = true;

            List<Gene> thisFirst = this.getFirstGenes();
            List<Gene> thisSecond = this.getSecondGenes();
            List<Gene> otherFirst = other.getFirstGenes();
            List<Gene> otherSecond = other.getSecondGenes();

            // Check if first and second genes are exactly the same in both pairs
            if ( thisFirst.size() == otherFirst.size() && thisSecond.size() == otherSecond.size() ) {
                for ( int i = 0; i < thisFirst.size(); i++ ) {
                    String s1 = thisFirst.get( i ).toString();
                    String s2 = otherFirst.get( i ).toString();
                    if ( !s1.equals( s2 ) ) {
                        eqOrdered = false;
                    }
                }
                for ( int i = 0; i < thisSecond.size(); i++ ) {
                    String s1 = thisSecond.get( i ).toString();
                    String s2 = otherSecond.get( i ).toString();
                    if ( !s1.equals( s2 ) ) {
                        eqOrdered = false;
                    }
                }
            } else {
                eqOrdered = false;
            }

            // Check if first is same as second in the other, and vice versa
            if ( thisFirst.size() == otherSecond.size() && thisSecond.size() == otherFirst.size() ) {
                for ( int i = 0; i < thisFirst.size(); i++ ) {
                    String s1 = thisFirst.get( i ).toString();
                    String s2 = otherSecond.get( i ).toString();
                    if ( !s1.equals( s2 ) ) {
                        eqAlternate = false;
                    }
                }
                for ( int i = 0; i < thisSecond.size(); i++ ) {
                    String s1 = thisSecond.get( i ).toString();
                    String s2 = otherFirst.get( i ).toString();
                    if ( !s1.equals( s2 ) ) {
                        eqAlternate = false;
                    }
                }
            } else {
                eqAlternate = false;
            }

            // if gene pairs are equal in the same order or equal in alternate order, they are equal
            if ( eqOrdered || eqAlternate ) {
                return true;
            }
            return false;

        }

        /**
         * @return the firstGenes
         */
        public List<Gene> getFirstGenes() {
            return this.get( 0 );
        }

        /**
         * @return the secondGenes
         */
        public List<Gene> getSecondGenes() {
            return this.get( 1 );
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            final int PRIME = 31;
            int result1 = 1;
            int result2 = 1;
            int result3 = 1;
            int result4 = 1;
            for ( int i = 0; i < getFirstGenes().size(); i++ ) {
                Gene g = getFirstGenes().get( i );
                result1 = PRIME * result1 + ( ( g == null ) ? 0 : g.hashCode() );
            }
            for ( int i = getFirstGenes().size() - 1; i >= 0; i-- ) {
                Gene g = getFirstGenes().get( i );
                result2 = PRIME * result2 + ( ( g == null ) ? 0 : g.hashCode() );
            }
            for ( int i = 0; i < getSecondGenes().size(); i++ ) {
                Gene g = getSecondGenes().get( i );
                result3 = PRIME * result3 + ( ( g == null ) ? 0 : g.hashCode() );
            }
            for ( int i = getSecondGenes().size() - 1; i >= 0; i-- ) {
                Gene g = getSecondGenes().get( i );
                result4 = PRIME * result4 + ( ( g == null ) ? 0 : g.hashCode() );
            }
            return result1 * result2 * result3 * result4;
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

    }

    private static class ProbeComparator implements Comparator<CompositeSequence> {

        @Override
        public int compare( CompositeSequence o1, CompositeSequence o2 ) {

            Long g1 = o1.getId();
            Long g2 = o2.getId();

            if ( g1 > g2 )
                return 1;
            else if ( g1 < g2 ) return -1;
            return 0;
        }

    }

    private static final String GENE_CACHE = "geneCache";

    private static final String GENE_GO_MAP_FILE = "GeneGOMap";

    private static final String GO_PROB_MAP = "GoProbMap";

    private static final String HOME_DIR = ConfigUtils.getString( "gemma.appdata.home" );

    private static final String RANDOM_SUBSET = "RandomSubset1K";

    private static Integer SET_SIZE = 0;

    private static final String VECTOR_MATRIX = "VectorMatrix";

    public static void main( String[] args ) {
        LinkEvalCli p = new LinkEvalCli();
        try {
            Exception ex = p.doWork( args );
            if ( ex != null ) {
                ex.printStackTrace();
            }
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    private DoubleMatrix<Long, String> geneVectorMatrix = new SparseRaggedDoubleMatrix<Long, String>();

    private Map<String, Integer> GOcountMap = new HashMap<String, Integer>();

    private Map<String, Double> GOProbMap = new HashMap<String, Double>();

    private String adShortName = "";// holds inputted string indicating array design short name

    private ArrayDesign arrayDesign = null; // holds actual array design used

    private ArrayDesignService arrayDesignService;

    private String termsOutPath = null;

    private int cCount = 0;

    private String component = "http://purl.org/obo/owl/GO#GO_0005575";

    private CompositeSequenceService css;

    private int fCount = 0;

    private String file_path = "";

    private int firstProbeColumn = 0;

    private List<CompositeSequence> firstProbes;

    private String function = "http://purl.org/obo/owl/GO#GO_0003674";

    private Map<String, Gene> geneCache = new HashMap<String, Gene>();

    private Map<Long, Collection<String>> geneGoMap = new HashMap<Long, Collection<String>>();

    // A list of service beans
    private GeneService geneService;

    private Collection<Gene> goMappedGenes;

    private GoMetric goMetric;

    private boolean max = false;

    private final int MAX_GO_SIM = 200;

    private Metric metric = GoMetric.Metric.simple;

    private boolean noZeros = false;// true when gene pairs with genes containing no GO terms wish to be excluded

    private int numberOfRandomRuns = 1;

    private GeneOntologyService goService;

    private String outFile;

    private String outRandLinksFile;

    // INCLUDE PARTOF OR CHANGE STRINGENCY
    private boolean partOf = true;

    private int pCount = 0;

    private boolean printRandomLinks = false;

    private String probenames_file_path = "";

    private String process = "http://purl.org/obo/owl/GO#GO_0008150";

    // inputted

    private boolean randomFromArray = false;// true when selecting random pairs from array design is desired

    private boolean randomFromSubset = false;// true when selecting random probe pairs from a given file is desired

    private boolean randomFromTaxon = false;// true when selecting random gene pairs from a particular genome(human,
    // mouse, rat) is desired

    private Map<String, Integer> rootMap = new HashMap<String, Integer>();

    private int secondProbeColumn = 1;

    private List<CompositeSequence> secondProbes;

    private Map<CompositeSequence, Collection<Gene>> probemap;

    private boolean selectSubset = false;

    private int subsetLinks = 0;

    private Double subsetSize = 0.0;

    private boolean subsetUsed = false;

    private Taxon taxon;

    private TaxonService taxonService;

    private boolean weight = false;

    private GOAspect goAspectToUse = null;

    /**
     * @param f
     * @return
     */
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

    /**
     * @param toSave
     * @param filename
     */
    public void saveCacheToDisk( Object toSave, String filename ) {

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

    @Override
    @SuppressWarnings("static-access")
    protected void buildOptions() {
        Option goMetricOption = OptionBuilder.hasArg().withArgName( "Choice of GO Metric" ).withDescription(
                "resnik, lin, jiang, percent, cosine, kappa; default = simple" ).withLongOpt( "metric" ).create( 'm' );
        addOption( goMetricOption );

        Option maxOption = OptionBuilder.hasArg().withArgName( "Choice of using MAX calculation" ).withDescription(
                "MAX" ).withLongOpt( "max" ).create( 'x' );
        addOption( maxOption );

        Option weightedOption = OptionBuilder.hasArg().withArgName( "Choice of using weighted matrix" )
                .withDescription( "weight" ).withLongOpt( "weight" ).create( 'w' );
        addOption( weightedOption );

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
                "Column index with gene2 (starting from 0; default=1)" ).create( "g2col" );
        addOption( firstGeneOption );
        addOption( secondGeneOption );

        Option arrayDesignOption = OptionBuilder.hasArg().isRequired().withArgName( "Array Design" ).withDescription(
                "Short Name of Microarray Design" ).create( "array" );
        addOption( arrayDesignOption );

        Option numberOfRandomRunsOption = OptionBuilder
                .hasArg()
                .withArgName( "Number of Random Runs" )
                .withDescription(
                        "Number of runs for random gene pair selection from array design (starting from 1; default = 1)" )
                .create( "runs" );
        addOption( numberOfRandomRunsOption );

        Option outFileOption = OptionBuilder.hasArg().isRequired().withArgName( "outFile" ).withDescription(
                "Write output to this file" ).create( 'o' );
        addOption( outFileOption );

        Option noZeroGo = OptionBuilder.withDescription( "Exclude genes with no GO terms" ).create( "noZeroGo" );
        addOption( noZeroGo );

        Option print = OptionBuilder.hasArg().withArgName( "Output file for random links" ).withDescription(
                "Print out randomly chosen probe pairs in a separate output file" ).create( "print" );
        addOption( print );

        Option probenames = OptionBuilder
                .hasArg()
                .withArgName( "File containing subset of probe names" )
                .withDescription(
                        "File containing subset of probe names associated with selected array design from which to choose random pairs" )
                .create( "probenames" );
        addOption( probenames );

        Option subsetOption = OptionBuilder.hasArg().withArgName(
                "Approximate number of probe pairs desired to score from full set" ).withDescription(
                "Take random subset of probe pairs approximated to given argument from input file to score" ).create(
                "subset" );
        addOption( subsetOption );

        Option aspectOption = OptionBuilder.hasArg().withArgName( "aspect" ).withDescription(
                "Limit to mf, bp or cc. Default=use all three" ).create( "aspect" );
        addOption( aspectOption );

        Option outputGoAnnots = OptionBuilder.hasArg().withArgName( "path" ).withDescription(
                "Also rint out the Gene-GO relationships in a tabbed format" ).create( "termsout" );
        addOption( outputGoAnnots );

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#doWork(java.lang.String[])
     */
    @Override
    protected Exception doWork( String[] args ) {

        Exception err = processCommandLine( "Compute Go Overlap ", args );
        if ( err != null ) return err;

        StringBuilder buf = new StringBuilder();
        buf.append( "" );

        this.arrayDesign = arrayDesignService.findByShortName( adShortName );
        this.arrayDesign = arrayDesignService.thawLite( this.arrayDesign );
        if ( this.arrayDesign == null ) {
            System.out.println( "Array design " + adShortName + " not found" );
            System.exit( 0 );
        }

        Collection<CompositeSequence> probes = null;

        if ( randomFromSubset ) {// subset desired from file containing probe NAME pairs
            if ( probenames_file_path.equals( "" ) ) {
                System.out.println( "Must enter a valid filepath for probe names file" );
                System.exit( 0 );
            }
            try {
                File fl = new File( probenames_file_path );
                probes = getProbesFromSubset( fl );
            } catch ( IOException e ) {
                return e;
            }
        } else {
            probes = this.arrayDesign.getCompositeSequences();
        }

        this.populateGeneGoMapForTaxon();

        probemap = css.getGenes( probes );

        if ( this.randomFromArray ) { // randomly select links from array design

            if ( numberOfRandomRuns > 1 ) {
                int[][] results = new int[numberOfRandomRuns][];

                for ( int i = 0; i < numberOfRandomRuns; i++ ) {
                    Collection<GenePair> randomPairs = getRandomPairsFromProbes( SET_SIZE );
                    Map<GenePair, Double> scoreMap = scorePairs( randomPairs );
                    addResults( i, results, scoreMap );
                }

                summarizeResults( results );

                if ( this.printRandomLinks ) {
                    outputRandomLinks();
                }
            } else {// if only 1 run, no need to generate histogram data, produce regular format for GO scores
                Collection<GenePair> randomPairs = getRandomPairsFromProbes( SET_SIZE );
                Map<GenePair, Double> scoreMap = scorePairs( randomPairs );
                writeGoSimilarityResults( scoreMap );

                if ( this.printRandomLinks ) {
                    outputRandomLinks();
                }
            }

        } else {
            Collection<GenePair> genePairs = getLinks();

            Map<GenePair, Double> scoreMap = scorePairs( genePairs );

            writeGoSimilarityResults( scoreMap );
        }

        if ( termsOutPath != null ) {
            try {
                writeGoMap();
            } catch ( IOException e ) {
                return e;
            }

        }

        return null;

    }

    protected void initBeans() {
        taxonService = ( TaxonService ) getBean( "taxonService" );
        geneService = ( GeneService ) getBean( "geneService" );
        goService = ( GeneOntologyService ) getBean( "geneOntologyService" );
        goMetric = ( GoMetric ) getBean( "goMetric" );
        arrayDesignService = ( ArrayDesignService ) getBean( "arrayDesignService" );
        css = ( CompositeSequenceService ) getBean( "compositeSequenceService" );

    }

    /**
     * Opens a file for writing and adds the header for histogram data for selecting random gene pairs from an array
     * design
     * 
     * @param fileName if Null, output will be written to standard output.
     * @throws IOException
     */
    protected Writer initHistFile( String fileName ) throws IOException {

        Writer writer;
        if ( StringUtils.isBlank( fileName ) ) {
            log.info( "Output to stdout" );
            writer = new PrintWriter( System.out );
        } else {

            // write into file
            log.info( "Creating new link eval file " + fileName + " \n" );

            File f = new File( fileName );

            if ( f.exists() ) {
                log.warn( "Will overwrite existing file " + f );
                f.delete();
            }

            f.createNewFile();
            writer = new FileWriter( f );
        }

        writer.write( "Overlap" );
        for ( int j = 0; j < numberOfRandomRuns; j++ ) {
            writer.write( "\tRun_" + ( j + 1 ) );
        }
        // writer.write( buf.toString() );
        writer.write( "\tMean\tSDev\tMeanF\tStdevF\n" );

        return writer;
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

            File f = new File( fileName );

            if ( f.exists() ) {
                log.warn( "Will overwrite existing file " + f );
                f.delete();
            }

            f.createNewFile();
            writer = new FileWriter( f );
        }
        // writer.write( buf.toString() );
        if ( this.subsetUsed ) {
            writer.write( "# scoresGenerated:" + subsetLinks + "\n" );
        }

        writer.write( "Gene1\tGene2\tScore\tG1GOTerms\tG2GOTerms\tOverlapTerms\n" );

        return writer;
    }

    /**
     * Opens a file for writing scores for the randomly selected probe pairs
     * 
     * @param fileName if Null, output will be written to standard output.
     * @throws IOException
     */
    protected Writer initRandLinksFile( String fileName ) throws IOException {

        Writer writer;
        if ( StringUtils.isBlank( fileName ) ) {
            log.info( "Output to stdout" );
            writer = new PrintWriter( System.out );
        } else {

            // write into file
            log.info( "Creating new annotation file " + fileName + " \n" );

            File f = new File( fileName );

            if ( f.exists() ) {
                log.warn( "Will overwrite existing file " + f );
                f.delete();
            }

            f.createNewFile();
            writer = new FileWriter( f );
        }

        return writer;
    }

    @Override
    protected void processOptions() {
        super.processOptions();
        initBeans();
        String commonName = getOptionValue( 't' );
        if ( StringUtils.isBlank( commonName ) ) {
            System.out.println( "MUST enter a valid taxon!" );
            System.exit( 0 );
        }
        if ( !StringUtils.equalsIgnoreCase( "mouse", commonName ) && !StringUtils.equalsIgnoreCase( "rat", commonName )
                && !StringUtils.equalsIgnoreCase( "human", commonName ) ) {
            System.out.println( "MUST enter a valid taxon!" );
            System.exit( 0 );
        }

        this.taxon = taxonService.findByCommonName( commonName );
        if ( taxon == null ) {
            System.out.println( "Taxon " + commonName + " not found." );
            System.exit( 0 );
        }
        String input = getOptionValue( 'd' );
        if ( StringUtils.isBlank( input ) ) {
            System.out.println( "You MUST enter a valid filename OR size of dataset" );
            System.exit( 0 );
        }
        this.adShortName = getOptionValue( "array" );
        if ( StringUtils.isBlank( this.adShortName ) ) {
            System.out.println( "You MUST enter the shortname for the associated microarray design" );
            System.exit( 0 );
        }
        if ( StringUtils.isNumeric( input ) ) {// set size entered
            if ( this.adShortName.equals( "n/a" ) ) {
                this.randomFromTaxon = true;
            } else {
                this.randomFromArray = true;
            }

            SET_SIZE = Integer.parseInt( input );
            if ( SET_SIZE < 1 ) {
                System.out.println( "Random set size must be a positive integer" );
                System.exit( 0 );
            }
            System.out.println( "Will create a set of " + SET_SIZE + " random gene pairs!" );
            if ( this.hasOption( "runs" ) ) {
                String runs = getOptionValue( "runs" );
                if ( StringUtils.isNumeric( runs ) ) {
                    numberOfRandomRuns = Integer.parseInt( runs );
                    if ( numberOfRandomRuns < 1 ) {
                        System.out.println( "Number of random runs must be a positive integer" );
                        System.exit( 0 );
                    }
                } else {
                    System.out.println( "Number of random runs must be a numeric value" );
                    System.exit( 0 );
                }
            }
        } else {// path to input file entered
            File f = new File( input );
            if ( f.canRead() ) {
                this.file_path = input;
                if ( this.hasOption( "subset" ) ) {
                    String size = getOptionValue( "subset" );
                    if ( StringUtils.isNumeric( size ) ) {
                        this.subsetSize = Double.parseDouble( size );
                        this.selectSubset = true;
                        log.info( "Approximate subset of scores desired" );
                    } else {
                        System.out.println( "Subset size must be a numeric value" );
                        System.exit( 0 );
                    }
                }
            } else {
                System.out.println( input
                        + "is NOT a valid filename! You MUST enter a valid filename OR size of dataset" );
                System.exit( 0 );
            }
        }
        if ( hasOption( 'x' ) ) {
            String maxs = getOptionValue( 'x' );
            if ( maxs.equalsIgnoreCase( "MAX" ) )
                this.max = true;
            else
                this.max = false;
        }

        if ( hasOption( 'w' ) ) {
            String weights = getOptionValue( 'w' );
            if ( weights.equalsIgnoreCase( "weight" ) )
                this.weight = true;
            else
                this.weight = false;
        }

        if ( hasOption( "termsout" ) ) {
            this.termsOutPath = getOptionValue( "termsout" );
            log.info( "GO mapping will be saved as tabbed text to " + this.termsOutPath );
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
            else if ( metricName.equalsIgnoreCase( "cosine" ) )
                this.metric = GoMetric.Metric.cosine;
            else if ( metricName.equalsIgnoreCase( "kappa" ) )
                this.metric = GoMetric.Metric.kappa;
            else {
                this.metric = GoMetric.Metric.simple;
                this.max = false;
            }
        }

        if ( this.hasOption( "g1col" ) ) {
            this.firstProbeColumn = getIntegerOptionValue( "g1col" );
        }

        if ( this.hasOption( "g2col" ) ) {
            this.secondProbeColumn = getIntegerOptionValue( "g2col" );
        }

        if ( this.hasOption( "noZeroGo" ) ) {
            this.noZeros = true;
        }

        if ( this.hasOption( "print" ) ) {
            this.printRandomLinks = true;
            this.outRandLinksFile = getOptionValue( "print" );
            if ( StringUtils.isBlank( outRandLinksFile ) ) {
                System.out.println( "You must enter an output file path for printing random probe pairs" );
                System.exit( 0 );
            }
        }

        if ( this.hasOption( "probenames" ) ) {
            this.randomFromSubset = true;
            this.probenames_file_path = getOptionValue( "probenames" );
        }

        if ( this.hasOption( "aspect" ) ) {
            String aspect = getOptionValue( "aspect" );
            if ( aspect.equals( "mf" ) ) {
                this.goAspectToUse = GOAspect.MOLECULAR_FUNCTION;
            } else if ( aspect.equals( "bp" ) ) {
                this.goAspectToUse = GOAspect.BIOLOGICAL_PROCESS;
            } else if ( aspect.equals( "cc" ) ) {
                this.goAspectToUse = GOAspect.CELLULAR_COMPONENT;

            } else {
                System.out.println( "Aspect must be one of bp, mf or cc" );
                System.exit( 0 );
            }
        }

        outFile = getOptionValue( 'o' );
        initGO();
    }

    /**
     * @param writer
     * @param pairString
     * @param score e.g. overlap
     * @param goTerms - terms in the overlap
     * @param masterGOTerms number of go terms for the first gene
     * @param coExpGOTerms number of go terms for the second gene
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
                writer.write( "|" + GeneOntologyServiceImpl.asRegularGoId( oe ) );
            else
                writer.write( GeneOntologyServiceImpl.asRegularGoId( oe ) );
            wrote = true;
        }

        writer.write( "\n" );
        writer.flush();
    }

    /**
     * Assumes using TO
     * 
     * @param currentRunIndex
     * @param results
     * @param scoreMap
     */
    private void addResults( int currentRunIndex, int[][] results, Map<GenePair, Double> scoreMap ) {
        results[currentRunIndex] = new int[MAX_GO_SIM];
        for ( int i = 0; i < MAX_GO_SIM; i++ ) {
            results[currentRunIndex][i] = 0; // init.
        }

        for ( Double s : scoreMap.values() ) {
            int sim = ( int ) Math.floor( s.doubleValue() );

            if ( sim >= MAX_GO_SIM ) {
                throw new IllegalArgumentException( "sim was " + sim + "; increase MAX_GO_SIM" );
            }

            results[currentRunIndex][sim]++;
        }

    }

    /**
     * @param genePair
     * @return
     */
    private boolean checkGenePair( GenePair genePair ) {
        boolean isAKeeper = true;
        if ( genePair == null ) {
            return false;
        } else if ( genePair.getFirstGenes().isEmpty() || genePair.getSecondGenes().isEmpty() ) {
            return false;
        } else {// check if genes are equal
            List<Gene> first = genePair.getFirstGenes();
            List<Gene> second = genePair.getSecondGenes();
            for ( int i = 0; i < first.size() && isAKeeper == true; i++ ) {
                String s1 = first.get( i ).toString();
                for ( int j = 0; j < second.size() && isAKeeper == true; j++ ) {
                    String s2 = second.get( j ).toString();
                    if ( s1.equals( s2 ) ) {
                        isAKeeper = false;
                    }
                }
            }
        }
        return isAKeeper;
    }

    /**
     * 
     */
    @SuppressWarnings("unchecked")
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
            GOProbMap = makeProbMap();

            this.saveCacheToDisk( GOProbMap, GO_PROB_MAP );
        }
    }

    @SuppressWarnings("unchecked")
    private void createGene2TermMatrix() {

        File f3 = new File( HOME_DIR + File.separatorChar + VECTOR_MATRIX );
        if ( f3.exists() ) {
            geneVectorMatrix = ( DoubleMatrix<Long, String> ) getCacheFromDisk( f3 );
            log.info( "Found vector matrix file!" );
        }

        else {
            log.info( "Creating sparse matrix... " );

            geneVectorMatrix = goMetric.createVectorMatrix( geneGoMap, weight );

            this.saveCacheToDisk( geneVectorMatrix, VECTOR_MATRIX );
        }

    }

    /**
     * @param taxon
     * @return
     */
    private String getGeneGOMapFileName() {
        return GENE_GO_MAP_FILE + "." + taxon.getCommonName();
    }

    /**
     * @param probemap
     * @return
     */
    private Collection<GenePair> getLinks() {
        Collection<GenePair> genePairs = new HashSet<GenePair>();

        if ( randomFromTaxon ) {
            genePairs = loadRandomPairs();
        } else if ( StringUtils.isNotBlank( file_path ) ) {
            try {
                File linkFile = new File( file_path );
                genePairs = loadLinks( linkFile );
                log.info( "Done loading data..." );
            } catch ( IOException e ) {
                throw new RuntimeException( e );
            }

        } else {
            log.error( "What should I do?" );
            bail( ErrorCode.INVALID_OPTION );
        }
        return genePairs;
    }

    /**
     * @param merged1
     * @param merged2
     * @return
     */
    private Collection<OntologyTerm> getMergedTermOverlap( Set<String> merged1, Set<String> merged2 ) {

        Collection<OntologyTerm> overlapTerms = new HashSet<OntologyTerm>();

        if ( ( merged2 == null ) || merged2.isEmpty() ) return null;

        if ( ( merged1 == null ) || merged1.isEmpty() ) return null;

        for ( String goTerm1 : merged1 ) {
            if ( goTerm1.equalsIgnoreCase( process ) || goTerm1.equalsIgnoreCase( function )
                    || goTerm1.equalsIgnoreCase( component ) ) continue;
            for ( String goTerm2 : merged2 ) {

                if ( goTerm2.equalsIgnoreCase( process ) || goTerm2.equalsIgnoreCase( function )
                        || goTerm2.equalsIgnoreCase( component ) ) continue;

                if ( goTerm1.equalsIgnoreCase( goTerm2 ) )
                    overlapTerms.add( GeneOntologyServiceImpl.getTermForURI( goTerm1 ) );
            }
        }

        return overlapTerms;
    }

    /**
     * @param file containing probe names(not IDs) in first column
     * @return collection of composite sequences (probes) obtained from file
     */
    private Collection<CompositeSequence> getProbesFromSubset( File f ) throws IOException {

        BufferedReader in = new BufferedReader( new FileReader( f ) );
        Collection<CompositeSequence> probeSubset = new HashSet<CompositeSequence>();

        String line;
        log.info( "Loading probes from " + f );
        while ( ( line = in.readLine() ) != null ) {
            line = line.trim();
            if ( line.startsWith( "#" ) ) {
                continue;
            }

            String[] fields = StringUtils.split( line, "\t" );

            String prb = fields[0];
            CompositeSequence cs = css.findByName( arrayDesign, prb );
            probeSubset.add( cs );
        }
        return probeSubset;
    }

    /**
     * @param take a collection of genes and size of susbset
     * @return a collection of random gene pairs that have GO annotations
     */
    @SuppressWarnings("unused")
    private Collection<GenePair> getRandomPairs( int size, Collection<Gene> genes ) {

        Collection<GenePair> subsetPairs = new HashSet<GenePair>();
        int i = 0;

        while ( i < size ) {
            List<Gene> twoGenes = new ArrayList<Gene>( RandomChooser.chooseRandomSubset( 2, genes ) );

            if ( twoGenes.size() != 2 ) {
                log.warn( "A pair consists of two objects. More than two is unacceptable. Fix it!!" );
            }
            Collections.sort( twoGenes, new GeneComparator() );

            GenePair genePair = new GenePair();

            Gene g1 = twoGenes.get( 0 );
            Gene g2 = twoGenes.get( 1 );

            genePair.addFirstGene( g1 );
            genePair.addSecondGene( g2 );

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

    /**
     * @param take a collection of probes and size of susbset
     * @return a collection of random gene pairs that may or may not have GO annotations
     */
    private Collection<GenePair> getRandomPairsFromProbes( int size ) {

        Collection<GenePair> subsetPairs = new HashSet<GenePair>();

        firstProbes = new ArrayList<CompositeSequence>();
        secondProbes = new ArrayList<CompositeSequence>();
        List<CompositeSequence> probes = new ArrayList<CompositeSequence>( probemap.keySet() );
        Collections.sort( probes, new ProbeComparator() );

        while ( subsetPairs.size() < size ) {

            List<CompositeSequence> twoProbes = new ArrayList<CompositeSequence>( RandomChooser.chooseRandomSubset( 2,
                    probes ) );

            if ( twoProbes.size() != 2 ) {
                log.debug( "A pair consists of two objects. More than two is unacceptable. Fix it!!" );
            }

            // Collections.sort( twoProbes, new ProbeComparator() );

            Collection<Gene> firstGenes = probemap.get( twoProbes.get( 0 ) );
            Collection<Gene> secondGenes = probemap.get( twoProbes.get( 1 ) );

            GenePair genePair = makeGenePair( firstGenes, secondGenes );

            boolean isAKeeper = checkGenePair( genePair );

            if ( !isAKeeper ) {
                continue;
            }

            subsetPairs.add( genePair );
            log.debug( "Added pair to subset!" );

            firstProbes.add( twoProbes.get( 0 ) );
            secondProbes.add( twoProbes.get( 1 ) );
        }

        return subsetPairs;
    }

    /**
     * @param g
     * @param coexpG
     * @return
     */
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

                if ( ontologyEntryC.equalsIgnoreCase( process ) || ontologyEntryC.equalsIgnoreCase( function )
                        || ontologyEntryC.equalsIgnoreCase( component ) ) continue;

                if ( ontologyEntry.equalsIgnoreCase( ontologyEntryC ) )
                    overlapTerms.add( GeneOntologyServiceImpl.getTermForURI( ontologyEntry ) );
            }
        }

        return overlapTerms;
    }

    /**
     * 
     */
    private void initGO() {
        /*
         * Initialize the Gene Ontology.
         */
        this.goService.init( true );

        while ( !goService.isReady() ) {
            log.info( "waiting for ontology load.." );
            try {
                Thread.sleep( 10000 );
                if ( !goService.isRunning() ) break;
            } catch ( InterruptedException e ) {
                throw new RuntimeException( e );
            }
        }

        if ( !goService.isReady() ) {
            throw new RuntimeException( "Gene Ontology was not loaded successfully." );
        }
    }

    private Collection<GenePair> loadLinks( File f ) throws IOException {

        log.info( "Loading data from " + f );
        BufferedReader in = new BufferedReader( new FileReader( f ) );

        Collection<GenePair> geneMap = new HashSet<GenePair>();
        String line;
        Double printedLinks = -1.0;
        Random generator = new Random();
        double rand = 0.0;
        double fraction = 0.0;
        boolean alreadyWarned = false;

        while ( ( line = in.readLine() ) != null ) {
            line = line.trim();
            if ( line.startsWith( "#" ) ) {
                if ( line.contains( "printedLinks" ) ) {
                    int ind = line.indexOf( ':' );
                    printedLinks = Double.parseDouble( line.substring( ind + 1 ) );
                    fraction = this.subsetSize / printedLinks;
                }
                continue;
            }
            if ( printedLinks == -1.0 ) {
                System.out.println( "Printed link count not found in file header" );
                System.exit( 0 );
            }
            if ( selectSubset && printedLinks > this.subsetSize ) {
                this.subsetUsed = true;
                rand = generator.nextDouble();
                if ( rand > fraction ) continue;
            }
            String[] fields = StringUtils.split( line, "\t" );

            if ( fields.length < 2 ) {
                if ( !alreadyWarned ) {
                    log.warn( "Bad field on line: " + line + " (subsequent errors suppressed)" );
                    alreadyWarned = true;
                }
                continue;
            }

            String g1 = fields[firstProbeColumn];
            String g2 = fields[secondProbeColumn];

            /*
             * Use the probe field, get the gene mapping from the probemap
             */
            CompositeSequence cs1 = css.load( Long.parseLong( g1 ) );
            CompositeSequence cs2 = css.load( Long.parseLong( g2 ) );

            Collection<Gene> genes1 = probemap.get( cs1 );
            Collection<Gene> genes2 = probemap.get( cs2 );

            GenePair genePair = null;

            if ( genes1 == null ) {
                log.warn( "No genes found for probe ID " + g1 + " in array design" );
            } else if ( genes2 == null ) {
                log.warn( "No genes found for probe ID " + g2 + " in array design" );
            } else {
                genePair = makeGenePair( genes1, genes2 );
            }

            if ( !this.checkGenePair( genePair ) ) {
                continue;
            }

            geneMap.add( genePair );

            if ( geneMap.size() % 50000 == 0 ) {
                log.info( "Loaded " + geneMap.size() + " links" );
            }
            // compute the median of gooverlaps and do something with the
            // result.

        }
        log.info( "Loaded " + geneMap.size() + " links" );
        saveCacheToDisk( geneCache, GENE_CACHE );
        return geneMap;
    }

    /**
     * @return
     */
    private Collection<GenePair> loadRandomPairs() {
        Collection<GenePair> randomPairs = null;

        try {
            File f3 = new File( HOME_DIR + File.separatorChar + RANDOM_SUBSET );
            if ( f3.exists() ) {
                randomPairs = loadLinks( f3 );
                log.info( "Found cached subset file!" );
            }

            else {
                populateGeneGoMapForTaxon();

                assert !this.goMappedGenes.isEmpty();

                // randomPairs = getRandomPairs( SET_SIZE, this.goMappedGenes );

                // Writer w = new FileWriter( f3 );
                // this.writeLinks( randomPairs, w );
            }
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }

        return randomPairs;
    }

    /**
     * @param firstGenes
     * @param secondGenes
     * @return
     */
    private GenePair makeGenePair( Collection<Gene> firstGenes, Collection<Gene> secondGenes ) {
        GenePair genePair = new GenePair();
        for ( Gene firstGene : firstGenes ) {
            if ( noZeros
                    && ( geneGoMap.get( firstGene.getId() ) == null || geneGoMap.get( firstGene.getId() ).isEmpty() ) ) {// exclude
                // genes
                // with
                // zero
                // GO
                // terms
                // if
                // desired
                continue;
            }
            if ( !( firstGene instanceof PredictedGene ) && !( firstGene instanceof ProbeAlignedRegion ) ) {// known
                // gene
                genePair.addFirstGene( firstGene );
            }
        }

        for ( Gene secondGene : secondGenes ) {
            if ( noZeros
                    && ( geneGoMap.get( secondGene.getId() ) == null || geneGoMap.get( secondGene.getId() ).isEmpty() ) ) {// exclude
                // genes
                // with
                // zero
                // GO
                // terms
                // if
                // desired
                continue;
            }

            if ( !( secondGene instanceof PredictedGene ) && !( secondGene instanceof ProbeAlignedRegion ) ) {// known
                // gene
                genePair.addSecondGene( secondGene );
            }
        }

        return genePair;
    }

    private Map<String, Double> makeProbMap() {

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

    /**
     * @param take a collection of GOTerm URIs
     * @return Identify the root of each term and put it in the rootMap
     */
    private void makeRootMap( Collection<String> terms ) {

        Collection<String> remove = new HashSet<String>();

        for ( String t : terms ) {
            Collection<OntologyTerm> parents = goService.getAllParents( GeneOntologyServiceImpl.getTermForURI( t ), partOf );

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
     * Organizes a list of genes in to different bins, identified by gene name, with each bin containing duplicates
     * found of different genes with the same gene name.
     * 
     * @param genes list of genes to be organized
     * @return a map of organized genes with the value being the collection of duplicate genes and the key being the
     *         common gene name among them
     */
    private Map<String, List<Gene>> organizeDuplicates( List<Gene> genes ) {
        Map<String, List<Gene>> organizedGeneBins = new HashMap<String, List<Gene>>();
        String tempName = "";
        Set<String> uniqueGeneNames = new HashSet<String>();
        for ( Gene tempGene : genes ) {
            tempName = tempGene.getName();
            if ( uniqueGeneNames.contains( tempName ) ) {// gene already encountered; duplicate
                List<Gene> tempGeneBin = organizedGeneBins.get( tempName );
                tempGeneBin.add( tempGene );
                organizedGeneBins.put( tempName, tempGeneBin );
            } else {
                uniqueGeneNames.add( tempName );
                List<Gene> tempGeneBin = new ArrayList<Gene>();
                tempGeneBin.add( tempGene );
                organizedGeneBins.put( tempName, tempGeneBin );
            }
        }
        return organizedGeneBins;
    }

    private void outputRandomLinks() {
        try {
            assert firstProbes.size() == secondProbes.size() : "yikes";
            Writer w = initRandLinksFile( outRandLinksFile );
            for ( int i = 0; i < firstProbes.size(); i++ ) {
                w.write( firstProbes.get( i ).getId() + "\t" + secondProbes.get( i ).getId() + "\t\n" );
            }
            w.flush();
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * @param genes
     */
    private void populateGeneGoMap( Collection<Gene> genes ) {
        if ( genes.isEmpty() ) {
            throw new IllegalArgumentException( "No genes!" );
        }
        this.goMappedGenes = genes;
        int hadTerms = 0;
        for ( Gene gene : genes ) {
            Collection<OntologyTerm> GOTerms = goService.getGOTerms( gene, partOf, goAspectToUse );

            if ( GOTerms == null || GOTerms.isEmpty() ) continue;
            if ( log.isDebugEnabled() ) log.debug( "Got go terms for " + gene.getName() );

            hadTerms++;

            Collection<String> termString = new HashSet<String>();
            for ( OntologyTerm oe : GOTerms ) {
                termString.add( oe.getUri() );
            }
            geneGoMap.put( gene.getId(), termString );
        }

        log.info( hadTerms + " genes had GO terms" );

        if ( hadTerms == 0 ) {
            throw new IllegalStateException( "NO genes had go terms" );
        }
    }

    /**
     * 
     */
    @SuppressWarnings("unchecked")
    private void populateGeneGoMapForTaxon() {

        File f = new File( HOME_DIR + File.separatorChar + getGeneGOMapFileName() );
        if ( !f.exists() ) {
            log.info( "Reloading GO mapping" );
            rebuildTaxonGeneGOMap();
        } else {
            log.info( "Loading GO mapping from disk cache" );
            this.geneGoMap = ( Map<Long, Collection<String>> ) getCacheFromDisk( f );
        }

        if ( metric.equals( GoMetric.Metric.resnik ) || metric.equals( GoMetric.Metric.lin )
                || metric.equals( GoMetric.Metric.jiang ) ) {
            log.info( "Computing term probabilities for all GOterms ..." );
            computeTermProbabilities();
        } else if ( metric.equals( GoMetric.Metric.cosine ) || metric.equals( GoMetric.Metric.kappa ) ) {
            createGene2TermMatrix();
        }

    }

    /**
     * @param taxon
     */
    private void rebuildTaxonGeneGOMap() {
        Collection<Gene> genes = geneService.loadKnownGenes( this.taxon );
        populateGeneGoMap( genes );
        saveCacheToDisk( geneGoMap, getGeneGOMapFileName() );
    }

    /**
     * @param genePairs
     * @return
     */
    private Map<GenePair, Double> scorePairs( Collection<GenePair> genePairs ) {
        Map<GenePair, Double> scoreMap = new HashMap<GenePair, Double>();
        log.info( genePairs.size() + " pairs to score" );

        for ( GenePair pair : genePairs ) {

            List<Gene> genes1 = pair.getFirstGenes();
            List<Gene> genes2 = pair.getSecondGenes();

            List<Double> scores = new ArrayList<Double>();

            if ( metric.equals( GoMetric.Metric.simple ) ) {
                Map<String, List<Gene>> orgGenes1 = organizeDuplicates( genes1 );
                Map<String, List<Gene>> orgGenes2 = organizeDuplicates( genes2 );

                // calculate scores
                for ( String geneName1 : orgGenes1.keySet() ) {
                    List<Gene> sameGenes1 = orgGenes1.get( geneName1 );
                    for ( String geneName2 : orgGenes2.keySet() ) {
                        List<Gene> sameGenes2 = orgGenes2.get( geneName2 );
                        if ( sameGenes1.get( 0 ).getId() == sameGenes2.get( 0 ).getId() ) continue;
                        double mergedScore = 0.0;
                        mergedScore = goMetric.computeMergedOverlap( sameGenes1, sameGenes2, geneGoMap, goAspectToUse );
                        scores.add( mergedScore );
                    }
                }

            } else {

                if ( this.goAspectToUse != null ) {
                    throw new IllegalArgumentException(
                            "Sorry, you can't use go aspect filtering with that metric - supported for overlap only" );
                }

                for ( Gene g1 : genes1 ) {
                    for ( Gene g2 : genes2 ) {
                        if ( g1.getId().equals( g2.getId() ) ) continue;
                        double score = 0.0;

                        if ( metric.equals( GoMetric.Metric.cosine ) || metric.equals( GoMetric.Metric.kappa ) ) {
                            score = goMetric.computeMatrixSimilarity( g1, g2, geneVectorMatrix, metric );
                        } else if ( max ) {
                            log.info( "getting MAX scores for " + metric );
                            score = goMetric.computeMaxSimilarity( g1, g2, GOProbMap, metric );
                        }
                        scores.add( score );
                    }
                }
            }

            DoubleArrayList dlist = new DoubleArrayList();
            dlist.addAllOf( scores );

            double median = Descriptive.median( dlist );
            log.debug( "Adding pair " + pair + " with score " + median );
            scoreMap.put( pair, median );
        }
        return scoreMap;
    }

    private void summarizeResults( int[][] results ) {
        try {
            Writer w = initHistFile( outFile );

            for ( int i = 0; i < MAX_GO_SIM; i++ ) {
                double[] valsForBin = new double[numberOfRandomRuns];
                double[] freqForBin = new double[numberOfRandomRuns];
                w.write( i + "" );
                for ( int j = 0; j < numberOfRandomRuns; j++ ) {
                    valsForBin[j] = results[j][i];
                    freqForBin[j] = results[j][i] / ( double ) SET_SIZE;
                    w.write( "\t" + results[j][i] );
                }

                DoubleArrayList valL = new DoubleArrayList( valsForBin );
                DoubleArrayList valFL = new DoubleArrayList( freqForBin );

                double mean = Descriptive.mean( valL );
                double meanF = Descriptive.mean( valFL );

                double stdev = 0.0;
                double stdevF = 0.0;

                if ( numberOfRandomRuns > 1 ) {
                    stdev = Math.sqrt( Descriptive.sampleVariance( valL, mean ) );
                    stdevF = Math.sqrt( Descriptive.sampleVariance( valFL, meanF ) );
                }

                w.write( "\t" + mean + "\t" + stdev + "\t" + meanF + "\t" + stdevF + "\n" );
            }
            w.flush();
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }

    }

    /**
     * Write the GO annotations in a tabbed format.
     */
    private void writeGoMap() throws IOException {

        Writer w = new FileWriter( new File( this.termsOutPath ) );
        w.write( "# Go terms for probes used in linkeval\n" );
        w.write( "ProbeId\tProbeName\tGene\tGoTermCount\tGoTerms\n" );
        Set<String> seenProbes = new HashSet<String>();
        for ( CompositeSequence cs : this.probemap.keySet() ) {

            if ( seenProbes.contains( cs.getName() ) ) {
                log.info( "Skipping duplicate probe name " + cs.getName() );
                continue;
            }
            seenProbes.add( cs.getName() );

            w.write( cs.getId() + "\t" + cs.getName() + "\t" );
            Collection<Gene> genes = this.probemap.get( cs );
            Set<String> goTerms = new HashSet<String>();
            Set<String> geneSymbs = new HashSet<String>();
            for ( Gene gene : genes ) {
                if ( geneGoMap.containsKey( gene.getId() ) ) {
                    for ( String go : geneGoMap.get( gene.getId() ) ) {
                        goTerms.add( GeneOntologyServiceImpl.asRegularGoId( go ) );
                    }
                }
                geneSymbs.add( gene.getOfficialSymbol() );
            }

            w.write( StringUtils.join( geneSymbs, "|" ) + "\t" );
            w.write( goTerms.size() + "\t" );
            w.write( StringUtils.join( goTerms, "|" ) + "\n" );
        }

        w.close();
    }

    /**
     * @param scoreMap
     * @param overallWatch
     */
    private void writeGoSimilarityResults( Map<GenePair, Double> scoreMap ) {
        StopWatch overallWatch = new StopWatch();
        overallWatch.start();
        subsetLinks = scoreMap.size();
        try {
            Writer write = initOutputFile( outFile );
            for ( GenePair pair : scoreMap.keySet() ) {
                List<Gene> firstGenes = pair.getFirstGenes();
                List<Gene> secondGenes = pair.getSecondGenes();

                String pairString = pair.toString();
                Double score = scoreMap.get( pair );
                if ( score == null ) {
                    log.warn( "Score is null for " + pair );
                    continue;
                }
                int masterGOTerms = 0;
                int coExpGOTerms = 0;
                Collection<OntologyTerm> overlappingGoTerms = null;
                if ( firstGenes.size() == 1 && secondGenes.size() == 1 ) {
                    Gene mGene = firstGenes.iterator().next();
                    Gene cGene = secondGenes.iterator().next();

                    if ( !geneGoMap.containsKey( mGene.getId() ) )
                        masterGOTerms = 0;
                    else
                        masterGOTerms = ( geneGoMap.get( mGene.getId() ) ).size();

                    if ( !geneGoMap.containsKey( cGene.getId() ) )
                        coExpGOTerms = 0;
                    else
                        coExpGOTerms = ( geneGoMap.get( cGene.getId() ) ).size();

                    overlappingGoTerms = getTermOverlap( mGene, cGene );

                } else {
                    Map<String, List<Gene>> orgGenes1 = organizeDuplicates( firstGenes );
                    Map<String, List<Gene>> orgGenes2 = organizeDuplicates( secondGenes );

                    GenePair uniquePair = new GenePair();
                    List<Set<String>> mergedGoTerms1 = new ArrayList<Set<String>>();
                    List<Set<String>> mergedGoTerms2 = new ArrayList<Set<String>>();
                    for ( String geneName1 : orgGenes1.keySet() ) {
                        List<Gene> tempGenes = orgGenes1.get( geneName1 );
                        uniquePair.addFirstGene( tempGenes.get( 0 ) );
                        Set<String> uniqGoTerms = new HashSet<String>();
                        for ( Gene g1 : tempGenes ) {
                            if ( geneGoMap.containsKey( g1.getId() ) ) {
                                uniqGoTerms.addAll( geneGoMap.get( g1.getId() ) );
                            }
                        }
                        mergedGoTerms1.add( uniqGoTerms );
                    }
                    for ( String geneName2 : orgGenes2.keySet() ) {
                        List<Gene> tempGenes = orgGenes2.get( geneName2 );
                        uniquePair.addSecondGene( tempGenes.get( 0 ) );
                        Set<String> uniqGoTerms = new HashSet<String>();
                        for ( Gene g2 : tempGenes ) {
                            if ( geneGoMap.containsKey( g2.getId() ) ) {
                                uniqGoTerms.addAll( geneGoMap.get( g2.getId() ) );
                            }
                        }
                        mergedGoTerms2.add( uniqGoTerms );
                    }
                    pairString = uniquePair.toString();
                    if ( orgGenes1.size() == 1 && orgGenes2.size() == 1 ) {
                        masterGOTerms = mergedGoTerms1.get( 0 ).size();
                        coExpGOTerms = mergedGoTerms2.get( 0 ).size();
                        overlappingGoTerms = getMergedTermOverlap( mergedGoTerms1.get( 0 ), mergedGoTerms2.get( 0 ) );
                    }
                }
                writeOverlapLine( write, pairString, score, overlappingGoTerms, masterGOTerms, coExpGOTerms );
            }
            write.flush();
            overallWatch.stop();
            log.info( "Compute GoOverlap takes " + overallWatch.getTime() + "ms" );

            // printResults(masterTermCountMap);
        } catch ( IOException ioe ) {
            log.error( "Couldn't write to file: " + ioe );

        }
    }

}
