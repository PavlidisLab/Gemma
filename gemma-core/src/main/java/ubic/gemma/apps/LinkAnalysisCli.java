package ubic.gemma.apps;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashSet;
import java.util.Random;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;

import ubic.basecode.dataStructure.matrix.DoubleMatrixNamed;
import ubic.basecode.datafilter.AffymetrixProbeNameFilter;
import ubic.basecode.datafilter.Filter;
import ubic.basecode.datafilter.RowLevelFilter;
import ubic.basecode.datafilter.RowMissingFilter;
import ubic.gemma.analysis.linkAnalysis.LinkAnalysis;
import ubic.gemma.datastructure.matrix.ExpressionDataMatrixService;
import ubic.gemma.model.association.coexpression.Probe2ProbeCoexpression;
import ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionService;
import ubic.gemma.model.common.quantitationtype.GeneralType;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Gene;
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
	 * Use for batch processing These two files could contain the lists of
	 * experiment;
	 */
	private String geneExpressionList = null;

	private String geneExpressionFile = null;

	private String localHome = "c:";

	private ExpressionExperimentService eeService = null;

	private ExpressionDataMatrixService expressionDataMatrixService = null;

	private DesignElementDataVectorService vectorService = null;

	private LinkAnalysis linkAnalysis = new LinkAnalysis();

	private double tooSmallToKeep = 0.5;
	
	final static int MINIMUM_SAMPLE = 7;
	
    private boolean minPresentFractionIsSet = true;
    private boolean lowExpressionCutIsSet = true;
    private double minPresentFraction = 0.3;
    private double lowExpressionCut = 0.3;
    private double highExpressionCut = 0.0;


	@SuppressWarnings("static-access")
	@Override
	protected void buildOptions() {
		// TODO Add the running options
		Option localHomeOption = OptionBuilder
				.hasArg()
				.withArgName("Local Home Folder")
				.withDescription(
						"The local folder for TestData and TestResult(Should have these two subfolders)")
				.withLongOpt("localHome").create('l');
		addOption(localHomeOption);

		Option geneFileOption = OptionBuilder.hasArg().withArgName(
				"Gene Expression file").withDescription(
				"The Gene Expression File for analysis")
				.withLongOpt("genefile").create('g');
		addOption(geneFileOption);

		Option geneFileListOption = OptionBuilder.hasArg().withArgName(
				"list of Gene Expression file").withDescription(
				"The list file of Gene Expression for analysis").withLongOpt(
				"listfile").create('f');
		addOption(geneFileListOption);

		Option cdfCut = OptionBuilder.hasArg()
				.withArgName("Tolerance Thresold").withDescription(
						"The tolerance threshold for coefficient value")
				.withLongOpt("cdfcut").create('c');
		addOption(cdfCut);

		Option tooSmallToKeep = OptionBuilder.hasArg().withArgName(
				"Cache Threshold").withDescription(
				"The threshold for coefficient cache").withLongOpt("cachecut")
				.create('k');
		addOption(tooSmallToKeep);

		Option fwe = OptionBuilder.hasArg().withArgName(
				"Family Wise Error Ratio").withDescription(
				"The setting for family wise error control").withLongOpt("fwe")
				.create('w');
		addOption(fwe);

		Option binSize = OptionBuilder.hasArg().withArgName("Bin Size")
				.withDescription("The Size of Bin for histogram").withLongOpt(
						"bin").create('b');
		addOption(binSize);

		Option minPresentFraction = OptionBuilder.hasArg().withArgName(
				"Missing Value Threshold").withDescription(
				"The tolerance for accepting the gene with missing values")
				.withLongOpt("missing").create('m');
		addOption(minPresentFraction);

		Option lowExpressionCut = OptionBuilder.hasArg().withArgName(
				"Expression Threshold").withDescription(
				"The tolerance for accepting the expression values")
				.withLongOpt("expression").create('e');
		addOption(lowExpressionCut);

		Option absoluteValue = OptionBuilder.withDescription(
				"If using absolute value in expression file")
				.withLongOpt("abs").create('a');
		addOption(absoluteValue);

		Option useDB = OptionBuilder.withDescription(
				"If Saving the link into database").withLongOpt("usedb")
				.create('d');
		addOption(useDB);
	}

	@Override
	protected void processOptions() {
		super.processOptions();
		if (hasOption('l')) {
			this.localHome = getOptionValue('l');
			this.linkAnalysis.setHomeDir(this.localHome);
		}
		if (hasOption('g')) {
			this.geneExpressionFile = getOptionValue('g');
		}
		if (hasOption('f')) {
			this.geneExpressionList = getOptionValue('f');
		}
		if (hasOption('c')) {
			this.linkAnalysis
					.setCdfCut(Double.parseDouble(getOptionValue('c')));
		}
		if (hasOption('k')) {
			this.tooSmallToKeep = Double.parseDouble(getOptionValue('k'));
			this.linkAnalysis.setTooSmallToKeep(this.tooSmallToKeep);
		}
		if (hasOption('w')) {
			this.linkAnalysis.setFwe(Double.parseDouble(getOptionValue('w')));
		}
		if (hasOption('b')) {
			this.linkAnalysis.setBinSize(Double
					.parseDouble(getOptionValue('b')));
		}

		if (hasOption('m')) {
			this.minPresentFractionIsSet = true;
			this.minPresentFraction = Double.parseDouble(getOptionValue('m'));
		}
		if (hasOption('e')) {
			this.lowExpressionCutIsSet = true;
			this.lowExpressionCut = Double.parseDouble(getOptionValue('e'));
		}

		if (hasOption('a')) {
			this.linkAnalysis.setAbsoluteValue();
		}
		if (hasOption('d')) {
			this.linkAnalysis.setUseDB();
		}
	}

	private QuantitationType getQuantitationType(ExpressionExperiment ee) {
		QuantitationType qtf = null;
		Collection<QuantitationType> eeQT = this.eeService.getQuantitationTypes(ee);
		for (QuantitationType qt : eeQT) {
			System.err.print( qt.getId() + " ");
		}
		for (QuantitationType qt : eeQT) {
			StandardQuantitationType tmpQT = qt.getType();
			if (tmpQT == StandardQuantitationType.DERIVEDSIGNAL
					|| tmpQT == StandardQuantitationType.MEASUREDSIGNAL
					|| tmpQT == StandardQuantitationType.RATIO) {
				qtf = qt;
				break;
			}
		}
		return qtf;
	}
    private DoubleMatrixNamed filter(DoubleMatrixNamed dataMatrix, ExpressionExperiment ee) {
 
    	DoubleMatrixNamed r = dataMatrix;
    	//ArrayDesignService adService = this.getBean("arrayDesignService");
    	if(r == null) return r;
    	
    	//if(this.eeService.)
        log.info( "Data set has " + r.rows() + " rows and " + r.columns() + " columns." );

        if ( minPresentFractionIsSet ) {

            log.info( "Filtering out genes that are missing too many values" );
            RowMissingFilter x = new RowMissingFilter();
            x.setMinPresentFraction( minPresentFraction );
            r = ( DoubleMatrixNamed ) x.filter( r );
        }

        if ( lowExpressionCutIsSet ) { // todo: make sure this works with ratiometric data. Make sure we don't do this
            // as well as affy filtering.
            log.info( "Filtering out genes with low expression" );
            RowLevelFilter x = new RowLevelFilter();
            x.setLowCut( this.lowExpressionCut );
            x.setHighCut(this.highExpressionCut);
            x.setRemoveAllNegative( true ); // todo: fix
            x.setUseAsFraction( true );
            r = ( DoubleMatrixNamed ) x.filter( r );
        }

        log.info( "Filtering by Affymetrix probe name" );
        Filter x = new AffymetrixProbeNameFilter(new int[] { 2 } );
        r = ( DoubleMatrixNamed ) x.filter( r );
        return r;
    }

	private boolean analysis(ExpressionExperiment ee) {
		System.err.println("");
		System.err.print(ee.getShortName() + " ");
		QuantitationType qt1 = this.getQuantitationType(ee);
		if(true)return true;
		
		eeService.thaw(ee);
		QuantitationType qt = this.getQuantitationType(ee);
		if (qt == null) {
			log.info("No Quantitation Type in " + ee.getShortName());
			return false;
		}

		log.info("Load Data for  " + ee.getShortName());

		DoubleMatrixNamed dataMatrix = this.expressionDataMatrixService.getDoubleNamedMatrix(ee, qt);
		dataMatrix = this.filter(dataMatrix, ee);
			
		if (dataMatrix == null) {
			log.info("No data matrix " + ee.getShortName());
			return false;
		}
		if (dataMatrix.columns() < LinkAnalysisCli.MINIMUM_SAMPLE){
			log.info("No enough samples " + ee.getShortName());
			return false;
		}
		this.linkAnalysis.setDataMatrix(dataMatrix);
		Collection<DesignElementDataVector> dataVectors = vectorService.findAllForMatrix(ee, qt);
		if (dataVectors == null) {
			log.info("No data vector " + ee.getShortName());
			return false;
		}
		this.linkAnalysis.setDataVector(dataVectors);
		this.linkAnalysis.setTaxon(eeService.getTaxon(ee.getId()));

		log.info("Starting generating Raw Links for " + ee.getShortName());
		/*this value will be optimized depending on the size of experiment in the analysis.
		 * So it need to be set as the given value before the analysis.
		 * Otherwise, the value in the previous experiment will be in effect for the current experiment.
		 */
		this.linkAnalysis.setTooSmallToKeep(this.tooSmallToKeep); 

		if (this.linkAnalysis.analysis() == true) {
			log.info("Successful Generating Raw Links for "	+ ee.getShortName());
		}
		
		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected Exception doWork(String[] args) {
		// TODO Auto-generated method stub
		Exception err = processCommandLine("Link Analysis Data Loader", args);
		if (err != null) {
			return err;
		}
		try {
			this.eeService = (ExpressionExperimentService) this
					.getBean("expressionExperimentService");

			this.expressionDataMatrixService = (ExpressionDataMatrixService) this
					.getBean("expressionDataMatrixService");

			this.vectorService = (DesignElementDataVectorService) this
					.getBean("designElementDataVectorService");

//			this.test();
//			if(true) return null;
			ExpressionExperiment expressionExperiment = null;
			this.linkAnalysis.setDEService(vectorService);
			this.linkAnalysis.setPPService((Probe2ProbeCoexpressionService) this.getBean("probe2ProbeCoexpressionService"));

			if (this.geneExpressionFile == null) {
				if (this.geneExpressionList == null) {
					Collection<ExpressionExperiment> all = eeService.loadAll();
					log.info("Total ExpressionExperiment: " + all.size());
					for (ExpressionExperiment ee : all)
						this.analysis(ee);
				} else {
					InputStream is = new FileInputStream(
							this.geneExpressionList);
					BufferedReader br = new BufferedReader(
							new InputStreamReader(is));
					String accession = null;
					while ((accession = br.readLine()) != null) {
						if (StringUtils.isBlank(accession))
							continue;
						expressionExperiment = eeService.findByShortName(accession);
						if (expressionExperiment == null) {
							log.info(accession+ " is not loaded yet!");
							continue;
						}

						this.analysis(expressionExperiment);
					}
				}
			} else {
				expressionExperiment = eeService.findByShortName(this.geneExpressionFile);
				if (expressionExperiment == null) {
					log.info(this.geneExpressionFile + " is not loaded yet!");
					return null;
				}
				this.analysis(expressionExperiment);
			}
		} catch (Exception e) {
			log.error(e);
			return e;
		}
		return null;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		LinkAnalysisCli analysis = new LinkAnalysisCli();
		StopWatch watch = new StopWatch();
		watch.start();
		try {
			Exception ex = analysis.doWork(args);
			if (ex != null) {
				ex.printStackTrace();
			}
			watch.stop();
			log.info(watch.getTime());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
