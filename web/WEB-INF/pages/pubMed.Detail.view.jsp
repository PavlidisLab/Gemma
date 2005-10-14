<%@ include file="/common/taglibs.jsp"%>
<%@ page import="java.util.*" %>
<%@ page import="edu.columbia.gemma.common.description.BibliographicReference" %>

<jsp:useBean id="bibliographicReference" scope="request" class="edu.columbia.gemma.common.description.BibliographicReferenceImpl"/>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<HTML>
<SCRIPT LANGUAGE = "JavaScript">
	function selectButton(target){
		if(target == 0){
			document.backForm._eventId.value="back" 
			<%-- uncomment when using webflows
			document.backForm.action="flowController.htm" --%>
			document.backForm.action="bibRefs.htm"
		}
		if(target == 1){
		<%-- confirm deletion of item --%>
			if (confirm("Are you sure you want to delete this reference from the system?")){
				document.backForm._eventId.value="delete"
				document.backForm.action="bibRefDetails.htm"
				<%--
				Not yet implemented for webflows
				document.backForm._flowId.value="pubMed.Delete" 
				document.backForm.action="flowController.htm"
				--%>
				}
		}
		if(target == 2){
		<%-- enter the flow --%>
			document.backForm._eventId.value="edit"
			document.backForm._flowId.value="pubMed.Edit"
			document.backForm.action="flowController.htm"			
		}
		document.backForm.submit();
	}
	</SCRIPT> 
	<HEAD></HEAD>
	<BODY>
		<FORM name="backForm" action="">
				<%-- uncomment when using webflow implementation
				<input type="hidden" name="_flowExecutionId" value="<%=request.getAttribute("flowExecutionId") %>">--%>
				<input type="hidden" name="_eventId" value="">
				<input type="hidden" name="_flowId" value="">
				<input type="hidden" name="pubMedId" value="<%=request.getAttribute("pubMedId") %>">
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
                <TD><B>Publication Date</B></TD>
                <TD><jsp:getProperty name="bibliographicReference" property="publicationDate"/></TD>
            </TR>
			<TR>
				<TD><B>Publication</B></TD>
				<TD><jsp:getProperty name="bibliographicReference" property="publication"/></TD>
			</TR>
			<TR>
				<TD><B>Abstract</B></TD>
				<TD><jsp:getProperty name="bibliographicReference" property="abstractText"/></TD>
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
				
				<authz:acl domainObject="${bibliographicReference}" hasPermission="1,6">
				<TD COLSPAN="2">
					<DIV align="right">
						<INPUT type="button" onclick="javascript:selectButton(1)" value="Delete">
					</DIV>
				</TD>
				</authz:acl>
				
				<authz:acl domainObject="${bibliographicReference}" hasPermission="1,6">
				<TD COLSPAN="2">
					<DIV align="right">
						<INPUT type="button" onclick="javascript:selectButton(2)" value="Edit">
					</DIV>
				</TD>
				</authz:acl>
			</TR>
		</TABLE>
	</BODY>
</HTML>
