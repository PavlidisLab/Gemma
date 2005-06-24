<%@ include file="/common/taglibs.jsp"%>
<%@ page import="java.util.*" %>
<%@ page import="edu.columbia.gemma.common.description.BibliographicReference" %>

<jsp:useBean id="bibliographicReference" scope="request" class="edu.columbia.gemma.common.description.BibliographicReferenceImpl"/>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<HTML>
	<HEAD></HEAD>
	<BODY>
		<FORM name="backForm" action="arrayDesignDetail.htm">
				<INPUT type="hidden" name="_flowExecutionId" value="<%=request.getAttribute("flowExecutionId") %>">
				<INPUT type="hidden" name="_eventId" value="back">
		</FORM>
		<TABLE width="100%">
			<TR>
				<TD><b>ArrayDesign Details</b></TD>
			</TR>
			<TR>
				<TD COLSPAN="2"><HR></TD>
			</TR>
			<TR>
				<TD><B>Name</B></TD>
				<TD><jsp:getProperty name="arrayDesign" property="name"/></TD>
			</TR>
			<TR>
				<TD><B>Description</B></TD>
				<TD><jsp:getProperty name="arrayDesign" property="description"/></TD>
			</TR>
			<TR>
				<TD><B>Number Of Features</B></TD>
				<TD><jsp:getProperty name="arrayDesign" property="numberOfFeatures"/></TD>
			</TR>
			<TR>
				<TD><B>Number Of Composite Sequences</B></TD>
				<TD><jsp:getProperty name="arrayDesign" property="numberOfCompositeSequences"/></TD>
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
