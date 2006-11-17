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
			this.linkAnalysis.setMinPresentFraction(Double
					.parseDouble(getOptionValue('m')));
		}
		if (hasOption('e')) {
			this.linkAnalysis.setLowExpressionCut(Double
					.parseDouble(getOptionValue('e')));
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
		/*
		 * QuantitationTypeService qts = (QuantitationTypeService)
		 * getBean("quantitationTypeService"); QuantitationType qtf =
		 * QuantitationType.Factory.newInstance(); // Affymetrix platform.
		 * 
		 * qtf.setName("VALUE"); qtf.setScale(ScaleType.UNSCALED);
		 * qtf.setRepresentation(PrimitiveType.DOUBLE);
		 * qtf.setGeneralType(GeneralType.QUANTITATIVE);
		 * qtf.setType(StandardQuantitationType.DERIVEDSIGNAL);
		 * 
		 * qtf.setName( "ABS_CALL" ); qtf.setScale( ScaleType.OTHER );
		 * qtf.setRepresentation( PrimitiveType.STRING ); qtf.setGeneralType(
		 * GeneralType.CATEGORICAL ); qtf.setType(
		 * StandardQuantitationType.PRESENTABSENT);
		 */
		Collection<QuantitationType> eeQT = eeService.getQuantitationTypes(ee);
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
	
	private void test(){
		QuantitationTypeService qts = (QuantitationTypeService)getBean("quantitationTypeService");
		QuantitationType qtf = QuantitationType.Factory.newInstance();
		qtf.setName("VALUE"); 
		qtf.setScale(ScaleType.UNSCALED);
		qtf.setRepresentation(PrimitiveType.DOUBLE);
		qtf.setGeneralType(GeneralType.QUANTITATIVE);
		qtf.setType(StandardQuantitationType.DERIVEDSIGNAL);
		qtf = qts.find(qtf);
		if(qtf == null){
			log.info("NO Quantitation Type!");
			return;
		}
		log.debug("Got Quantitiontype : " + qtf.getId());
		ExpressionExperiment ee = this.eeService.findById(new Long(1));
		Collection<ExpressionExperiment> ees = new HashSet<ExpressionExperiment>();
		ees.add(ee);
		GeneService geneService = (GeneService)getBean("geneService");
		Gene gene = geneService.load(461722);
        Collection<Probe2ProbeCoexpression> p2plinks = null;
        Probe2ProbeCoexpressionService ppService = (Probe2ProbeCoexpressionService) this
		.getBean("probe2ProbeCoexpressionService");
		p2plinks = ppService.findCoexpressionRelationships(gene,ees,qtf);
		log.debug("Got links "+ p2plinks.size());
	}
	private boolean analysis(ExpressionExperiment ee) {
		eeService.thaw(ee);
		QuantitationType qt = this.getQuantitationType(ee);
		if (qt == null) {
			log.info("No Quantitation Type in " + ee.getShortName());
			return false;
		}

		log.info("Load Data for  " + ee.getShortName());
		// this.linkAnalysis.setExpressionExperiment(ees.iterator().next());
		DoubleMatrixNamed dataMatrix = expressionDataMatrixService
				.getDoubleNamedMatrix(ee, qt);
		// DoubleMatrixNamed dataMatrix =
		// ((ExpressionDataDoubleMatrix)expressionDataMatrixService.getMatrix(expressionExperiment,
		// this.getQuantitationType())).getDoubleMatrixNamed();
		if (dataMatrix == null) {
			log.info("No data matrix " + ee.getShortName());
			return false;
		}
		if (dataMatrix.columns() < LinkAnalysisCli.MINIMUM_SAMPLE){
			log.info("No enough samples " + ee.getShortName());
			return false;
		}
		this.linkAnalysis.setDataMatrix(dataMatrix);
		Collection<DesignElementDataVector> dataVectors = vectorService
				.findAllForMatrix(ee, qt);
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
