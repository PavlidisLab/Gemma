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
			document.backForm.action="bibRefs.htm"
		}
		if(target == 1){
			document.backForm._eventId.value="edit"
			document.backForm._flowId.value="pubMed.Edit"
			document.backForm.action="search.htm"
		}
		document.backForm.submit();
	}
	</SCRIPT> 
	<HEAD></HEAD>
	<BODY>
		<FORM name="backForm" action="">
				<!-- <INPUT type="hidden" name="_flowExecutionId" value="<%=request.getAttribute("flowExecutionId") %>">-->
				<INPUT type="hidden" name="_eventId" value="back">
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
						<INPUT type="button" onclick="javascript:selectButton(0)" value="Back">
					</DIV>
				</TD>
				<%--<r:isUserInRole role="admin">--%>
				<%--<authz:authorize ifAnyGranted="admin">--%>
				<%--<authz:acl domainObject="${arrayDesign}" hasPermission="1,6">--%>
				<TD COLSPAN="2">
					<DIV align="right">
						<INPUT type="button" onclick="javascript:selectButton(1)" value="Edit">
					</DIV>
				</TD>
				<%--</authz:acl>--%>
				<%--</authz:authorize>--%>
				<%--</r:isUserInRole> --%>
			</TR>
		</TABLE>
	</BODY>
</HTML>
