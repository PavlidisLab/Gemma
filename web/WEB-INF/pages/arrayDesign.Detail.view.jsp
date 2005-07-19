<%@ include file="/common/taglibs.jsp"%>
<%@ page import="java.util.*" %>
<%@ page import="edu.columbia.gemma.expression.arrayDesign.ArrayDesign" %>

<jsp:useBean id="arrayDesign" scope="request" class="edu.columbia.gemma.expression.arrayDesign.ArrayDesignImpl"/>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<HTML>
<SCRIPT LANGUAGE = "JavaScript">
	function selectButton(target){
		if(target == 0){
			document.backForm._eventId.value="back"
		}
		if(target == 1){
			document.backForm._eventId.value="edit"
		}
		document.backForm.submit();
	}
	</SCRIPT> 
	<HEAD></HEAD>
	<BODY>
		<FORM name="backForm" action="">
				<input type="hidden" name="_flowExecutionId" value="<%=request.getAttribute("flowExecutionId") %>">
				<input type="hidden" name="_eventId" value="">
				<input type="hidden" name="name" value="<%=request.getAttribute("name") %>">
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
						<INPUT type="button" onclick="javascript:selectButton(0)" value="Back">
					</DIV>
				</TD>
				<TD COLSPAN="2">
					<DIV align="right">
						<INPUT type="button" onclick="javascript:selectButton(1)" value="Edit">
					</DIV>
				</TD>
			</TR>
		</TABLE>
	</BODY>
</HTML>
