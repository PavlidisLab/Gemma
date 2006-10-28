package ubic.gemma.apps;

import java.util.Collection;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang.time.StopWatch;

import ubic.basecode.dataStructure.matrix.DoubleMatrixNamed;
import ubic.gemma.analysis.linkAnalysis.LinkAnalysis;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.datastructure.matrix.ExpressionDataMatrixService;
import ubic.gemma.loader.expression.geo.GeoDomainObjectGenerator;
import ubic.gemma.loader.expression.geo.GeoDomainObjectGeneratorLocal;
import ubic.gemma.loader.expression.geo.service.GeoDatasetService;
import ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionService;
import ubic.gemma.model.common.quantitationtype.GeneralType;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.util.AbstractSpringAwareCLI;

/**
 * offline tools to conduct the link analysis
 * 
 * @author xiangwan
 * @version $Id$
 */
public class LinkAnalysisCli extends AbstractSpringAwareCLI {

    /**
     * Use for batch processing These two files could contain the lists of experiment;
     */
    private String geneExpressionFile = null;
    private String localHome = "c:";;
    private LinkAnalysis linkAnalysis = new LinkAnalysis();

    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {
        // TODO Add the running options
        Option localHomeOption = OptionBuilder.hasArg().isRequired().withArgName( "Local Home Folder" )
        .withDescription( "The local folder for TestData and TestResult(Should have these two subfolders)" ).withLongOpt( "localHome" ).create( 'l' );
        addOption( localHomeOption );

        Option geneFileOption = OptionBuilder.hasArg().isRequired().withArgName( "Gene Expression file" )
                .withDescription( "The Gene Expression File for analysis" ).withLongOpt( "genefile" ).create( 'f' );
        addOption( geneFileOption );

        Option cdfCut = OptionBuilder.hasArg().withArgName( "Tolerance Thresold" ).withDescription(
                "The tolerance threshold for coefficient value" ).withLongOpt( "cdfcut" ).create( 'c' );
        addOption( cdfCut );

        Option tooSmallToKeep = OptionBuilder.hasArg().withArgName( "Cache Threshold" ).withDescription(
                "The threshold for coefficient cache" ).withLongOpt( "cachecut" ).create( 'k' );
        addOption( tooSmallToKeep );

        Option fwe = OptionBuilder.hasArg().withArgName( "Family Wise Error Ratio" ).withDescription(
                "The setting for family wise error control" ).withLongOpt( "fwe" ).create( 'w' );
        addOption( fwe );

        Option binSize = OptionBuilder.hasArg().withArgName( "Bin Size" ).withDescription(
                "The Size of Bin for histogram" ).withLongOpt( "bin" ).create( 'b' );
        addOption( binSize );

        Option minPresentFraction = OptionBuilder.hasArg().withArgName( "Missing Value Threshold" ).withDescription(
                "The tolerance for accepting the gene with missing values" ).withLongOpt( "missing" ).create( 'm' );
        addOption( minPresentFraction );

        Option lowExpressionCut = OptionBuilder.hasArg().withArgName( "Expression Threshold" ).withDescription(
                "The tolerance for accepting the expression values" ).withLongOpt( "expression" ).create( 'e' );
        addOption( lowExpressionCut );

        Option absoluteValue = OptionBuilder.withDescription( "If using absolute value in expression file" )
                .withLongOpt( "abs" ).create( 'a' );
        addOption( absoluteValue );

        Option useDB = OptionBuilder.withDescription( "If Saving the link into database" ).withLongOpt( "usedb" )
                .create( 'd' );
        addOption( useDB );
    }

