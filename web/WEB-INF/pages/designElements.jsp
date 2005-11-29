<%@ include file="/common/taglibs.jsp"%>
<%@ page import="java.util.*"%>
<%@ page import="edu.columbia.gemma.expression.designElement.DesignElement"%>

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
	<display:table name="designElements" class="list" requestURI="" id="designElementList" export="true">
		<display:column property="name" sort="true" titleKey="arrayDesign.name"/>
		<display:column property="description" sort="true" titleKey="arrayDesign.description"/>
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
