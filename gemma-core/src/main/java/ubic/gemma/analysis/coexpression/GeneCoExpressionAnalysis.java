/**
 * 
 */
package ubic.gemma.analysis.coexpression;

import java.io.PrintStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix2DNamed;
import ubic.basecode.dataStructure.matrix.DoubleMatrixNamed;
import ubic.basecode.io.ByteArrayConverter;
import ubic.basecode.math.CorrelationStats;
import ubic.basecode.math.metaanalysis.CorrelationEffectMetaAnalysis;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Gene;

/**
 * @author xwan
 *
 */
public class GeneCoExpressionAnalysis {
	private class ExpressedData {
	    public DesignElementDataVector target = null;
	    public DesignElementDataVector coexpressed = null;
	    public ExpressedData(DesignElementDataVector target, DesignElementDataVector coexpressed){
	    	this.target = target; this.coexpressed = coexpressed;
	    }
	}
	private HashMap<Long, HashMap<Long, HashSet<DesignElementDataVector>>> dataVectors = null;
	
	private HashMap<DesignElementDataVector, Collection<Gene>> devToGenes = null;

	private HashMap<Long, Double> meanData = null;
	private HashMap<Long, Double> sqrtData = null;
	
	private HashMap<Long,Integer> targetGeneMap = null;
	private Object targetGeneArray[] = null;

	private HashMap<Long,Integer> dependentGeneMap = null;
	private Object dependentGeneArray[] = null;
	
	private HashMap<Long, Integer> eeMap = null;
	private Object eeArray[] = null;
	
	private ArrayList<Double> [][] coRelationData = null;
	
	private ArrayList<ExpressedData> [][] allExpressedData = null; 
	
	private ExpressionExperimentService eeService = null;
	
	private HashMap<Long, Double> devRank = null;
	
    private static Log log = LogFactory.getLog( GeneCoExpressionAnalysis.class.getName() );

