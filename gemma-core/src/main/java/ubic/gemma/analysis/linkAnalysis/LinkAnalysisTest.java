package ubic.gemma.analysis.linkAnalysis;

import ubic.basecode.dataStructure.Link;
import cern.colt.list.ObjectArrayList;


public class LinkAnalysisTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		/*DbManager dbManager;
		try{
			dbManager = new DbManager();
			ObjectArrayList temp = new ObjectArrayList();
			Link oneLink = new Link(1,2,0.3);
			temp.add(oneLink);
			dbManager.addLinks(temp, null);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		System.exit(0);
		*/
		System.err.println("Test LinkAnalysis");
//		LinkAnalysisDataLoader oneLoader = new LinkAnalysisDataLoader("GSE2276","an.txt");
//		LinkAnalysis oneAnalysis = new LinkAnalysis(oneLoader);
		System.err.println("Finish Loading");
		//oneLoader.writeExpressionDataToFile("2276.data");
		//oneLoader.writeDataIntoFile("2276_matrix.data");
//		oneAnalysis.analysis();
		System.err.println("Finish test");
	}

}
