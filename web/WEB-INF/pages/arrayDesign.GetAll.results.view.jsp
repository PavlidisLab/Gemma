<%@ include file="/common/taglibs.jsp"%>
<%@ page import="java.util.*"%>
<%@ page import="edu.columbia.gemma.expression.arrayDesign.ArrayDesign"%>

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
	<display:table name="arrayDesigns" class="list" requestURI="" id="arrayDesignList" export="true">
		<display:column property="name" sort="true" href="search.htm?_flowId=arrayDesign.Detail&_eventId=" paramId="name" paramProperty="name" titleKey="arrayDesign.name"/>
		<display:column property="description" sort="true" titleKey="arrayDesign.description"/>
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
