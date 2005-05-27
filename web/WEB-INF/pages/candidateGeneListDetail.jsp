
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
<content tag="heading">Candidate Gene List Detail View</content>




<%
CandidateGene g = null;
Map m = (Map)request.getAttribute("model");
if( m!=null) {
	CandidateGeneList cg=(CandidateGeneList) m.get("candidateGeneLists");
	out.println("Name: " + cg.getName() + "<BR>");
	out.println("Description: " + cg.getDescription() + "<BR>");
	Collection can = cg.getCandidates();
	Iterator iter = can.iterator();
	%>
	<table bgcolor="#eeeeee" colspacing="1">
		<tr><td width="150"<b>Name</b></td><td><b>Description</b></td></tr>
	<%
	if( !iter.hasNext() )
		out.println("<tr><td>No Genes in List</td></tr>");
	else{
		while (iter.hasNext()) {
			g=(CandidateGene)iter.next();
			%>
			<tr>
				<td bgcolor="white"><a href="GeneDetail.jsp?id=<%=g.getId()%>"><%=g.getName()%></a></td>
				<td bgcolor="white"><%=g.getDescription()%></td>
			</tr>
		<%
		}
		%></table><%
	}
}
%>
</body>
</html>


