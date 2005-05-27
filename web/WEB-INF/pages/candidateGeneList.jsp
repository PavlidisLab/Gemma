
<%@ page contentType="text/html; charset=iso-8859-1" errorPage="" %>
<%@ page import="java.util.Collection" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="edu.columbia.gemma.genome.gene.CandidateGeneList" %>

<html>
<head>
	<title>Candidate Gene Lists</title>
	<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
</head>
<body bgcolor="#ffffff">
<content tag="heading"><fmt:message key="menu.CandidateGeneList"></content>

<h3>Candidate Gene Lists:</h3><BR>
<table bgcolor="#eeeeee" colspacing="1">
<tr><td width="150"<b>Name</b></td><td><b>Description</b></td></tr>
<%

Map m = (Map)request.getAttribute("model");
if( m!=null) {
	Collection results=(Collection)m.get("candidateGeneLists");

	Iterator iter = results.iterator();
	CandidateGeneList cgl = null;
	while (iter.hasNext()) {
		cgl=(CandidateGeneList)iter.next();
		%>
		<tr>
			<td bgcolor="white"><a href="candidateGeneListDetail.jsp?id=<%=cgl.getId()%>"><%=cgl.getName()%></a></td>
			<td bgcolor="white"><%=cgl.getDescription()%></td>
		</tr>
		<%
	}
}
%>
</table>

</body>
</html>


