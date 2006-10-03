package ubic.gemma.analysis.linkAnalysis;

import ubic.basecode.bio.geneset.GeneAnnotations;
import ubic.basecode.dataStructure.matrix.DoubleMatrixNamed;
import ubic.gemma.persistence.Persister;
import ubic.gemma.persistence.PersisterHelper;

/**
 * This class is to set the analysis parameters for linkAnalysis.
 * @author xiangwan
 */
public class LinkAnalysisDataLoader extends ExpressionDataLoader {
    
	private String metric = "pearson";
    private DoubleMatrixNamed dataMatrix = null;
    private double tooSmallToKeep = 0.5;
    private GeneAnnotations duplicateMap;
    private boolean absoluteValue = false;
    
	public LinkAnalysisDataLoader(String paraExperimentName,String goFile) {
		// TODO Auto-generated constructor stub
		super(paraExperimentName, goFile);
	}
	
	public String getMetric() { return this.metric;}
	public DoubleMatrixNamed getDataMatrix() { return this.dataMatrix;}
	public double getCacheCut() { return this.tooSmallToKeep;};
	public GeneAnnotations getGeneAnnotations() { return this.duplicateMap;};
	public boolean getAbsoluteValue() { return this.absoluteValue;};
}
