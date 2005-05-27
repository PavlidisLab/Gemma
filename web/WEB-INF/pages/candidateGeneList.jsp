
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
<script language="javascript">
	function doAddNew(){
		newname = prompt("Enter name for new Candidate List");
		if(newname != null)
			document.location = "candidateGeneList.htm?action=add&newName=" + newname
	}
	function doDel(idDel){
		if( confirm("Confirm: Delete this list permanently?")) 
			document.location = "candidateGeneList.htm?id=" + idDel + "&action=delete";
	}
</script>

<content tag="heading">Candidate Gene Lists</content>

<h3>Candidate Gene Lists:</h3><BR>

<table bgcolor="#eeeeee" colspacing="1" cellpadding="2">
<tr>
	<td width="150"><b>Name</b></td>
	<td><b>Description</b></td>
	<td width="100">&nbsp;</td></tr>
<%

Map m = (Map)request.getAttribute("model");
if( m!=null) {
	Collection results=(Collection)m.get("candidateGeneLists");

	Iterator iter = results.iterator();
	CandidateGeneList cgl = null;
	while (iter.hasNext()) {
		cgl=(CandidateGeneList)iter.next();
		String desc = cgl.getDescription();
		if( desc == null ) 
			desc = "<i>n.a.</i>";
		%>
		<tr>
			<td bgcolor="white"><a href="candidateGeneListDetail.htm?id=<%=cgl.getId()%>"><%=cgl.getName()%></a></td>
			<td bgcolor="white"><%=desc%></td>
			<td align="center" bgcolor="white"><a href="javascript:doDel(<%=cgl.getId()%>)">delete</a></td>
		</tr>
		<%
	}
}
%>
</table>
<P>
<a href="javascript:doAddNew()">Add new Candidate List</a>

</body>
</html>


