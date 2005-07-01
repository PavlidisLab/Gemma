<%@ page contentType="text/html; charset=iso-8859-1" errorPage="" %>
<%@ page import="java.util.Collection" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="edu.columbia.gemma.expression.experiment.ExpressionExperiment" %>
<%@ page import="edu.columbia.gemma.common.auditAndSecurity.Person" %>
<html>
<head>
	<title>Experiments</title>
	<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
</head>
<body bgcolor="#ffffff">

<content tag="heading">Experiments</content>


<table bgcolor="#eeeeee" colspacing="1" cellpadding="2">
<tr>
	<td width="150"><b>Name</b></td>
	<td><b>Description</b></td>
	<td><b>Principle Investigator</b></td>
</tr>
<%

Map m = (Map)request.getAttribute("model");
if( m!=null) {
	Collection results=(Collection)m.get("experiments");
	Iterator iter = results.iterator();
	ExpressionExperiment ee = null;
	while (iter.hasNext()) {
		ee=(ExpressionExperiment)iter.next();
		String desc = ee.getDescription();
		if( desc == null ){ 
			desc = "<i>n.a.</i>";
		}
		Person pi = (Person)ee.getOwner();
		String piName = null;
		if( pi==null){
			piName = "Unassigned";
		}
		else{
			piName = pi.getFullName();
		}
		%>
		<tr>
			<td bgcolor="white"><a href="ExperimentDetail.htm?experimentID=<%=ee.getId()%>"><%=ee.getName()%></a></td>
			<td bgcolor="white"><%=desc%></td>
			<td bgcolor="white"><%=piName%></td>
		</tr>
		<%
	}
}
%>
</table>


<P>&nbsp;</P>

<form method="POST" name="addform" action="ExperimentList.htm">
	<input type="hidden" name="action" id="action" value="add">
	<b>New Experiment Name:</b><input type="text" name="newName" id="newName">&nbsp;
	<input type="submit"  value="Add" size="50">
</form>
</body>
</html>