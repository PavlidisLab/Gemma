package ubic.gemma.apps;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang.time.StopWatch;

import ubic.gemma.analysis.linkAnalysis.EffectSizeService;
import ubic.gemma.analysis.linkAnalysis.EffectSizeService.CoexpressionMatrices;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.ontology.GeneOntologyService;

/**
 * Calculate the effect size
 * 
 * @author xwan
 * @author raymond
 */
public class EffectSizeCalculationCli extends AbstractGeneManipulatingCLI {
    private String[] geneSymbols;
    private String queryGeneFile;
    private String targetGeneListFile;
    private String goTerm;

    private String outFilePrefix;

    private Taxon taxon;

    private EffectSizeService effectSizeService;

    private ExpressionExperimentService eeService;

    private GeneService geneService;
    private GeneOntologyService goService;

    private int stringency = 3;

    public static final int DEFAULT_STRINGENCY = 3;

    public EffectSizeCalculationCli() {
        super();
    }

    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {
        Option goOption = OptionBuilder.hasArg().withArgName( "GOTerm" ).withDescription( "Target GO term" )
                .withLongOpt( "GOTerm" ).create( 'g' );
        addOption( goOption );
        Option geneOption = OptionBuilder.hasArgs().withArgName( "queryGene" ).withDescription(
                "Query gene(s) (official symbol)" ).withLongOpt( "queryGene" ).create( 's' );
        addOption( geneOption );
        Option queryGeneFileOption = OptionBuilder.hasArgs().withArgName( "queryGeneFile" ).withDescription(
                "Query gene file" ).withLongOpt( "queryGeneFile" ).create( 'q' );
        addOption( queryGeneFileOption );
        Option targetGeneFileOption = OptionBuilder.hasArg().withArgName( "targetGeneFile" ).withDescription(
                "File containing list of target genes" ).withLongOpt( "targetGeneFile" ).create( 'f' );
        addOption( targetGeneFileOption );
        Option taxonOption = OptionBuilder.hasArg().isRequired().withArgName( "Taxon" ).withDescription(
                "the taxon of the genes" ).withLongOpt( "Taxon" ).create( 't' );
        addOption( taxonOption );
        Option outputFileOption = OptionBuilder.hasArg().isRequired().withArgName( "outFilePrefix" ).withDescription(
                "File prefix for saving the correlation data" ).withLongOpt( "outFilePrefix" ).create( 'o' );
        addOption( outputFileOption );
        Option stringencyOption = OptionBuilder.hasArg().withArgName( "stringency" ).withDescription(
                "Vote count stringency for link selection" ).withLongOpt( "stringency" ).create( 'r' );
        addOption( stringencyOption );
    }

    @Override
    protected void processOptions() {
        super.processOptions();
        if ( hasOption( 'g' ) ) {
            this.goTerm = getOptionValue( 'g' );
        }
        if ( hasOption( 's' ) ) {
            this.geneSymbols = getOptionValues( 's' );
        }
        if ( hasOption( 'q' ) ) {
            this.queryGeneFile = getOptionValue( 'q' );
        }
        if ( hasOption( 'f' ) ) {
            this.targetGeneListFile = getOptionValue( 'f' );
        }
        if ( hasOption( 'g' ) ) {
            this.goTerm = getOptionValue( 'g' );
        }
        String taxonName = getOptionValue( 't' );
        taxon = Taxon.Factory.newInstance();
        taxon.setCommonName( taxonName );
        TaxonService taxonService = ( TaxonService ) this.getBean( "taxonService" );
        taxon = taxonService.find( taxon );
        if ( taxon == null ) {
            log.info( "No Taxon found!" );
        }
        if ( hasOption( 'o' ) ) {
            this.outFilePrefix = getOptionValue( 'o' );
        }
        if ( hasOption( 'r' ) ) {
            this.stringency = Integer.parseInt( getOptionValue( 'r' ) );
        } else {
            this.stringency = DEFAULT_STRINGENCY;
        }

        initBeans();
    }

    protected void initBeans() {
        effectSizeService = ( EffectSizeService ) this.getBean( "effectSizeService" );
        eeService = ( ExpressionExperimentService ) this.getBean( "expressionExperimentService" );
        geneService = ( GeneService ) this.getBean( "geneService" );
    }

    private Collection<Long> getGoTermIds( String goTerm ) {
        Collection<Gene> genes = goService.getGenes( goTerm, taxon );
        Collection<Long> geneIds = new HashSet<Long>();
        for ( Gene gene : genes ) {
            geneIds.add( gene.getId() );
        }
        return geneIds;
    }

    @Override
    protected Exception doWork( String[] args ) {
        Exception exc = processCommandLine( "EffectSizeCalculation ", args );
        if ( exc != null ) {
            return exc;
        }
        StopWatch watch = new StopWatch();
        watch.start();

        Collection<ExpressionExperiment> EEs = eeService.findByTaxon( taxon );
        if ( EEs == null ) {
            return new Exception( "Could not find expression experiments for taxon" );
        }

        List<Long> queryGeneIds = new ArrayList<Long>();
        List<Long> targetGeneIds = new ArrayList<Long>();
        if ( geneSymbols != null ) {
            for ( String symbol : geneSymbols ) {
                for ( Gene gene : ( Collection<Gene> ) geneService.findByOfficialSymbol( symbol ) ) {
                    if ( gene.getTaxon().equals( taxon ) ) queryGeneIds.add( gene.getId() );
                }
            }
        }
        if ( queryGeneFile != null ) {
            try {
                queryGeneIds.addAll( readGeneListFileToIds( queryGeneFile, taxon ) );
            } catch ( IOException e ) {
                return e;
            }
        }

        if ( targetGeneListFile != null ) {
            try {
                targetGeneIds.addAll( readGeneListFileToIds( targetGeneListFile, taxon ) );
            } catch ( IOException e ) {
                return e;
            }
        }
        if ( goTerm != null ) {
            targetGeneIds.addAll( getGoTermIds( goTerm ) );
        }

        if ( targetGeneIds.size() == 0 || queryGeneIds.size() == 0 ) {
            return new Exception( "No genes in query/target" );
        }

        CoexpressionMatrices matrices = effectSizeService.calculateCoexpressionMatrices( EEs, queryGeneIds,
                targetGeneIds );

        try {
            effectSizeService.saveToFile( outFilePrefix + ".corr.txt", matrices.getCorrelationMatrix(), true );
            effectSizeService.saveToFile( outFilePrefix + ".expr_lvl.txt", matrices.getExprLvlMatrix(), true );
            effectSizeService.saveToFigure( outFilePrefix + ".corr.png", matrices.getCorrelationMatrix() );
        } catch ( IOException e ) {
            return e;
        }

        return null;
    }

    public static void main( String[] args ) {
        EffectSizeCalculationCli analysis = new EffectSizeCalculationCli();
        StopWatch watch = new StopWatch();
        watch.start();
        log.info( "Starting Effect Size Analysis" );
        Exception exc = analysis.doWork( args );
        if ( exc != null ) {
            log.error( exc.getMessage() );
        }
        log.info( "Finished analysis in " + watch.getTime() / 1000 + " seconds" );
    }
}