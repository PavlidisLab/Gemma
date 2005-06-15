
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

<a href="candidateGeneList.htm">Back to all Candidate Lists</a>
<P>&nbsp;</P>

<form name="dForm" id="dForm" method="POST" action="candidateGeneListDetail.htm">
<input type="hidden" name="action" id="action" value="update">
<input type="hidden" name="listID" id="listID" value="<%=request.getParameter("listID")%>">
<%
CandidateGene g = null;
Map m = (Map)request.getAttribute("model");
if( m!=null) {
	CandidateGeneList cgList=(CandidateGeneList) m.get("candidateGeneLists");
	Collection can = cgList.getCandidates();
	Iterator iter = can.iterator();
	String desc = cgList.getDescription();
	if( desc==null ){
		desc="";
	}
	%>
	<table width="100%" bgcolor="#eeeeee" cellpadding="1" cellspacing="2">
	<tr>
		<td colspan="2" nowrap="true" bgcolor="#eeeeee"><B>Candidate Gene List Details</B></td>
	</tr>
	<tr>
		<td bgcolor="white">Name:</td> 
		<td bgcolor="white"><input type="text" size="100" name="listName" id="listName" value="<%=cgList.getName()%>"></td>
	</tr>
	<tr>
		<td bgcolor="white">Description:</td> 
		<td bgcolor="white"> <input type="text" size="100" name="listDescription" id="listDescription" value="<%=desc%>"></td>
	</tr>
	<tr>
		<td valign="center" align="center" colspan="2" bgcolor="white"><input type="submit" value="Update Details"><BR><BR></td>
	</tr>
	</form>
	</table>
	<table width="100%" bgcolor="#eeeeee" cellpadding="1" cellspacing="2">
	<tr>
		<td colspan="2" nowrap="true" bgcolor="#eeeeee"><B>Candidate Genes</B></td>
	</tr>
	<%
	if( !iter.hasNext() ){
		%>
		<tr><td colspan="2" bgcolor="white"><i>No Genes in List</i></td></tr>
		<%
	}
	else{
		int ct = 1;
		while (iter.hasNext()) {
			g=(CandidateGene)iter.next();
			String gID = g.getId().toString();
			%>
			<tr>
				<td width="90%" bgcolor="white"><a href="geneDetail.htm?geneID=<%=g.getGene().getId()%>"><%=g.getName()%></a><BR><%=g.getDescription()%></td>
				<form method="POST" action="candidateGeneListDetail.htm">
				<input type="hidden" name="listID" id="listID" value="<%=request.getParameter("listID")%>">
				<input type="hidden" name="action" id="action" value="removegenefromcandidatelist">
				<input type="hidden" name="geneID" id="geneID" value="<%=gID%>">
				<td size='75px' bgcolor="white"><%=g.getDescription()%>
					<input type="submit" value="Remove">
				</td>
				</form>
				<%
				if( can.size()>1 && ct>1) {
				%>
				<form method="POST" action="candidateGeneListDetail.htm">
				<input type="hidden" name="listID" id="listID" value="<%=request.getParameter("listID")%>">
				<input type="hidden" name="action" id="action" value="movecandidateuponcandidatelist">
				<input type="hidden" name="geneID" id="geneID" value="<%=gID%>">
				<td size='75px' bgcolor="white">
					<input type="submit" value="Move Up">
				</td>
				</form>
				<%
				}
				else
					out.print("<td size='75px' bgcolor='white'>&nbsp;</td>");
				if( can.size()>1 && ct!=can.size()){
				%>
				<form method="POST" action="candidateGeneListDetail.htm">
				<input type="hidden" name="listID" id="listID" value="<%=request.getParameter("listID")%>">
				<input type="hidden" name="action" id="action" value="movecandidatedownoncandidatelist">
				<input type="hidden" name="geneID" id="geneID" value="<%=gID%>">
				<td size='75px' bgcolor="white">
					<input type="submit" value="Move Down">
				</td>
				</form>
				<%
				}
				else
					out.print("<td size='75px' bgcolor='white'>&nbsp;</td>");
				ct++;
				%>
			</tr>
		<%
		}
	}
}
%>
</table>
<a href="geneFinder.htm?listID=<%=request.getParameter("listID")%>">Add New Candidate Gene to List</a>

<P>

<p>&nbsp;<p>
<form method="POST" name="delform" action="candidateGeneListDetail.htm">
	<input type="hidden" name="action" id="action" value="delete">
	<input type="hidden" name="listID" id="listID" value="<%=request.getParameter("listID")%>">
	<input type="submit" value="Delete this list permanently">
</form>


</form>
</body>
</html>


