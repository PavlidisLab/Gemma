package ubic.gemma.analysis.linkAnalysis;

/*
* @author xiangwan
*/

public class LinkAnalysis {
	
	private LinkAnalysisDataLoader dataLoader;
	private MatrixRowPairAnalysis metricMatrix;
	
	public LinkAnalysis(ExpressionDataLoader paraExpressionDataLoader) 
	{
		// TODO Auto-generated constructor stub
		this.dataLoader = (LinkAnalysisDataLoader)paraExpressionDataLoader;
	}
	
	private void filter()
	{
		
	}
	private void calculateDistribution()
	{
        if ( this.dataLoader.getMetric().equals("pearson" ) ) {
            metricMatrix = MatrixRowPairAnalysisFactory.pearson( this.dataLoader.getDataMatrix(), this.dataLoader.getCacheCut() );
        } else if ( this.dataLoader.getMetric().equals( "spearmann" ) ) {
            // metricMatrix = MatrixRowPairAnalysisFactory.spearman(dataMatrix, tooSmallToKeep);
        }

        metricMatrix.setUseAbsoluteValue( this.dataLoader.getAbsoluteValue());
        metricMatrix.calculateMetrics( this.dataLoader.getGeneAnnotations());
        System.err.println( "Completed first pass over the data. Cached " + metricMatrix.numCached()
                + " values in the correlation matrix with values over " + this.dataLoader.getCacheCut() );

	}
	private void getLinks()
	{
		
	}
	public void analysis()
	{
		this.filter();
		this.calculateDistribution();
		this.getLinks();
	}
}
