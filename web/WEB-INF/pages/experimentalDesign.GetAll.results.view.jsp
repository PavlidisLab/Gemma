<%@ include file="/common/taglibs.jsp"%>
<%@ page import="java.util.*"%>
<%@ page import="edu.columbia.gemma.expression.experiment.ExperimentalDesign"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<HTML>
<HEAD>
</HEAD>
<BODY>

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
	<display:table name="experimentalDesigns" class="list" requestURI="" id="experimentalDesignList" export="true">		
		<display:column property="name" sort="true" titleKey="experimentalDesign.name"/>
		<display:column property="description" sort="true" titleKey="experimentalDesign.description"/>
		<display:column property="replicateDescription" sort="true" titleKey="experimentalDesign.replicateDescription"/>
		<display:column property="qualityControlDescription" sort="true" titleKey="experimentalDesign.qualityControlDescription"/>
		<display:column property="normalizationDescription" sort="true" titleKey="experimentalDesign.normalizationDescription"/>
  		<display:column property="name" sort="true" href="expressionExperimentDetails.htm" paramId="name" paramProperty="name" titleKey="owning.expressionExperiment.name"/>
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
