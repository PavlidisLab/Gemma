<%@ page contentType="text/html; charset=iso-8859-1" errorPage="" %>
<%@ page import="edu.columbia.gemma.genome.Gene" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="java.util.Collection" %>
<html>
<head>
	<title>Candidate Gene Lists</title>
	<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
</head>
<body>


<content tag="heading">Gene Search</content>
From here you can search the Gemma database for existing Genes.<BR><BR>
<a href="candidateGeneListDetail.htm?listID=<%=request.getParameter("listID")%>">Back to Candidate List Detail</a>.<BR>
<BR>
<%
String allCheck="", byNameCheck="", bySymbolCheck="", bySymbolInexactCheck="";
String lookup = request.getParameter("lookup");
String searchtype = request.getParameter("searchtype");
if(lookup==null)
	lookup="";
if( searchtype==null )
	searchtype="all";

if( searchtype.compareTo("all")==0 )
	allCheck="checked='TRUE'";
if( searchtype.compareTo("byName")==0 )
	byNameCheck="checked='TRUE'";
if( searchtype.compareTo("bySymbol")==0 )
	bySymbolCheck="checked='TRUE'";
if( searchtype.compareTo("bySymbolInexact")==0 )
	bySymbolInexactCheck="checked='TRUE'";
%>
<form name="dForm" id="dForm" method="POST" action="geneFinder.htm">
<input type="hidden" name="listID" value="<%=request.getParameter("listID")%>">
<input type="hidden" name="geneID" id="geneID">
<input type="hidden" name="action" value="search">

<table width="100%" bgcolor="#eeeeee" cellpadding="1" cellspacing="2">
	<tr>
		<td bgcolor="#eeeeee"><B>Search For Genes in Gemma</B></td>
	</tr>
	<tr>
		<td bgcolor="white">
			<input type="radio" name="searchtype" id="searchtype" value="all" <%=allCheck%>> Return All Genes<BR>
			<input type="radio" name="searchtype" id="searchtype" value="byName" <%=byNameCheck%>>Search by Official Name<BR>
			<input type="radio" name="searchtype" id="searchtype" value="bySymbol" <%=bySymbolCheck%>>Search by Official Symbol (exact)<BR>
			<input type="radio" name="searchtype" id="searchtype" value="bySymbolInexact" <%=bySymbolInexactCheck%>>Search by Official Symbol (inexact)<BR>
			<BR><BR>
			<b>Search Term:</b><BR>
			<input name="lookup" id="lookup" value="<%=lookup%>">
		</td>
	</tr>
	<tr><td align="center" bgcolor="white"><input type="submit" value="Search"></td></tr>
</table>	
</form>	

<P>&nbsp;</P>
<%

Map m = (Map)request.getAttribute("model");
if( m==null) {
	out.print("Please enter a search term and press Search.");
}
else{
	out.print("<B>Search results:</B>");
	Collection can = (Collection) m.get("genes");
	Iterator iter = can.iterator();
	Gene g = null;
	if( !iter.hasNext() )
		out.print("No genes found that match this query.");
	else{
		out.print("<table><tr bgcolor='#eeeeee'><td width='150'><b>Name</b></td><td width='150'><b>Symbol</b></td><td><b>Description</b></td><td>&nbsp;</td></tr>");
		String desc = null;
		while(iter.hasNext()){
			g = (Gene) iter.next();
			desc = g.getDescription();
			if(desc=="")
				desc = "n.a.";
			%>
			<tr>
				<form method="POST" action="candidateGeneListDetail.htm">
				<input type="hidden" name="listID" id="listID" value="<%=request.getParameter("listID")%>">
				<input type="hidden" name="action" id="action" value="addgenetocandidatelist">
				<input type="hidden" name="geneID" id="geneID" value="<%=g.getId().toString()%>">
				<td><%=g.getOfficialName()%></td>
				<td><%=g.getOfficialSymbol()%></td>
				<td><%=desc%></td>
				<td><input type="submit" value="Add to list"></td>
				</form>
			</tr>
			<%
		}
		out.print("</table>");
	}
}

%>

</body>