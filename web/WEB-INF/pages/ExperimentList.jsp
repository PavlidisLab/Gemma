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
<display:table name="experiments" class="list" requestURI="/ExperimentList.html">
	<display:setProperty name="basic.empty.showtable" value="true"/>
	<display:column sortable="true" property="name" href="ExperimentDetail.html" paramId="ExperimentID" paramProperty="id"/>
	<display:column title="Principle Investigator" property="owner.fullName" />
	<display:column property="description" />
</display:table >
	