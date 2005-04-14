<%@ page session="false" %>

<%@ taglib uri="http://www.springframework.org/tags" prefix="spring" %>

<%--<jsp:useBean id="query" scope="request" class="edu.columbia.gemma.controller.domain.BibliographicReferenceQuery"/>--%>
<jsp:useBean id="query" scope="request" class="edu.columbia.gemma.common.description.BibliographicReferenceImpl"/>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<HTML>
	<HEAD>
	</HEAD>
	<BODY>
		<DIV align="left">
			<FORM name="searchForm" action="pubMedSearch.htm">
				<INPUT type="hidden" name="_flowExecutionId" value="<%=request.getAttribute("flowExecutionId") %>">
				<INPUT type="hidden" name="_eventId" value="submit">
				<TABLE width="100%">
					<TR>
						<TD>
							<DIV align="left">Search Criteria</DIV>
						</TD>
					</TR>
					<TR>
						<TD COLSPAN="2"><HR></TD>
					</TR>
					<spring:hasBindErrors name="query">
						<TR>
							<TD COLSPAN="2"><FONT color="red">Please provide valid query criteria!</FONT></TD>
						</TR>
					</spring:hasBindErrors>
					<TR>
						<TD>PubMed Id</TD>
						<TD><INPUT type="text" name="pubMedId" value=""></TD>
					</TR>
					<TR>
						<TD COLSPAN="2"><HR></TD>
					</TR>
					<TR>
  					    <TD COLSPAN="2">
						    <DIV align="right">
							    <INPUT type="button" onclick="javascript:document.searchForm.submit()" value="Search">
						    </DIV>
						</TD>
					</TR>
				</TABLE>
			</FORM>
		</DIV>
	</BODY>
	<A HREF="home.jsp">Home</A>
</HTML>