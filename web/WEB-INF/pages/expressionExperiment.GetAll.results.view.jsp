<%@ include file="/common/taglibs.jsp"%>
<%@ page import="java.util.*"%>
<%@ page import="edu.columbia.gemma.expression.experiment.ExpressionExperiment"%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<HTML>
<HEAD>
</HEAD>
<BODY>
<FORM name="newSearchForm" action="expressionExperimentSearch.htm"><INPUT
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
        Collection results = ( Collection ) request.getAttribute( "expressionExperiments" );
        Iterator iter = results.iterator();
        while ( iter.hasNext() ) {
            ExpressionExperiment expressionExperiment = ( ExpressionExperiment ) iter.next();
            %>
			<TR>
				<TD><A
					href="expressionExperimentSearch.htm?_flowExecutionId=<%=request.getAttribute("flowExecutionId") %>&_eventId=select&experimentID=<%=expressionExperiment.getId() %>">
				<%=expressionExperiment.getName() %> </A></TD>
				<TD><%=expressionExperiment.getDescription() %></TD>
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
