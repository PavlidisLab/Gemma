<%@ page contentType="text/html; charset=iso-8859-1" errorPage="" %>
<%@ page import="edu.columbia.gemma.genome.Gene" %>
<%@ page import="edu.columbia.gemma.genome.gene.GeneAlias" %>
<%@ page import="edu.columbia.gemma.genome.Taxon" %>
<%@ page import="edu.columbia.gemma.common.description.BibliographicReference" %>
<%@ page import="edu.columbia.gemma.common.description.DatabaseEntry" %>
<%@ page import="edu.columbia.gemma.genome.gene.GeneProduct" %>
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
String oName = null;
String aliaslist="";
String acclist="";
String prodlist="";
BibliographicReference br = null;
DatabaseEntry db = null;
GeneProduct prod = null;
Taxon t=null;

g = (Gene) m.get("gene");

if(g==null)
	out.print("whoops, gene is null.");
else{
	t = g.getTaxon();
	ncbi = g.getNcbiId();
	if(ncbi==null)
		ncbi="None known";
	oName = g.getOfficialName();
	if(oName==null)
		oName="None known";	
		
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
	
	if( g.getProducts()==null || g.getProducts().size()==0){
		prodlist="None known";
	}
	else{
		for( Iterator iter=g.getProducts().iterator(); iter.hasNext();){
			prod = (GeneProduct)iter.next();
			prodlist += prod.getName();
			if( iter.hasNext() ){
				prodlist += "<BR>";
			}
		}
	}
	
	%>
	
	<table>
		<tr><td align="top">Official Name:</td><td><%=oName%></td></tr>
		<tr><td align="top">Official Symbol:</td><td><%=g.getOfficialSymbol()%></td></tr>
		<tr><td align="top">NCBI ID:</td><td><%=ncbi%></td></tr>
		<tr><td align="top">Aliases:</td><td><%=aliaslist%></td></tr>
		<tr><td align="top">Citations:</td><td><%
		
		if( g.getCitations()==null || g.getCitations().size()==0 )
			out.print("None known");
		else{
			for(Iterator iter=g.getCitations().iterator(); iter.hasNext();){
				br = (BibliographicReference) iter.next();
				
				out.print(br.getAuthorList() + ". <B>" + br.getTitle() + "</B>. ");
				if( br.getPublicationDate()!=null )
					out.print( br.getPublicationDate().toString());
				if(br.getPubAccession()!=null)
					out.print("<a href='http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=pubmed&dopt=Abstract&list_uids=" + br.getPubAccession().toString() + "</a>");
				%>
				<BR>
				Description:
				<form method="post" action="geneDetail.htm" id="addcite" name="addcite">
				<input type="hidden" name="geneID" value="<%=g.getId()%>">
				<input type="hidden" name="citationID" value="<%=br.getId()%>">
				<input type="hidden" name="action" value="updatecitation">
				<input type="text" name="description" id="description"><input type="submit" value="Add">
				</form>
				&nbsp;<a href='/Gemma/citationForm.htm?citationID=<%=br.getId()%>&referPage=geneDetail&referID=<%=g.getId()%>'>edit</a>
				&nbsp;&nbsp;&nbsp;&nbsp;<a href='/Gemma/geneDetail.htm?citationID=<%=br.getId()%>&geneID=<%=g.getId()%>&action=removecitation'>Remove From List</a>
				<%
				if( iter.hasNext() ){
					out.print("<BR>");
				}	
			}
		}
		
		%></td></tr>
		<tr><td>Accessions:</td><td><%=acclist%></td></tr>
		<tr><td>Products:</td><td><%=prodlist%></td></tr>
		<tr><td>Taxon:</td><td><%=t.getScientificName()%> (<%=t.getCommonName()%>)</td></tr>
	</table>
	<BR><BR>
	<form method="post" action="geneDetail.htm" id="addcite" name="addcite">
	<input type="hidden" name="geneID" value="<%=g.getId()%>">
	<input type="hidden" name="action" value="addcitation">
	Add Citation by Pubmed ID: <input type="text" name="pubmedID" id="pubmedID"><input type="submit" value="Add">
	</form>
	<%
}
%>

</body>
</html>