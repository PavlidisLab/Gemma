package ubic.gemma.apps;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;

import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix2DNamed;
import ubic.basecode.dataStructure.matrix.DoubleMatrixNamed;
import ubic.basecode.dataStructure.matrix.StringMatrix2DNamed;
import ubic.basecode.gui.ColorMap;
import ubic.basecode.gui.ColorMatrix;
import ubic.basecode.gui.JMatrixDisplay;
import ubic.basecode.io.reader.StringMatrixReader;
import ubic.gemma.analysis.coexpression.GeneCoExpressionAnalysis;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.util.AbstractSpringAwareCLI;

/**
 * @author xwan
 *
 */
public class CoExpressionAnalysisCli extends AbstractSpringAwareCLI {

	private String geneList = null;
	private String taxonName = null;
	private String outputFile = null;
	private static String DIVIDOR = "-----";
	@Override
	protected void buildOptions() {
		// TODO Auto-generated method stub
        Option geneFileOption = OptionBuilder.hasArg().isRequired().withArgName( "geneFile" ).withDescription(
        "Short names of the genes to analyze" )
        .withLongOpt( "geneFile" ).create( 'g' );
        addOption( geneFileOption );
        Option taxonOption = OptionBuilder.hasArg().isRequired().withArgName( "Taxon" ).withDescription(
        "the taxon of the genes to analyze" )
        .withLongOpt( "Taxon" ).create( 't' );
        addOption( taxonOption );
        Option outputFileOption = OptionBuilder.hasArg().isRequired().withArgName( "outFile" ).withDescription(
        "File for saving the corelation data" )
        .withLongOpt( "outFile" ).create( 'o' );
        addOption( outputFileOption );

	}
	protected void processOptions() {
		super.processOptions();
        if ( hasOption( 'g' ) ) {
            this.geneList = getOptionValue( 'g' );
        }
        if ( hasOption( 't' ) ) {
            this.taxonName = getOptionValue( 't' );
        }
        if ( hasOption( 'o' ) ) {
            this.outputFile = getOptionValue( 'o' );
        }
	}
	
	private Collection <Gene> getTestGenes(GeneService geneService, Taxon taxon){
		String geneNames[] = {"RPL8", "BC071678", "c1orf151", "RPS18", "PCOLCE2",
					"RPS14", "BC072682", "Ak130913"};
		
		//String geneNames[] = {"RPS18", "BC071678"};
		
		return this.getGenes(geneService, geneNames, taxon);
	}
	private Collection <Gene> getGenes(GeneService geneService, Object[] geneNames, Taxon taxon){
		HashSet<Gene> genes = new HashSet();
		for(int i = 0; i < geneNames.length; i++){
			Gene gene = getGene(geneService, (String)geneNames[i], taxon);
			if(gene != null)
				genes.add(gene);
		}
		return genes;
	}
	
