<%@ include file="/common/taglibs.jsp"%>
<%@ page import="java.util.*" %>
<%@ page import="edu.columbia.gemma.expression.experiment.ExpressionExperiment" %>

<jsp:useBean id="arrayDesign" scope="request" class="edu.columbia.gemma.expression.experiment.ExpressionExperimentImpl"/>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<HTML>
<SCRIPT LANGUAGE = "JavaScript">
	function selectButton(target){
		if(target == 0){
			document.detailsForm._eventId.value="back"
			document.detailsForm.action="expressionExperiments.htm"
		}
		if(target == 1){
		<%-- confirm deletion of item --%>
			if (confirm("Are you sure you want to delete this?")){
				document.detailsForm._eventId.value="delete"
				document.detailsForm.action="expressionExperimentDetails.htm"
				<%--
				Not yet implemented for webflows
				document.backForm._flowId.value="pubMed.Delete" 
				document.backForm.action="flowController.htm"
				--%>
				}
		}
		if(target == 2){
			document.detailsForm._eventId.value="edit"
			document.detailsForm._flowId.value="expressionExperiment.Edit"
			document.detailsForm.action="flowController.htm"
		}
		document.detailsForm.submit();
	}
	</SCRIPT> 
	<HEAD></HEAD>
	<BODY>
		<FORM name="detailsForm" action="">
				<%--<input type="hidden" name="_flowExecutionId" value="<%=request.getAttribute("flowExecutionId") %>">--%>
				<input type="hidden" name="_eventId" value="">
				<input type="hidden" name="_flowId" value="">
				<input type="hidden" name="name" value="<%=request.getAttribute("name") %>">
		</FORM>
		<TABLE width="100%">
			<TR>
				<TD><b>ExpressionExperiment Details</b></TD>
			</TR>
			<TR>
				<TD COLSPAN="2"><HR></TD>
			</TR>
			<TR>
				<TD><B>Name</B></TD>
				<TD><jsp:getProperty name="expressionExperiment" property="name"/></TD>
			</TR>
			<%--
			<TR>
				<TD><B>Manufacturer</B></TD>
				<TD><%=arrayDesign.getDesignProvider().getName()%></TD>
			</TR>
			--%>
			<TR>
				<TD><B>Source</B></TD>
				<TD><jsp:getProperty name="expressionExperiment" property="source"/></TD>
			</TR>		
			<TR>
				<TD><B>Database Entry</B></TD>
				<%--<TD><%=expressionExperiment.getAccession().getAccession()%></TD>--%>
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
				
				<%--<authz:acl domainObject="${bibliographicReference}" hasPermission="1,6">--%>
				<TD COLSPAN="2">
					<DIV align="right">
						<INPUT type="button" onclick="javascript:selectButton(1)" value="Delete">
					</DIV>
				</TD>
				<%--</authz:acl>--%>
				
				<%--<authz:acl domainObject="${bibliographicReference}" hasPermission="1,6">--%>
				<TD COLSPAN="2">
					<DIV align="right">
						<INPUT type="button" onclick="javascript:selectButton(2)" value="Edit">
					</DIV>
				</TD>
				<%--</authz:acl>--%>
			</TR>
		</TABLE>
	</BODY>
</HTML>
