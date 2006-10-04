package ubic.gemma.analysis.linkAnalysis;

import ubic.gemma.persistence.Persister;
import ubic.gemma.persistence.PersisterHelper;
import ubic.gemma.testing.BaseTransactionalSpringContextTest;

public class LinkAnalysisTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		System.err.println("Test LinkAnalysis");
		LinkAnalysisDataLoader oneLoader = new LinkAnalysisDataLoader("GSE2276","an.txt");
		LinkAnalysis oneAnalysis = new LinkAnalysis(oneLoader);
		System.err.println("Finish Loading");
		//oneLoader.WriteExpressionDataToFile("2276.data");
		oneAnalysis.analysis();
		System.err.println("Finish test");
	}

}