	private Gene getGene(GeneService geneService, String geneName, Taxon taxon){
		Gene gene = Gene.Factory.newInstance();
		gene.setOfficialSymbol(geneName.trim());
		gene.setTaxon(taxon);
		gene = geneService.find(gene);
		if(gene == null){
			log.info("Can't Load gene "+ geneName);
		}
		return gene;
	}
	private Taxon getTaxon(String name){
		Taxon taxon = Taxon.Factory.newInstance();
		taxon.setCommonName(name);
		TaxonService taxonService = (TaxonService)this.getBean("taxonService");
		taxon = taxonService.find(taxon);
		if(taxon == null){
			log.info("NO Taxon found!");
		}
		return taxon;
	}
	@Override
	protected Exception doWork(String[] args) {
		// TODO Auto-generated method stub
		Exception err = processCommandLine("CoExpression Analysis", args);
		if (err != null) {
			return err;
		}
		DesignElementDataVectorService devService = (DesignElementDataVectorService) this.getBean("designElementDataVectorService");
		ExpressionExperimentService eeService = (ExpressionExperimentService) this.getBean("expressionExperimentService");
		GeneService geneService = (GeneService) this.getBean("geneService");
		Taxon taxon = getTaxon(this.taxonName);
		if(taxon == null){
			log.info("No taxon is found " + this.taxonName);
			return null;
		}
		Collection <ExpressionExperiment> allEE = eeService.getByTaxon(taxon);
		Collection <Gene> testGenes = getTestGenes(geneService,taxon);
		HashSet<String> geneNames = new HashSet<String>();
		HashSet<String> targetGeneNames = new HashSet<String>();
		boolean targetGene = true;
		try{
			InputStream is = new FileInputStream( this.geneList );
			BufferedReader br = new BufferedReader( new InputStreamReader( is ) );
			String shortName = null;
			while ( ( shortName = br.readLine() ) != null ) {
				if ( StringUtils.isBlank( shortName ) ) continue;
				if(shortName.trim().contains(DIVIDOR)){
					targetGene = false;
					continue;
				}
				if(targetGene) targetGeneNames.add(shortName.trim());
				else geneNames.add(shortName.trim());
			}
		}catch(Exception e){
			e.printStackTrace();
			return e;
		}
        Collection<Gene> genes = this.getGenes(geneService, geneNames.toArray(), taxon);
        Collection<Gene> targetGenes = this.getGenes(geneService, targetGeneNames.toArray(), taxon);
//        HashMap<QuantitationType, HashSet<ExpressionExperiment>> eeMap = new HashMap<QuantitationType, HashSet<ExpressionExperiment>>();
//        for(ExpressionExperiment ee:allEE){
//        	Collection <QuantitationType> eeQT = eeService.getQuantitationTypes(ee);
//        	for (QuantitationType qt : eeQT) {
//        		if(qt.getIsPreferred()){
//        			HashSet<ExpressionExperiment> eeCollection =  eeMap.get( qt );
//        			if(eeCollection == null){
//        				log.info(" Get Quantitation Type : " + qt.getName()+ ":"+qt.getType());
//        				eeCollection = new HashSet<ExpressionExperiment>();
//        				eeMap.put( qt, eeCollection );
//        			}
//        			eeCollection.add( ee );
//        			break;
//        		}
//        	}
//        }
//        Collection<DesignElementDataVector> allDevs = new HashSet<DesignElementDataVector>();
//       	for(QuantitationType qt:eeMap.keySet()){
//      			HashSet<ExpressionExperiment> expressionExperimentService = eeMap.get(qt);
//      			System.err.println(expressionExperimentService.size() + " " + genes.size());
//       			for(ExpressionExperiment ee:expressionExperimentService){
//       				HashSet<ExpressionExperiment> tmpEEs = new HashSet<ExpressionExperiment>();
//       				System.err.println(ee.getShortName() + " " + qt);
//       				tmpEEs.add(ee);
//       	       		try{
//       	       			Collection<DesignElementDataVector> devs = vectorService.getGeneCoexpressionPattern(tmpEEs, genes, qt);
//       	       			System.err.println(devs.size());
//       	       			vectorService.thaw(devs);
//       	       			for(DesignElementDataVector dev:devs){
//       	       				System.err.print(dev.getId() + "( " + dev.getQuantitationType().getId() + ") ");
//       	       			}
//       	       			System.err.println("");
//       	       			allDevs.addAll(devs);
//       	       		}catch(Exception e){
//       	       			e.printStackTrace();
//       	       		}
//       			}
//      			try{
//      				Collection<DesignElementDataVector> devs = vectorService.getGeneCoexpressionPattern(expressionExperimentService, genes, qt);
//      				System.err.println(devs.size());
//      				allDevs.addAll(devs);
//      			}catch(Exception e){
//	       			e.printStackTrace();
//   	       		}
//       	}
        HashSet<Gene> queryGenes = new HashSet<Gene>();
        queryGenes.addAll(genes);
        queryGenes.addAll(targetGenes);
        System.err.println("Start the Query for "+ queryGenes.size() + " genes");
        StopWatch qWatch = new StopWatch();
        qWatch.start();

        Map<DesignElementDataVector, Collection<Gene>> geneMap = new HashMap<DesignElementDataVector, Collection<Gene>>(devService.getGeneCoexpressionPattern(allEE, queryGenes));
        qWatch.stop();
        System.err.println("Query takes " + qWatch.getTime());

        GeneCoExpressionAnalysis coExperssion = new GeneCoExpressionAnalysis((Set)targetGenes, (Set)genes, (Set)new HashSet(allEE));
        
       	coExperssion.setDevToGenes(geneMap);
       	coExperssion.setExpressionExperimentService(eeService);
       	System.err.println(geneMap.size());
       	coExperssion.analysis((Set)geneMap.keySet());
       	try{
       		//Generate the data file for Cluster3
       		PrintStream output = new PrintStream(new FileOutputStream(new File(this.outputFile)));
       		double presencePercent = 0.5;
       		coExperssion.output(output, presencePercent);
       		output.close();
       		
       		//Running Cluster3 to geneate .cdt file
       		Runtime rt = Runtime.getRuntime();
       		Process clearOldFiles = rt.exec("rm *.cdt -f");
       		clearOldFiles.waitFor();

       		String clusterCmd = "cluster";
       		String commonOptions = "-g 7 -e 7 -m c";
			Process cluster = rt.exec(clusterCmd + " -f " + this.outputFile + " " + commonOptions);
			cluster.waitFor();
			
       		//Read the generated file into a String Matrix
			StringMatrixReader mReader = new StringMatrixReader();
			int dotIndex = this.outputFile.lastIndexOf('.');
			String CDTMatrixFile = this.outputFile.substring(0, dotIndex);
			StringMatrix2DNamed cdtMatrix = (StringMatrix2DNamed)mReader.read(CDTMatrixFile+".cdt");

			//Read String Matrix and convert into DenseDoubleMatrix
			int extra_rows = 2, extra_cols = 3;
			double[][] data = new double[cdtMatrix.rows() - extra_rows][];
	        List<String> rowLabels = new ArrayList<String>();
	        List<String> colLabels = new ArrayList<String>();
	        
	        List colNames = cdtMatrix.getColNames();
	        for(int i = extra_cols; i < colNames.size(); i++)
	        	colLabels.add((String)colNames.get(i));
	        
	        int rowIndex = 0;
            for(int i = extra_rows; i < cdtMatrix.rows(); i++){
            	Object row[] = cdtMatrix.getRow(i);
            	rowLabels.add((String)row[0]);
            	data[rowIndex] = new double[row.length - extra_cols];
            	for(int j = extra_cols; j < row.length; j++)
            		try{
            			data[rowIndex][j-extra_cols] = Double.valueOf((String)row[j]);
            		}catch(Exception e){
            			data[rowIndex][j-extra_cols] = Double.NaN;
            			continue;
            		}
            	rowIndex++;
            }
       		DoubleMatrixNamed dataMatrix = new DenseDoubleMatrix2DNamed( data );
            dataMatrix.setRowNames( rowLabels );
            dataMatrix.setColumnNames( colLabels );

       		// Get the rank Matrix
       		DoubleMatrixNamed rankMatrix = coExperssion.getRankMatrix(dataMatrix);
       		
       		// generate the png figures
       		ColorMatrix dataColorMatrix = new ColorMatrix(dataMatrix);
       		dataColorMatrix.setColorMap(ColorMap.GREENRED_COLORMAP);
       		ColorMatrix rankColorMatrix = new ColorMatrix(rankMatrix);
       		rankColorMatrix.setColorMap(ColorMap.GREENRED_COLORMAP);
       		
       		JMatrixDisplay display1 = new JMatrixDisplay( dataColorMatrix );
       		JMatrixDisplay display2 = new JMatrixDisplay( rankColorMatrix );
       		
       		display1.saveImage("fig1.png", true);
       		display2.saveImage("fig2.png", true);
       		
       	}catch(Exception e){
       		e.printStackTrace();
       		return null;
       	}
       	
		return null;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		CoExpressionAnalysisCli analysis = new CoExpressionAnalysisCli();
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
