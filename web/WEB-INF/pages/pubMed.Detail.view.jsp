<%@ include file="/common/taglibs.jsp"%>
<%@ page import="java.util.*" %>
<%@ page import="edu.columbia.gemma.common.description.BibliographicReference" %>

<jsp:useBean id="bibliographicReference" scope="request" class="edu.columbia.gemma.common.description.BibliographicReferenceImpl"/>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<HTML>
	<HEAD></HEAD>
	<BODY>
		<FORM name="backForm" action="pubMedDetail.htm">
				<INPUT type="hidden" name="_flowExecutionId" value="<%=request.getAttribute("flowExecutionId") %>">
				<INPUT type="hidden" name="_eventId" value="back">
		</FORM>
		<TABLE width="100%">
			<TR>
				<TD><b>Bibliographic Reference Details</b></TD>
			</TR>
			<TR>
				<TD COLSPAN="2"><HR></TD>
			</TR>
			<TR>
				<TD><B>Author List</B></TD>
				<TD><jsp:getProperty name="bibliographicReference" property="authorList"/></TD>
			</TR>
			<TR>
				<TD><B>Title</B></TD>
				<TD><jsp:getProperty name="bibliographicReference" property="title"/></TD>
			</TR>
			<TR>
				<TD><B>Volume</B></TD>
				<TD><jsp:getProperty name="bibliographicReference" property="volume"/></TD>
			</TR>
			<TR>
				<TD><B>Issue</B></TD>
				<TD><jsp:getProperty name="bibliographicReference" property="issue"/></TD>
			</TR>
			<TR>
				<TD><B>Publication</B></TD>
				<TD><jsp:getProperty name="bibliographicReference" property="publication"/></TD>
			</TR>
			<TR>
				<TD><B>Abstract Text</B></TD>
				<TD><jsp:getProperty name="bibliographicReference" property="abstractText"/></TD>
			</TR>
			<TR>
				<TD><B>Publication Date</B></TD>
				<TD><jsp:getProperty name="bibliographicReference" property="publicationDate"/></TD>
			</TR>

			<TR>
				<TD COLSPAN="2"><HR></TD>
			</TR>
			<TR>
				<TD COLSPAN="2">
					<DIV align="right">
						<INPUT type="button" onclick="javascript:document.backForm.submit()" value="Back">
					</DIV>
				</TD>
			</TR>
		</TABLE>
	</BODY>
</HTML>
