package ubic.gemma.analysis.linkAnalysis;

import corejava.Format;
import cern.colt.list.DoubleArrayList;
import ubic.basecode.math.CorrelationStats;
import ubic.basecode.math.Stats;

/*
* @author xiangwan
*/

public class LinkAnalysis {
	
	private LinkAnalysisDataLoader dataLoader;
	private MatrixRowPairAnalysis metricMatrix;
    private DoubleArrayList cdf;
	private String metric = "pearson";
    private double tooSmallToKeep = 0.5;
    private boolean absoluteValue = false;
    private int uniqueItems = 0;
    
    private double fwe = 0.01;
    private double cdfCut = 0.01; // 1.0 means, keep everything.
    private double upperTailCut;
    private double lowerTailCut;
    
    private Format form;

	public LinkAnalysis(ExpressionDataLoader paraExpressionDataLoader) 
	{
		// TODO Auto-generated constructor stub
		this.dataLoader = (LinkAnalysisDataLoader)paraExpressionDataLoader;
		this.uniqueItems = this.dataLoader.geneAnnotations.numGenes();
        form = new Format( "%.4g" );
	}
	
	private void filter()
	{
		
	}
	private void calculateDistribution()
	{
        if ( metric.equals("pearson" ) ) {
            metricMatrix = MatrixRowPairAnalysisFactory.pearson( this.dataLoader.getDataMatrix(), this.tooSmallToKeep );
        } else if (metric.equals( "spearmann" ) ) {
            // metricMatrix = MatrixRowPairAnalysisFactory.spearman(dataMatrix, tooSmallToKeep);
        }

        metricMatrix.setUseAbsoluteValue( this.absoluteValue);
        metricMatrix.calculateMetrics( this.dataLoader.getGeneAnnotations());
        System.err.println( "Completed first pass over the data. Cached " + metricMatrix.numCached()
                + " values in the correlation matrix with values over " + this.tooSmallToKeep );

	}   
	public void chooseCutPoints() 
	{

        if ( cdfCut <= 0.0 ) {
            upperTailCut = 1.0;
            lowerTailCut = -1.0;
            return;
        }

        if ( cdfCut >= 1.0 ) {
            upperTailCut = 0.0;
            lowerTailCut = 0.0;
            return;
        }

        double cdfTailCut = cdfCut;
        double cdfUpperCutScore = 0.0;
        double cdfLowerCutScore = 0.0;

        // find the lower tail cutpoint, if we have to.
        if ( !this.absoluteValue ) {
            cdfTailCut /= 2.0;
            // find the lower cut point. Roundoff could be a problem...really need two cdfs or do it directly from
            // histogram.
            	for (  int i = 0; i < cdf.size(); i++  ) 
            	{
            		if ( 1.0 - cdf.get( i ) >= cdfTailCut )
            		{
            			cdfLowerCutScore = metricMatrix.getHistogram().xAxis().binUpperEdge(i == cdf.size() ? i : i + 1);
            			break;
            		}
            	}
            	System.err.println( form.format( cdfLowerCutScore ) + " is the lower cdf cutpoint at " + cdfTailCut );
        }

        // find the upper cut point.
        for ( int i = cdf.size() - 1; i >= 0; i--) {
            if ( cdf.get( i ) >= cdfTailCut ) {
                cdfUpperCutScore = metricMatrix.getHistogram().xAxis().binLowerEdge(i);
                break;
            }
        }


        System.err.println( form.format( cdfUpperCutScore ) + " is the upper cdf cutpoint at " + cdfTailCut );

        // get the cutpoint based on statistical signficance.
        double maxP = 1.0;
        double scoreAtP = 0.0;
        if ( fwe != 0.0 ) {
            maxP = fwe / uniqueItems; // bonferroni.
            scoreAtP = CorrelationStats.correlationForPvalue( maxP, this.dataLoader.getDataMatrix().columns() );
            System.err.println( "Minimum correlation to get " + form.format( maxP ) + " is about "
                    + form.format( scoreAtP ) + " for " + uniqueItems + " unique items (if all " + this.dataLoader.getDataMatrix().columns()
                    + " items are present)" );
        }
        this.metricMatrix.setPValueThreshold( maxP ); // this is the corrected value.

        upperTailCut = Math.max( scoreAtP, cdfUpperCutScore );
        System.err.println( "Final upper cut is " + form.format( upperTailCut ) );

        if ( !this.absoluteValue ) {
            lowerTailCut = Math.min( -scoreAtP, cdfLowerCutScore );
            System.err.println( "Final lower cut is " + form.format( lowerTailCut ) );
        }
        
        metricMatrix.setUpperTailThreshold( upperTailCut );
        if ( absoluteValue ) {
            metricMatrix.setLowerTailThreshold( upperTailCut );
        } else {
            metricMatrix.setLowerTailThreshold( lowerTailCut );
        }

    }

	private void getLinks()
	{
        cdf = Stats.cdf( metricMatrix.getHistogramArrayList() );
        chooseCutPoints();
        metricMatrix.calculateMetrics( this.dataLoader.getGeneAnnotations());
	}
	public void analysis()
	{
		this.filter();
		this.calculateDistribution();
		this.getLinks();
	}
}
