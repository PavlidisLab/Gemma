<%@ include file="/common/taglibs.jsp"%>
<%@ page import="java.util.*"%>
<%@ page import="edu.columbia.gemma.expression.arrayDesign.ArrayDesign"%>

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
	<display:table name="arrayDesigns" class="list" requestURI="" id="arrayDesignList" export="true">
		<display:column property="name" sort="true" href="showArrayDesign.html" paramId="name" paramProperty="name" titleKey="arrayDesign.name"/>
		<display:column property="description" sort="true" titleKey="arrayDesign.description"/>
		<display:column property="designProvider.name" sort="true" href="/auditAndSecurity/showContact.html" paramId="name" paramProperty="name" titleKey="arrayDesign.manufacturer"/>
		<display:column title="Design Elements" sort="true" href="/designElement/showAllDesignElements.html" paramId="name" paramProperty="name" >
      		<c:out value="${fn:length(designElements)}"/>
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
