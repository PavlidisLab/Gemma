<%@ include file="/common/taglibs.jsp"%>
<%@ page import="java.util.Map"%>
<%@ page import="java.util.Collection"%>
<%@ page import="ubic.gemma.model.genome.gene.CandidateGeneList"%>
<%@ page import="ubic.gemma.model.genome.gene.CandidateGene"%>
<html>
	<head>
		<title>Candidate Gene Lists</title>
		<meta http-equiv="Content-Type"
			content="text/html; charset=iso-8859-1">
	</head>
	<body bgcolor="#ffffff">

		<content tag="heading">
		Candidate Gene Lists
		</content>
		<a href="candidateGeneList.htm?limit=U">Show only my Candidate
			Lists</a>
		<BR>
		<a href="candidateGeneList.htm">Show all Candidate Lists</a>
		<BR>
		<form name="cForm" id="cForm" action="candidateGeneList.htm"
			method="POST">
			<input type="hidden" name="action" id="action" value="addme">
			<input type="hidden" name="newName" id="newName">
			<input type="hidden" name="listID" id="listID">

			<%
			            Map m = ( Map ) request.getAttribute( "model" );
			            if ( m != null ) {
			                request.setAttribute( "candidateGeneLists", m.get( "candidateGeneLists" ) );
			            }
			%>
			<display:table name="candidateGeneLists" class="list"
				requestURI="/candidateGeneList.htm">
				<display:setProperty name="basic.empty.showtable" value="true" />
				<display:column sortable="true" property="name"
					href="candidateGeneList.htm" paramId="listID" paramProperty="id" />
				<display:column title="List Owner" property="owner.fullName" />
				<display:column property="description" />
			</display:table>


		</form>
		<P>
			&nbsp;
		</P>
		<form method="POST" name="addform" action="candidateGeneList.htm">
			<input type="hidden" name="action" id="action" value="add">
			<b>New List Name:</b>
			<input type="text" name="newName" id="newName">
			&nbsp;
			<input type="submit" value="Add" size="50">
		</form>
	</body>
</html>