    @Override
    protected void processOptions() {
        super.processOptions();
        if ( hasOption( 'l' ) ) {
            this.localHome = getOptionValue( 'l' );
            this.linkAnalysis.setHomeDir(this.localHome);
        }
        if ( hasOption( 'f' ) ) {
            this.geneExpressionFile = getOptionValue( 'f' );
            this.linkAnalysis.setGeneExpressionFile( this.geneExpressionFile );
        }
        if ( hasOption( 'c' ) ) {
            this.linkAnalysis.setCdfCut( Double.parseDouble( getOptionValue( 'c' ) ) );
        }
        if ( hasOption( 'k' ) ) {
            this.linkAnalysis.setTooSmallToKeep( Double.parseDouble( getOptionValue( 'k' ) ) );
        }
        if ( hasOption( 'w' ) ) {
            this.linkAnalysis.setFwe( Double.parseDouble( getOptionValue( 'w' ) ) );
        }
        if ( hasOption( 'b' ) ) {
            this.linkAnalysis.setBinSize( Double.parseDouble( getOptionValue( 'b' ) ) );
        }
        if ( hasOption( 'm' ) ) {
            this.linkAnalysis.setMinPresentFraction( Double.parseDouble( getOptionValue( 'm' ) ) );
        }
        if ( hasOption( 'e' ) ) {
            this.linkAnalysis.setLowExpressionCut( Double.parseDouble( getOptionValue( 'e' ) ) );
        }
        if ( hasOption( 'a' ) ) {
            this.linkAnalysis.setAbsoluteValue();
        }
        if ( hasOption( 'd' ) ) {
            this.linkAnalysis.setUseDB();
        }
    }

    private QuantitationType getQuantitationType() {
        QuantitationTypeService qts = ( QuantitationTypeService ) getBean( "quantitationTypeService" );
        QuantitationType qtf = QuantitationType.Factory.newInstance();

        // Affymetrix platform.
        qtf.setName( "VALUE" );
        qtf.setScale( ScaleType.UNSCALED );
        qtf.setRepresentation( PrimitiveType.DOUBLE );
        qtf.setGeneralType( GeneralType.QUANTITATIVE );
        qtf.setType( StandardQuantitationType.MEASUREDSIGNAL );
        return qts.find( qtf );
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Exception doWork( String[] args ) {
        // TODO Auto-generated method stub
        Exception err = processCommandLine( "Link Analysis Data Loader", args );
        if ( err != null ) {
            return err;
        }
        this.linkAnalysis.outputOptions();
        try {
            ExpressionExperimentService eeService = ( ExpressionExperimentService ) this
            .getBean( "expressionExperimentService" );
            ExpressionExperiment expressionExperiment = eeService.findByShortName(this.geneExpressionFile);
            if(expressionExperiment == null)
            {
            	GeoDatasetService geoService = ( GeoDatasetService ) this.getBean( "geoDatasetService" );
            	//geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGenerator() );
            	geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal( this.localHome+"/TestData/" ) );
            	geoService.setLoadPlatformOnly( false );
            	Collection<ExpressionExperiment> ees = geoService.fetchAndLoad( this.geneExpressionFile );
            	expressionExperiment = ees.iterator().next();
            }

            // this.linkAnalysis.setExpressionExperiment(ees.iterator().next());
            ExpressionDataMatrixService expressionDataMatrixService = ( ExpressionDataMatrixService ) this
                    .getBean( "expressionDataMatrixService" );
            DoubleMatrixNamed dataMatrix = expressionDataMatrixService.getDoubleNamedMatrix( expressionExperiment, this.getQuantitationType() );
            //DoubleMatrixNamed dataMatrix = ((ExpressionDataDoubleMatrix)expressionDataMatrixService.getMatrix(expressionExperiment, this.getQuantitationType())).getDoubleMatrixNamed();
            this.linkAnalysis.setDataMatrix( dataMatrix );

            DesignElementDataVectorService vectorService = ( DesignElementDataVectorService ) this
                    .getBean( "designElementDataVectorService" );
            this.linkAnalysis.setDEService(vectorService);
            Collection<DesignElementDataVector> dataVectors = vectorService.findAllForMatrix( expressionExperiment,
                    this.getQuantitationType() );
            this.linkAnalysis.setDataVector( dataVectors );
            
            this.linkAnalysis.setPPService( ( Probe2ProbeCoexpressionService ) this
                    .getBean( "probe2ProbeCoexpressionService" ) );
            this.linkAnalysis.setTaxon( eeService.getTaxon( expressionExperiment.getId() ) );

            this.linkAnalysis.analysis();
        } catch ( Exception e ) {
            log.error( e );
            return e;
        }
        return null;
    }

    /**
     * @param args
     */
    public static void main( String[] args ) {
        // TODO Auto-generated method stub
        LinkAnalysisCli analysis = new LinkAnalysisCli();
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            Exception ex = analysis.doWork( args );
            if ( ex != null ) {
                ex.printStackTrace();
            }
            watch.stop();
            log.info( watch.getTime() );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

}