	public GeneCoExpressionAnalysis(Set<Gene> targetGenes, Set<Gene> dependentGenes, Set<ExpressionExperiment> ees){
		meanData = new HashMap<Long, Double>();
		sqrtData = new HashMap<Long, Double>();

		targetGeneMap = new HashMap<Long, Integer>();
		targetGeneArray = targetGenes.toArray();
		for(int i = 0; i < targetGeneArray.length; i++) targetGeneMap.put(((Gene)targetGeneArray[i]).getId(), i);
		
		dependentGeneMap = new HashMap<Long, Integer>();
		dependentGeneArray = dependentGenes.toArray();
		for(int i = 0; i < dependentGeneArray.length; i++) dependentGeneMap.put(((Gene)dependentGeneArray[i]).getId(), i);

		
		eeMap = new HashMap<Long, Integer>();
		eeArray = ees.toArray();
		for(int i = 0; i < eeArray.length; i++) eeMap.put(((ExpressionExperiment)eeArray[i]).getId(),i);
		
		coRelationData = new ArrayList[targetGeneArray.length][dependentGeneArray.length];
		allExpressedData = new ArrayList[targetGeneArray.length][dependentGeneArray.length];
		
		for(int i = 0; i <targetGeneArray.length; i++)
			for(int j = 0; j < dependentGeneArray.length; j++){
				coRelationData[i][j] = new ArrayList();
				for(int k = 0; k < eeArray.length; k++) coRelationData[i][j].add(Double.NaN);
			}
		
		for(int i = 0; i <targetGeneArray.length; i++)
			for(int j = 0; j < dependentGeneArray.length; j++){
				allExpressedData[i][j] = new ArrayList();
				for(int k = 0; k < eeArray.length; k++) allExpressedData[i][j].add(null);
			}
		
		this.devRank = new HashMap<Long, Double>();
	}
	private void distributeDesignElementDataVector(Set<DesignElementDataVector> devs){

		dataVectors = new HashMap<Long, HashMap<Long,HashSet<DesignElementDataVector>>>();
		
		for(DesignElementDataVector dev:devs){
			ExpressionExperiment ee = dev.getExpressionExperiment();
			if(ee.getId() == null){
				System.err.println(ee+ " wrong! ");
			}
			HashMap<Long,HashSet<DesignElementDataVector>> geneToDevs = dataVectors.get(ee.getId());
			if(geneToDevs == null){
				geneToDevs = new HashMap<Long, HashSet<DesignElementDataVector>>();
				for(Long geneId:this.targetGeneMap.keySet()){
					geneToDevs.put(geneId, new HashSet<DesignElementDataVector>());
				}
				for(Long geneId:this.dependentGeneMap.keySet()){
					geneToDevs.put(geneId, new HashSet<DesignElementDataVector>());
				}
				dataVectors.put(ee.getId(), geneToDevs);
			}
			HashSet<Gene> geneSet = (HashSet)devToGenes.get(dev);
			for(Gene gene:geneSet){
				HashSet<DesignElementDataVector> mappedDevs = geneToDevs.get(gene.getId());
				/**The mapped gene for dev may not in both target genes and candidate genes***/
				if(mappedDevs != null){
					mappedDevs.add(dev);
				}
			}
		}
	}
	private void getDevRank(){
/*	
		for(Long eeId:dataVectors.keySet()){
			System.err.println("Process " + eeId);
			DoubleArrayList rankList = getDevRankForExpressionExperiment(eeId);
			if(rankList == null) continue;
			HashMap<Long,HashSet<DesignElementDataVector>> geneToDevs = dataVectors.get(eeId);
			Collection<Long> geneIds = geneToDevs.keySet();
			for(Long geneId:geneIds){
				HashSet<DesignElementDataVector> devs = geneToDevs.get(geneId);
				for(DesignElementDataVector dev:devs){
					if(this.devRank.get(dev.getId()) == null){
						double valueForRank = this.getValueForRank(dev);
						int pos = rankList.binarySearch(valueForRank);
						double rank = Double.NaN;
						if(pos >= 0 && pos < rankList.size()) rank = (double)pos/(double)rankList.size();
						this.devRank.put(dev.getId(), rank);
					}
				}
			}
		}
*/
	}
    private double correlationNorm( int numused, double sxx, double sx, double syy, double sy ) {
        return ( sxx - sx * sx / numused ) * ( syy - sy * sy / numused );
    }
	private double correl( double[] ival, double[] jval ) {
    /* do it the old fashioned way */
		int numused = 0;
    	double sxy = 0.0, sxx = 0.0,syy = 0.0,sx = 0.0,sy = 0.0;
    	for ( int k = 0; k < ival.length; k++ ) {
        	double xj = ival[k];
        	double yj = jval[k];
        	if ( !Double.isNaN(ival[k]) && !Double.isNaN(jval[k]) ) {
            	sx += xj;
            	sy += yj;
            	sxy += xj * yj;
            	sxx += xj * xj;
            	syy += yj * yj;
            	numused++;
        	}
    	}
    	double denom = this.correlationNorm(numused, sxx, sx, syy, sy);
    	double correl = ( sxy - sx * sy / numused ) / Math.sqrt( denom );
    	return correl;
	}
    private double correlFast( double[] ival, double[] jval, double meani, double meanj, double sqrti, double sqrtj ) {
        double sxy = 0.0;
        for ( int k = 0, n = ival.length; k < n; k++ ) {
            sxy += ( ival[k] - meani ) * ( jval[k] - meanj );
        }
        return sxy / ( sqrti * sqrtj );
    }
    private double weightedCoRelation(DesignElementDataVector devI, DesignElementDataVector devJ){
    	double corr = coRelation(devI, devJ);
    	if(!Double.isNaN(corr))
    	{
    		ByteArrayConverter bac = new ByteArrayConverter();
    		byte[] bytes = devI.getData();
    		double[] ival = bac.byteArrayToDoubles( bytes );
    		bytes = devJ.getData();
    		double[] jval = bac.byteArrayToDoubles( bytes );
    		int numsamples = 0;
    		for(int i = 0; i <ival.length; i++){
    			if(!Double.isNaN(ival[i]) && !Double.isNaN(jval[i]))
    				numsamples++;
    		}
    		double samplingVariance = 1;//CorrelationEffectMetaAnalysis.samplingVariance(corr, numsamples);
    		if(Double.isNaN(samplingVariance)){
    			corr = Double.NaN;
    		}else{
    			corr = corr/samplingVariance;
    		}
    	}
    	return corr;
    }
    private double CoRelation_Pvalue(DesignElementDataVector devI, DesignElementDataVector devJ){
    	double corr = coRelation(devI, devJ);
    	if(!Double.isNaN(corr))
    	{
    		ByteArrayConverter bac = new ByteArrayConverter();
    		byte[] bytes = devI.getData();
    		double[] ival = bac.byteArrayToDoubles( bytes );
    		bytes = devJ.getData();
    		double[] jval = bac.byteArrayToDoubles( bytes );
    		int numsamples = 0;
    		for(int i = 0; i <ival.length; i++){
    			if(!Double.isNaN(ival[i]) && !Double.isNaN(jval[i]))
    				numsamples++;
    		}
    		double p = CorrelationStats.pvalue( corr, numsamples );
    		if(p>=0.10)
    			corr = Double.NaN;
    		else{
        		double samplingVariance = 1; //CorrelationEffectMetaAnalysis.samplingVariance(corr, numsamples);
        		samplingVariance = 1;
        		corr = corr/samplingVariance;
    		}
    			
    	}
    	return corr;
    }

