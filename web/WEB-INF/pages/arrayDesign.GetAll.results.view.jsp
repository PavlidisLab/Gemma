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
		<TD>
		<TABLE BORDER="1">
			<TR>
				<TD><B>Name</B></TD>
				<TD><B>Description</B></TD>

			</TR>
			<%
        Collection results = ( Collection ) request.getAttribute( "arrayDesigns" );
        Iterator iter = results.iterator();
        while ( iter.hasNext() ) {
            ArrayDesign arrayDesign = ( ArrayDesign ) iter.next();
            %>
			<TR>
				<TD><A
					href="search.htm?_flowExecutionId=<%=request.getAttribute("flowExecutionId") %>&_eventId=select&name=<%=arrayDesign.getName() %>">
				<%=arrayDesign.getName() %> </A></TD>
				<TD><%=arrayDesign.getDescription() %></TD>
			</TR>
			<%
        }
    %>
		</TABLE>
		</TD>
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
