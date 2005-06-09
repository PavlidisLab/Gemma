
<%@ page contentType="text/html; charset=iso-8859-1" errorPage="" %>
<%@ page import="java.util.Collection" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="edu.columbia.gemma.genome.gene.CandidateGeneList" %>
<%@ page import="edu.columbia.gemma.genome.gene.CandidateGene" %>
<html>
<head>
	<title>Candidate Gene Lists</title>
	<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
</head>
<body bgcolor="#ffffff">

<content tag="heading">Candidate Gene Lists</content>
<a href="candidateGeneList.htm?limit=U">Show only my Candidate Lists</a><BR>
<a href="candidateGeneList.htm">Show all Candidate Lists</a><BR>
<form name="cForm" id="cForm" action="candidateGeneList.htm" method="POST">
<input type="hidden" name="action" id="action" value="addme">
<input type="hidden" name="newName" id="newName">
<input type="hidden" name="listID" id="listID">


<table bgcolor="#eeeeee" colspacing="1" cellpadding="2">
<tr>
	<td width="150"><b>Name</b></td>
	<td><b>Description</b></td>
<%

Map m = (Map)request.getAttribute("model");
if( m!=null) {
	Collection results=(Collection)m.get("candidateGeneLists");

	Iterator iter = results.iterator();
	CandidateGeneList cgl = null;
	while (iter.hasNext()) {
		cgl=(CandidateGeneList)iter.next();
		String desc = cgl.getDescription();
		if( desc == null ){ 
			desc = "<i>n.a.</i>";
		}
		%>
		<tr>
			<td bgcolor="white"><a href="candidateGeneListDetail.htm?listID=<%=cgl.getId()%>"><%=cgl.getName()%></a></td>
			<td bgcolor="white"><%=desc%></td>
		</tr>
		<%
	}
}
%>
</table>

</form>
<P>&nbsp;</P>
<form method="POST" name="addform" action="candidateGeneList.htm">
	<input type="hidden" name="action" id="action" value="add">
	<b>New List Name:</b><input type="text" name="newName" id="newName">&nbsp;
	<input type="submit"  value="Add" size="50">
</form>
</body>
</html>


