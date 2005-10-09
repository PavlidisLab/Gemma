<%@ include file="/common/taglibs.jsp"%>
<%@ page import="java.util.*"%>
<%@ page import="edu.columbia.gemma.expression.experiment.ExpressionExperiment"%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<HTML>
<HEAD>
</HEAD>
<BODY>

<FORM name="newSearchForm" action=""><INPUT
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
		<display:column property="name" sort="true" titleKey="expressionExperiment.name"/>
		<display:column property="source" sort="true" href="expressionExperimentDetails.htm" paramId="source" paramProperty="source" titleKey="expressionExperiment.source"/>
		<display:column property="experimentalDesigns" sort="true" href="experimentalDesigns.htm" titleKey="experimentalDesigns"/>
		<display:column property="bioAssays" sort="true" href="bioAssays.htm" titleKey="bioAssays"/>
		<display:setProperty name="basic.empty.showtable" value="true"/>
	</display:table>	
	</TR>
	<TR>
		<TD>
		<HR>
		</TD>
	</TR>
	<TR>
		<TD>
		<DIV align="right"><INPUT type="button"
			onclick="javascript:document.newSearchForm.submit()"
			value="New Search"></DIV>
		</TD>
	</TR>
</TABLE>
</P>
</DIV>
</BODY>
</HTML>
