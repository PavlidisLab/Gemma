<%@ page contentType="text/html; charset=iso-8859-1" errorPage="" %>
<%@ page import="edu.columbia.gemma.genome.Gene" %>
<%@ page import="edu.columbia.gemma.genome.gene.GeneAlias" %>
<%@ page import="edu.columbia.gemma.genome.Taxon" %>
<%@ page import="edu.columbia.gemma.common.description.BibliographicReference" %>
<%@ page import="edu.columbia.gemma.common.description.DatabaseEntry" %>
<%@ page import="java.util.Collection" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="java.util.Map" %>
<html>
<head>
	<title>Gene Detail</title>
	<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
</head>
<body>
<content tag="heading">Gene Detail</content>

<%
Map m = (Map)request.getAttribute("model");
Gene g = null;
String ncbi=null;
String aliaslist=null;
String citelist=null;
String acclist=null;
String productlist=null;
BibliographicReference br = null;
DatabaseEntry db = null;
Taxon t=null;

g = (Gene) m.get("gene");

if(g==null)
	out.print("whoops, gene is null.");
else{
	t = g.getTaxon();
	ncbi = g.getNcbiId();
	if(ncbi==null)
		ncbi="None known";
		
	if( g.getGeneAliasses()==null || g.getGeneAliasses().size()==0 )
		aliaslist="None known";
	else{
		for(Iterator iter=g.getGeneAliasses().iterator(); iter.hasNext();){
			aliaslist += ((GeneAlias)iter.next()).getAlias();
			if( iter.hasNext() ){
				aliaslist +=  ", ";
			}	
		}
	}
	
	if( g.getCitations()==null || g.getCitations().size()==0 )
		citelist="None known";
	else{
		for(Iterator iter=g.getCitations().iterator(); iter.hasNext();){
			br = (BibliographicReference) iter.next();
			citelist += br.getCitation() ;
			if( iter.hasNext() ){
				citelist +=  "<BR>";
			}	
		}
	}

	if( g.getAccessions()==null || g.getAccessions().size()==0 )
		acclist="None known";
	else{
		for(Iterator iter=g.getAccessions().iterator(); iter.hasNext();){
			db = (DatabaseEntry) iter.next();
			acclist += db.getAccession() ;
			if( iter.hasNext() ){
				acclist +=  "<BR>";
			}	
		}
	}
		
	%>
	
	<table>
		<tr><td>Official Name:</td><td><%=g.getOfficialName()%></td></tr>
		<tr><td>Official Symbol:</td><td><%=g.getOfficialSymbol()%></td></tr>
		<tr><td>NCBI ID:</td><td><%=ncbi%></td></tr>
		<tr><td>Aliases:</td><td><%=aliaslist%></td></tr>
		<tr><td>Citations:</td><td><%=citelist%></td></tr>
		<tr><td>Accessions:</td><td><%=acclist%></td></tr>
		<tr><td>Products:</td><td><%=productlist%></td></tr>
		<tr><td>Taxon:</td><td><%=t.getScientificName()%> (<%=t.getCommonName()%>)</td></tr>
	</table>
	<%
}
%>

</body>
</html>