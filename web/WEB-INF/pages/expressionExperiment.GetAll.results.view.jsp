<%@ include file="/common/taglibs.jsp"%>
<%@ page import="java.util.*"%>
<%@ page import="edu.columbia.gemma.expression.experiment.ExpressionExperiment"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<HTML>
<HEAD>
</HEAD>
<BODY>

<FORM name="newSearchForm" action="mainMenu.htm"><INPUT
	type="hidden" name="_flowExecutionId"
	value="<%=request.getAttribute("flowExecutionId") %>"> <INPUT
	type="hidden" name="_eventId" value="newSearch"></FORM>
<DIV align="left">
<P>
<TABLE width="100%">
	<TR>
		<TD>
		<DIV align="left"><b>Search Results</b></DIV>
		</TD>
	</TR>
	<TR>
		<TD>
		<HR>
		</TD>
	</TR>
	<TR>
	<%--  display tag used here --%>
	<display:table name="expressionExperiments" class="list" requestURI="" id="expressionExperimentList" export="true">		
		<display:column property="accession.accession" sort="true" titleKey="expressionExperiment.id"/>
		<display:column property="name" sort="true" href="expressionExperimentDetails.htm" paramId="name" paramProperty="name" titleKey="expressionExperiment.name"/>
		<display:column property="source" sort="true" titleKey="expressionExperiment.source"/>
		<display:column title="Experimental Designs" sort="true" href="experimentalDesigns.htm" paramId="name" paramProperty="name" >
      		<c:out value="${fn:length(experimentalDesigns)}"/>
    	</display:column>
		<display:column title="Bioassays" sort="true" href="bioAssays.htm">
      		<c:out value="${fn:length(bioAssays)}"/>
    	</display:column>
		<display:setProperty name="basic.empty.showtable" value="true"/>
	</display:table>	
	</TR>
	<TR>
		<TD>
		<HR>
		</TD>
	</TR>
</TABLE>
</P>
</DIV>
</BODY>
</HTML>
