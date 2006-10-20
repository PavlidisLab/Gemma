package ubic.gemma.analysis.linkAnalysis;

import java.sql.SQLException;

import cern.colt.list.ObjectArrayList;
import ubic.basecode.dataStructure.Link;
import ubic.basecode.dataStructure.matrix.DoubleMatrixNamed;
import ubic.basecode.db.Handle;


public class DbManager {
	private Handle dbHandle;
	
	public DbManager() throws SQLException
	{
		dbHandle = new Handle();
	}
	public DbManager(String dataSet) throws SQLException
	{
		dbHandle = new Handle("localhost", dataSet);
	}
	public DbManager(String hostName, String dataSet) throws SQLException
	{
		dbHandle = new Handle(hostName, dataSet);
	}
	public Handle getDbHandle()
	{
		return this.dbHandle;
	}
	public void addLinks(ObjectArrayList linkLists, DoubleMatrixNamed dataMatrix) throws SQLException
	{
		if(linkLists.size() == 0) return;
		
		StringBuffer query = new StringBuffer( 10000 );
		
		this.dbHandle.runUpdateQuery("CREATE TEMPORARY TABLE  link_temp( " +
				  "p1 CHAR(40) NOT NULL," +
				  "p2 CHAR(40) NOT NULL," +
				  "score DOUBLE NOT NULL default 0," +
				  "pvalue DOUBLE NOT NULL default 0)");
		 
		/*
		this.dbHandle.runUpdateQuery( "CREATE TEMPORARY TABLE link_temp (d SMALLINT UNSIGNED NOT NULL,"
                + "p1 MEDIUMINT UNSIGNED NOT NULL, p2 MEDIUMINT UNSIGNED NOT NULL, s "
                + "SMALLINT UNSIGNED NOT NULL, pv SMALLINT UNSIGNED)" );
        */
		dbHandle.runUpdateQuery( "SET AUTOCOMMIT=0" );
		query.append( "INSERT INTO link_temp VALUES " );
		
		for(int i = 0; i < linkLists.size(); i++)
		{
			Link link = (Link)linkLists.get(i);
			double weight = link.getWeight();
			int x = link.getx();
			int y = link.gety();
			String p1 = dataMatrix.getRowName(x);
			String p2 = dataMatrix.getRowName(y);
			
		//	System.err.println(p1+"-----"+p2);
		//	if(true) continue;
			query.append("('" + p1 + "','" + p2 + "'," + weight+","+ 0.0 + ")");
			
			
			if( i == linkLists.size()-1 || i % 10000 == 0)
			{
				//System.err.println(query.toString());
				dbHandle.runUpdateQuery(query.toString());
				query.delete(0, query.length());
				query.append( "INSERT INTO link_temp VALUES " );
				if(i%20*10000 == 0 || i == linkLists.size() -1)
				{
					dbHandle.runUpdateQuery("COMMIT");
				}
			}
			else
				query.append(",");
		}
		dbHandle.runUpdateQuery("INSERT INTO linktemp select * from link_temp");
		dbHandle.runUpdateQuery( "DROP TABLE link_temp" );
		dbHandle.runUpdateQuery( "SET AUTOCOMMIT=1" );
		
		return;
	}
}
