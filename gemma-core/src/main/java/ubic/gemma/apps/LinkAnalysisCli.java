package ubic.gemma.apps;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;

import cern.colt.list.IntArrayList;

import ubic.basecode.dataStructure.matrix.DoubleMatrix2DNamedFactory;
import ubic.basecode.dataStructure.matrix.DoubleMatrixNamed;
import ubic.basecode.dataStructure.matrix.NamedMatrix;
import ubic.basecode.datafilter.AffymetrixProbeNameFilter;
import ubic.basecode.datafilter.Filter;
import ubic.basecode.datafilter.RowLevelFilter;
import ubic.basecode.datafilter.RowMissingFilter;
import ubic.gemma.analysis.linkAnalysis.LinkAnalysis;
import ubic.gemma.datastructure.matrix.ExpressionDataBooleanMatrix;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.datastructure.matrix.ExpressionDataMatrixService;
import ubic.gemma.model.association.coexpression.Probe2ProbeCoexpression;
import ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionService;
import ubic.gemma.model.common.quantitationtype.GeneralType;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.arrayDesign.TechnologyTypeEnum;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Chromosome;
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
	/***Use the one with the preferred set to TRUE******/
	private QuantitationType getQuantitationType(ExpressionExperiment ee) {
		QuantitationType qtf = null;
		Collection<QuantitationType> eeQT = this.eeService.getQuantitationTypes(ee);
		for (QuantitationType qt : eeQT) {
			if(qt.getIsPreferred()) {
				qtf = qt;
				StandardQuantitationType tmpQT = qt.getType();
				if (tmpQT != StandardQuantitationType.DERIVEDSIGNAL	&& tmpQT != StandardQuantitationType.RATIO) {
					log.info("Preferred Quantitation Type may not be correct." + ee.getShortName() + ":" + tmpQT.toString());
				}
				break;
			}
			/*
			StandardQuantitationType tmpQT = qt.getType();
			if (tmpQT == StandardQuantitationType.DERIVEDSIGNAL
					|| tmpQT == StandardQuantitationType.MEASUREDSIGNAL
					|| tmpQT == StandardQuantitationType.RATIO) {
				qtf = qt;
				break;
			}
			*/
		}
		if(qtf == null){
			log.info("Expression Experiment " + ee.getShortName() + " doesn't have a preferred quantitation type");
		}
		return qtf;
	}
	public NamedMatrix missingValueFilter( NamedMatrix data, ExpressionDataDoubleMatrix eeDoubleMatrix, ExpressionExperiment ee ) {
	        List MTemp = new Vector();
	        List rowNames = new Vector();
	        int numRows = data.rows();
	        int numCols = data.columns();
	        IntArrayList present = new IntArrayList( numRows );
	        int minPresentCount = 0;
	        int kept = 0;

	        if ( minPresentFractionIsSet ) {
	            minPresentCount =  ( int ) Math.ceil( minPresentFraction * numCols ) ;
	        }
	        else 
	        	return data;
	        
			QuantitationType qtf = null;
			Collection<QuantitationType> eeQT = this.eeService.getQuantitationTypes(ee);
			for (QuantitationType qt : eeQT) {
				StandardQuantitationType tmpQT = qt.getType();
				if (tmpQT == StandardQuantitationType.PRESENTABSENT)
					qtf = qt;
			}
			if(qtf == null){
				log.info("Expression Experiment " + ee.getShortName() + " doesn't have a PRESENTABSENT QUANTITATION TYPE");
				return null;
			}
			
			ExpressionDataBooleanMatrix maskMatrix = new ExpressionDataBooleanMatrix(ee, qtf);
			if(maskMatrix == null){
				log.info("Can't get the boolean matrix");
				return null;
			}
	        
	        /* first pass - determine how many missing values there are per row */
	        for ( int i = 0; i < numRows; i++ ) {
	            int missingCount = 0;
	            for ( int j = 0; j < numCols; j++ ) {
	            	BioAssay bioAssay = eeDoubleMatrix.getBioAssayForColumn(((Integer)data.getColName(j)).intValue());
	                if ( !maskMatrix.get((DesignElement)data.getRowName(i), bioAssay).booleanValue()|| Double.isNaN(((DoubleMatrixNamed)data).get( i, j ) )) {
	                    missingCount++;
	                    data.set(i,j,Double.NaN);
	                }
	            }
	            present.add( missingCount );
	            if ( missingCount <= minPresentCount ) {
	                kept++;
	                MTemp.add( data.getRowObj( i ) );
	                rowNames.add(data.getRowName( i ));
	            }
	        }

	        NamedMatrix returnval = DoubleMatrix2DNamedFactory.fastrow( MTemp.size(), numCols );

	        // Finally fill in the return value.
	        for ( int i = 0; i < MTemp.size(); i++ ) {
	            for ( int j = 0; j < numCols; j++ ) {
	                returnval.set( i, j, ( ( Object[] ) MTemp.get( i ) )[j] );
	            }
	        }
	        returnval.setColumnNames( data.getColNames() );
	        returnval.setRowNames( rowNames );

	        log.info( "There are " + kept + " rows after removing rows which have missed more than " + minPresentCount
	                + " values " );

	        return ( returnval );

	    }
    private DoubleMatrixNamed filter(DoubleMatrixNamed dataMatrix, ExpressionDataDoubleMatrix eeDoubleMatrix, ExpressionExperiment ee) {
    	/********Check the array design technology to choose the filter****/
    	DoubleMatrixNamed r = dataMatrix;
    	if(r == null) return r;
        log.info( "Data set has " + r.rows() + " rows and " + r.columns() + " columns." );
    	ArrayDesign arrayDesign = (ArrayDesign)this.eeService.getArrayDesignsUsed(ee).iterator().next();
    	TechnologyType techType = arrayDesign.getTechnologyType();
        
    	if(techType.equals(TechnologyTypeEnum.TWOCOLOR)|| techType.equals(TechnologyType.DUALMODE) ){
    		/***Apply for two color missing value filtered*/
        	if ( minPresentFractionIsSet ) {
        		
        		/*
                log.info( "Filtering out genes that are missing too many values" );
                RowMissingFilter x = new RowMissingFilter();
                x.setMinPresentFraction( minPresentFraction );
                */
                r = ( DoubleMatrixNamed ) missingValueFilter( dataMatrix,eeDoubleMatrix, ee);
            }
    	}
    	
    	if(techType.equals(TechnologyTypeEnum.ONECOLOR)){
        	if ( minPresentFractionIsSet ) {
                log.info( "Filtering out genes that are missing too many values" );
                RowMissingFilter x = new RowMissingFilter();
                x.setMinPresentFraction( minPresentFraction );
                r = (DoubleMatrixNamed)x.filter(r);
            }

    		if ( lowExpressionCutIsSet ) { // todo: make sure this works with ratiometric data. Make sure we don't do this
    			// as well as affy filtering.
    			log.info( "Filtering out genes with low expression for " + ee.getShortName() );
    			RowLevelFilter x = new RowLevelFilter();
    			x.setLowCut( this.lowExpressionCut );
    			x.setHighCut(this.highExpressionCut);
    			x.setRemoveAllNegative( true ); // todo: fix
    			x.setUseAsFraction( true );
    			r = ( DoubleMatrixNamed ) x.filter( r );
    			
    		}
    		if(arrayDesign.getName().toUpperCase().contains("AFFYMETRIX")){
    			log.info( "Filtering by Affymetrix probe name for " + ee.getShortName());
    			Filter x = new AffymetrixProbeNameFilter(new int[] { 2 } );
    			r = ( DoubleMatrixNamed ) x.filter( r );
    		}
    	}
        return r;
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
		log.info("Got links "+ p2plinks.size());
	}
	private String analysis(ExpressionExperiment ee) {
		eeService.thaw(ee);
		QuantitationType qt = this.getQuantitationType(ee);
		if (qt == null) 
			return("No Quantitation Type in " + ee.getShortName());

		log.info("Load Data for  " + ee.getShortName());


		ExpressionDataDoubleMatrix eeDoubleMatrix = (ExpressionDataDoubleMatrix)this.expressionDataMatrixService.getMatrix(ee, qt);
		DoubleMatrixNamed dataMatrix = eeDoubleMatrix.getNamedMatrix();
		dataMatrix = this.filter(dataMatrix,eeDoubleMatrix, ee);
		
		if (dataMatrix == null) 
			return("No data matrix " + ee.getShortName());

		if (dataMatrix.rows() < 100)
			return("Most Probes are filtered out " + ee.getShortName());

		if (dataMatrix.columns() < LinkAnalysisCli.MINIMUM_SAMPLE)
			return("No enough samples " + ee.getShortName());

		this.linkAnalysis.setDataMatrix(dataMatrix);
		Collection<DesignElementDataVector> dataVectors = vectorService.findAllForMatrix(ee, qt);
		if (dataVectors == null) 
			return("No data vector " + ee.getShortName());

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
		
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected Exception doWork(String[] args) {
		// TODO Auto-generated method stub
		Exception err = processCommandLine("Link Analysis Data Loader", args);
		if (err != null) {
			return err;
		}
		this.eeService = (ExpressionExperimentService) this.getBean("expressionExperimentService");

		this.expressionDataMatrixService = (ExpressionDataMatrixService) this.getBean("expressionDataMatrixService");

		this.vectorService = (DesignElementDataVectorService) this.getBean("designElementDataVectorService");

		ExpressionExperiment expressionExperiment = null;
		this.linkAnalysis.setDEService(vectorService);
		this.linkAnalysis.setPPService((Probe2ProbeCoexpressionService) this.getBean("probe2ProbeCoexpressionService"));

		if (this.geneExpressionFile == null) {
			Collection<String> errorObjects = new HashSet<String>();
			Collection<String> persistedObjects = new HashSet<String>();
			if (this.geneExpressionList == null) {
				Collection<ExpressionExperiment> all = eeService.loadAll();
				log.info("Total ExpressionExperiment: " + all.size());
				for (ExpressionExperiment ee : all){
					try {
						String info = this.analysis(ee); 
						if(info == null){
							persistedObjects.add( ee.toString() );
						}else{
							errorObjects.add( ee.getShortName() + " contains errors: " + info );
						}
					} catch (Exception e) {
						errorObjects.add( ee + ": " + e.getMessage() );
						e.printStackTrace();
						log.error( "**** Exception while processing " + ee + ": " + e.getMessage() + " ********" );
					}
				}
			} else {
				try{
					InputStream is = new FileInputStream(this.geneExpressionList);
					BufferedReader br = new BufferedReader(	new InputStreamReader(is));
					String accession = null;
					while ((accession = br.readLine()) != null) {
						if (StringUtils.isBlank(accession))
							continue;
						if(accession.trim().startsWith("GSE"))
							expressionExperiment = eeService.findByShortName(accession);
						else
							expressionExperiment = eeService.findById(new Long(accession.trim()));
						if (expressionExperiment == null) {
							errorObjects.add( accession + " is not loaded yet! " );
							continue;
						}
						try{
							String info = this.analysis(expressionExperiment);
							if(info == null){
								persistedObjects.add( expressionExperiment.toString() );
							}
							else{
								errorObjects.add( expressionExperiment.getShortName() + " contains errors: " + info );
							}
						} catch (Exception e) {
							errorObjects.add( expressionExperiment + ": " + e.getMessage() );
							e.printStackTrace();
							log.error( "**** Exception while processing " + expressionExperiment + ": " + e.getMessage() + " ********" );
						}
					}
				}catch(Exception e){
					return e;
				}
			}
			summarizeProcessing( errorObjects, persistedObjects );
		}else {
			expressionExperiment = eeService.findByShortName(this.geneExpressionFile);
			if (expressionExperiment == null) {
				log.info(this.geneExpressionFile + " is not loaded yet!");
				return null;
			}
			String info = this.analysis(expressionExperiment);
			if(info != null){
				log.info( expressionExperiment + " contains errors: " + info );
			}
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