	private double coRelation(DesignElementDataVector devI, DesignElementDataVector devJ){
		double corr = 0;
        byte[] bytes = devI.getData();
        ByteArrayConverter bac = new ByteArrayConverter();
        double[] ival = bac.byteArrayToDoubles( bytes );
        bytes = devJ.getData();
		double[] jval = bac.byteArrayToDoubles( bytes );
		
		if(ival.length != jval.length){
//			System.err.print("Error in Dimension " + devI.getId()+ " " + ival.length + " (" + devI.getExpressionExperiment().getId() + ") ");
//			System.err.println(devJ.getId() +  " " + jval.length + " (" + devJ.getExpressionExperiment().getId() + ") ");
			return Double.NaN;
		}
		if(devI.getId() == devJ.getId()){
//			System.err.println("Error in " + devI.getExpressionExperiment().getId());
			return Double.NaN;
		}
		int i;
		for( i = 0; i <ival.length; i++){
			if(Double.isNaN(ival[i]) || Double.isNaN(jval[i]))
				break;
		}
		if(i == ival.length){
			double meani, meanj, sqrti, sqrtj;
			Double mean, sqrt;
			mean = meanData.get(devI.getId());
			sqrt = sqrtData.get(devI.getId());
			if(mean == null){
				double ax = 0.0,sxx = 0.0;
				for ( int j = 0; j < ival.length; j++ ) {
					ax += ival[j];
				}
				meani = ( ax / ival.length );

				for ( int j = 0; j < ival.length; j++ ) {
		                double xt = ival[j] - meani; /* deviation from mean */
		                sxx += xt * xt; /* sum of squared error */
	            }
				sqrti = Math.sqrt( sxx );
				
				meanData.put(devI.getId(),new Double(meani));
				sqrtData.put(devI.getId(),new Double(sqrti));
			}else{
				meani = mean.doubleValue();
				sqrti = sqrt.doubleValue();
			}
			mean = meanData.get(devJ.getId());
			sqrt = sqrtData.get(devJ.getId());
			if(mean == null){
				double ay = 0.0,syy = 0.0;
				for ( int j = 0; j < ival.length; j++ ) {
					ay += jval[j];
				}
				meanj = ( ay / ival.length );

				for ( int j = 0; j < ival.length; j++ ) {
		                double yt = jval[j] - meanj; /* deviation from mean */
		                syy += yt * yt; /* sum of squared error */
	            }
				sqrtj = Math.sqrt( syy );

				meanData.put(devJ.getId(),new Double(meanj));
				sqrtData.put(devJ.getId(),new Double(sqrtj));
			}else{
				meanj = mean.doubleValue();
				sqrtj = sqrt.doubleValue();
			}
			
			corr = correlFast(ival, jval, meani, meanj, sqrti, sqrtj);
		}else{
			corr = correl(ival, jval);
		}
		return corr;
	}
	private void calculateCoRelation(){
		for(Long eeId:dataVectors.keySet()){
			int eeIndex = this.eeMap.get(eeId);
			/*Calculate the paired gene coexpression values*****/
			HashMap<Long,HashSet<DesignElementDataVector>> geneToDevs = dataVectors.get(eeId);
			for(int i = 0; i < targetGeneArray.length; i++){
				Object[] devI = geneToDevs.get(((Gene)targetGeneArray[i]).getId()).toArray();
/*
				//Choose the one with highest rank
				double rank = 0;
				Object devWithHighestRank = null;
				for(int ii = 0; ii < allDevI.length; ii++){
					if(((DesignElementDataVector)allDevI[ii]).getRank() != null){
						if(((DesignElementDataVector)allDevI[ii]).getRank() > rank){
						rank = ((DesignElementDataVector)allDevI[ii]).getRank();
						devWithHighestRank = allDevI[ii];
						}
					}
				}
				Object devI[] = new Object[0];
				if(devWithHighestRank == null ){
					if(allDevI.length > 0){
						devI = new Object[1];
						devI[0] = allDevI[0];
					}
					
				}else{
					devI = new Object[1];
					devI[0] = devWithHighestRank;
				}
*/				
				int indexI = targetGeneMap.get(((Gene)targetGeneArray[i]).getId());
				for(int j = 0; j < dependentGeneArray.length; j++){
					Object[] devJ = geneToDevs.get(((Gene)dependentGeneArray[j]).getId()).toArray();
					int shift = devI.length > devJ.length? devI.length:devJ.length;
					TreeMap<Double, Integer> sortedData = new TreeMap<Double, Integer>();
					for(int ii = 0; ii < devI.length; ii++)
						for(int jj = 0; jj < devJ.length; jj++){
							double corr = this.coRelation((DesignElementDataVector)devI[ii], (DesignElementDataVector)devJ[jj]);
							if(corr != Double.NaN)
								sortedData.put(new Double(corr), new Integer(ii*shift + jj));
						}

					int indexJ = dependentGeneMap.get(((Gene)dependentGeneArray[j]).getId());
					if(sortedData.size() > 0){
						Object corrArray[] = sortedData.keySet().toArray();
						Double medianCorr = (Double)corrArray[corrArray.length/2];
						coRelationData[indexI][indexJ].set(eeIndex,medianCorr);
						Integer combinedIndex = sortedData.get(medianCorr);
						int devIndexI = combinedIndex.intValue()/shift;
						int devIndexJ = combinedIndex.intValue()%shift;
						allExpressedData[indexI][indexJ].set(eeIndex, new ExpressedData((DesignElementDataVector)devI[devIndexI], (DesignElementDataVector)devJ[devIndexJ]));
					}
					else
						coRelationData[indexI][indexJ].set(eeIndex,Double.NaN);
				}
			}
		}
	}
	public void output(PrintStream output, double presencePercent){
		NumberFormat nf = NumberFormat.getNumberInstance();
		DecimalFormat df = (DecimalFormat)nf;
		df.applyPattern("#.####");
		output.print("Experiments");
		for(int i = 0; i < this.coRelationData.length; i++)
			for(int j = 0; j <this.coRelationData[i].length; j++)
					output.print("\t"+ ((Gene)targetGeneArray[i]).getName() + "_"+ ((Gene)dependentGeneArray[j]).getName());
		output.println();

		Object allEEs[] = eeMap.keySet().toArray();
		double total = this.coRelationData.length * this.coRelationData[0].length;
		for(int ee = 0; ee < allEEs.length; ee++){
			int eeIndex = this.eeMap.get((Long)allEEs[ee]);
			//Check the missing percentage
			double missing = 0;
			for(int i = 0; i < this.coRelationData.length; i++)
				for(int j = 0; j <this.coRelationData[i].length; j++)
					if(Double.isNaN(this.coRelationData[i][j].get(eeIndex)))
						missing++;
			if((total-missing)/total < presencePercent) continue;
	
			output.print(((ExpressionExperiment)this.eeArray[eeIndex]).getShortName());
			for(int i = 0; i < this.coRelationData.length; i++)
				for(int j = 0; j <this.coRelationData[i].length; j++){
					if(Double.isNaN(this.coRelationData[i][j].get(eeIndex)))
						output.print("\t");
					else
						output.print("\t"+ df.format(this.coRelationData[i][j].get(eeIndex)));
					}
			output.println();
		}
	}
	private double getExpressionRank(ExpressedData expressedData){
		Double rank = null;
		/*
		if(this.filtered(expressedData.target)){
			if(this.filtered(expressedData.coexpressed))
				mark = -1;
			else
				mark = 0;
		}else{
			if(this.filtered(expressedData.coexpressed))
				mark = 1;
			else
				mark = 2;
		}
		*/
		rank = expressedData.coexpressed.getRank();
		rank = expressedData.target.getRank();
		if(rank == null) rank = new Double(0);
		return rank.doubleValue();
	}
	public DoubleMatrixNamed getRankMatrix(DoubleMatrixNamed dataMatrix){
		double[][] rank = new double[dataMatrix.rows()][dataMatrix.columns()];
		DoubleMatrixNamed rankMatrix = new DenseDoubleMatrix2DNamed(rank);
		rankMatrix.setRowNames(dataMatrix.getRowNames());
		rankMatrix.setColumnNames(dataMatrix.getColNames());

		Object allEEs[] = eeMap.keySet().toArray();
		for(int ee = 0; ee < allEEs.length; ee++){
			int eeIndex = this.eeMap.get((Long)allEEs[ee]);
			String rowName = ((ExpressionExperiment)this.eeArray[eeIndex]).getShortName();
			int rowIndex = 0;
			try{
				rowIndex = dataMatrix.getRowIndexByName(rowName);
			}catch(Exception e){
				continue;
			}
			for(int i = 0; i < this.allExpressedData.length; i++)
				for(int j = 0; j <this.allExpressedData[i].length; j++){
					String colName = ((Gene)targetGeneArray[i]).getName() + "_"+ ((Gene)dependentGeneArray[j]).getName();
					int colIndex = 0;
					try{
						colIndex = dataMatrix.getColIndexByName(colName);
					}catch(Exception e){
						System.err.println("Col " + colName + " couldn't be found");
						continue;
					}
					if(!Double.isNaN(this.coRelationData[i][j].get(eeIndex)))
						rankMatrix.set(rowIndex, colIndex,getExpressionRank(this.allExpressedData[i][j].get(eeIndex)));
					else
						rankMatrix.set(rowIndex, colIndex,Double.NaN);
				}
		}
		return rankMatrix;
	}

	public boolean analysis(Set<DesignElementDataVector> devs){
		assert(this.devToGenes != null);
		assert(this.eeService != null);
		assert(this.eeMap.size() != 0);
		assert(this.targetGeneMap.size() != 0);
		assert(this.dependentGeneMap.size() != 0);
		this.distributeDesignElementDataVector(devs);
		this.getDevRank();
		this.calculateCoRelation();
		return true;
	}
	public void setDevToGenes(Map<DesignElementDataVector, Collection<Gene>> devToGenes) {
		this.devToGenes = (HashMap)devToGenes;
	}
	public void setExpressionExperimentService(ExpressionExperimentService eeService){
		this.eeService = eeService;
	}
}
