<%@ include file="/common/taglibs.jsp"%>

<%@ page import="java.util.Collection" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="edu.columbia.gemma.expression.experiment.ExpressionExperiment" %>
<%@ page import="edu.columbia.gemma.common.auditAndSecurity.Person" %>

<%

Map m = (Map)request.getAttribute("model");
if( m!=null) {
	request.setAttribute("experiments", (Collection)m.get("experiments"));
}

%>
<content tag="heading">Experiments</content>

<a href="mainMenu.html">Back to Main Menu</a>
<P>
<display:table name="experiments" class="list" id="experiment" requestURI="/ExperimentList.html">
	<display:setProperty name="basic.empty.showtable" value="true"/>

	<display:column sortable="true" property="name" href="ExperimentDetail.html" paramId="experimentID" paramProperty="id"/>
	<display:column title="Principle Investigator" property="owner.fullName" />
	<display:column property="description" />
	<display:column><a href="javascript:deleteExperiment(
	<c:out value="${experiment.id}"/>
	)">del</a></display:column>
</display:table >


<script language="javascript">
	function deleteExperiment(id){
		if( confirm("CONFIRM: Delete this experiment permanently?") ){
			document.addform.action.value="delete";
			document.addform.experimentID.value=id;
			document.addform.submit();
		}
	}
</script>

<form method="POST" name="addform" action="ExperimentList.html">
	<input type="hidden" name="action" id="action" value="add">
	<input type="hidden" name="experimentID" id="experimentID" value="">
	<b>New Experiment Name:</b><input type="text" name="newName" id="newName">&nbsp;
	<input type="submit"  value="Add" size="50">
</form>