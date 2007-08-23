package ubic.gemma.apps;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang.time.StopWatch;

import ubic.basecode.math.distribution.HistogramSampler;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.util.ConfigUtils;
import cern.colt.list.DoubleArrayList;

public class CorrelationHistogramSamplerCLI extends AbstractGeneExpressionExperimentManipulatingCLI {
    private Taxon taxon;
    private int numSamples;
    private String outFileName;
    private int kMax;
    public static final int DEFAULT_NUM_SAMPLES = 1000;

    public static final int DEFAULT_K_MAX = 0;

    @Override
    protected void buildOptions() {
        super.buildOptions();
        Option taxonOption = OptionBuilder.hasArg().isRequired().withArgName( "taxon" ).withDescription(
                "The taxon of the genes" ).withLongOpt( "taxon" ).create( 't' );
        addOption( taxonOption );
        Option numSamplesOption = OptionBuilder.hasArg().withArgName( "Number of samples" ).withDescription(
                "Number of times to sample each correlation histogram" ).withLongOpt( "numSamples" ).withType(
                Integer.class ).create( 'n' );
        addOption( numSamplesOption );

        Option outFileOption = OptionBuilder.hasArg().isRequired().withArgName( "Output file" ).withDescription(
                "File to write samples to" ).withLongOpt( "out" ).create( 'o' );
        addOption( outFileOption );

        Option kMaxOption = OptionBuilder.hasArg().withArgName( "kth largest value" ).withDescription(
                "Select the kth largest sample from the correlation histogram samples" ).withLongOpt( "kMax" ).create(
                'k' );
        addOption( kMaxOption );
    }

    @Override
    protected void processOptions() {
        super.processOptions();
        TaxonService taxonService = ( TaxonService ) getBean( "taxonService" );
        String taxonName = getOptionValue( 't' );
        taxon = Taxon.Factory.newInstance();
        taxon.setCommonName( taxonName );
        taxon = taxonService.find( taxon );

        if ( hasOption( 'n' ) ) {
            numSamples = getIntegerOptionValue( 'n' );
        } else {
            numSamples = DEFAULT_NUM_SAMPLES;
        }
        if ( hasOption( 'k' ) ) {
            kMax = getIntegerOptionValue( 'k' );
        } else {
            kMax = DEFAULT_K_MAX;
        }
        outFileName = getOptionValue( 'o' );

    }

    @Override
    protected Exception doWork( String[] args ) {
        Exception exc = processCommandLine( "CorrelationHistogramSampling", args );
        if ( exc != null ) return exc;
        Collection<ExpressionExperiment> ees;
        try {
            ees = getExpressionExperiments( taxon );
        } catch ( IOException e ) {
            return e;
        }
        Map<ExpressionExperiment, HistogramSampler> ee2SamplerMap = getHistogramSamplerMap( ees );

        log.info( "Sampling " + ee2SamplerMap.keySet().size() + " expression experiments" );
        log.info( "Taking the " + kMax + "th largest value " + numSamples + " times" );
        StopWatch watch = new StopWatch();
        watch.start();
        double[] samples = new double[numSamples];
        for ( int i = 0; i < numSamples; i++ ) {
            DoubleArrayList eeSamples = new DoubleArrayList( ees.size() );
            for ( ExpressionExperiment ee : ee2SamplerMap.keySet() ) {
                HistogramSampler sampler = ee2SamplerMap.get( ee );
                eeSamples.add( sampler.nextSample() );
            }
            eeSamples.sort();
            samples[i] = eeSamples.get( eeSamples.size() - 1 - kMax );
        }
        watch.stop();
        log.info("Finished sampling in " + watch);
        
        String header = new String();
        for (ExpressionExperiment ee : ee2SamplerMap.keySet()) {
            header += "# " + ee.getShortName() + "\n";
        }

        try {
            PrintWriter out = new PrintWriter( new FileWriter( outFileName ) );
            out.print( header);
            for (double d : samples)
                out.println( d );
            out.close();
        } catch ( IOException e ) {
            return e;
        }
        log.info( "Wrote samples to " + outFileName );

        return null;
    }

    /**
     * @param args
     */
    public static void main( String[] args ) {
        CorrelationHistogramSamplerCLI analysis = new CorrelationHistogramSamplerCLI();
        Exception exc = analysis.doWork( args );
        if ( exc != null ) log.error( exc.getMessage() );
    }

    private Map<ExpressionExperiment, HistogramSampler> getHistogramSamplerMap( Collection<ExpressionExperiment> ees ) {
        Map<ExpressionExperiment, HistogramSampler> histSamplers = new HashMap<ExpressionExperiment, HistogramSampler>();
        for ( ExpressionExperiment ee : ees ) {
            String fileName = ConfigUtils.getAnalysisStoragePath() + ee.getShortName() + ".correlDist.txt";
            try {
                HistogramSampler sampler = readHistogramFile( fileName );
                histSamplers.put( ee, sampler );
            } catch ( IOException e ) {
                log.error( e.getMessage() );
                log.error( "ERROR: Unable to read correlation distribution file for " + ee.getShortName() );
            }
        }
        return histSamplers;
    }

    /**
     * Read a correlation distribution
     * 
     * @param fileName
     * @return a histogram sampler for the read distribution
     * @throws IOException
     */
    public HistogramSampler readHistogramFile( String fileName ) throws IOException {
        BufferedReader in = new BufferedReader( new FileReader( fileName ) );
        int numHeaderLines = 1;
        LinkedList<Double> bins = new LinkedList<Double>();
        List<Integer> countList = new LinkedList<Integer>();
        while ( in.ready() ) {
            String line = in.readLine();
            if ( line.startsWith( "#" ) || numHeaderLines-- > 0 ) continue;
            String fields[] = line.split( "\t" );
            Double bin = Double.valueOf( fields[0] );
            bins.add( bin );
            Integer count = Integer.valueOf( fields[1] );
            countList.add( count );
        }

        double min = bins.getFirst().doubleValue();
        double max = bins.getLast().doubleValue();
        int[] counts = new int[countList.size()];
        for ( int i = 0; i < counts.length; i++ ) {
            counts[i] = countList.get( i );
        }
        return new HistogramSampler( counts, min, max );
    }

}
