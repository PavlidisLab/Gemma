<%@ include file="/common/taglibs.jsp"%>
<%@ page import="java.util.*"%>
<%@ page import="edu.columbia.gemma.expression.arrayDesign.ArrayDesign"%>

<jsp:useBean id="arrayDesign" scope="request"
    class="edu.columbia.gemma.expression.arrayDesign.ArrayDesignImpl" />

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<HTML>
<SCRIPT LANGUAGE="JavaScript">
	function selectButton(target){
		if(target == 0){
			document.backForm._eventId.value="back"
			document.backForm.action="arrayDesigns.htm"
		}
		if(target == 1){
			document.backForm._eventId.value="edit"
			document.backForm._flowId.value="arrayDesign.Edit"
			document.backForm.action="flowController.htm"
		}
		document.backForm.submit();
	}
	</SCRIPT>
<HEAD></HEAD>
<BODY>

<FORM name="backForm" action=""><%--<input type="hidden" name="_flowExecutionId" value="<%=request.getAttribute("flowExecutionId") %>">--%>
<input type="hidden" name="_eventId" value=""> <input type="hidden"
    name="_flowId" value=""> <input type="hidden" name="name"
    value="<%=request.getAttribute("name") %>"></FORM>

<TABLE width="100%">
    <TR>
        <TD><b>Array Design Details</b></TD>
    </TR>
    <TR>
        <TD COLSPAN="2">
        <HR>
        </TD>
    </TR>
    <TR>
        <TD><B>Name</B></TD>
        <TD><jsp:getProperty name="arrayDesign" property="name" /></TD>
    </TR>
    <TR>
        <TD><B>Provider</B></TD>
        <TD><%

        if ( arrayDesign.getDesignProvider() != null ) {
            %><%=arrayDesign.getDesignProvider().getName()%><%} else {%>(Not
        listed)<%}%></TD>
    </TR>
    <TR>
        <TD><B>Number Of Features (according to provider)</B></TD>
        <TD><jsp:getProperty name="arrayDesign"
            property="advertisedNumberOfDesignElements" /></TD>
    </TR>
    <TR>
        <TD><B>Description</B></TD>
        <TD><%

        if ( arrayDesign.getDescription() != null && arrayDesign.getDescription().length() > 0 ) {
        %><jsp:getProperty
            name="arrayDesign" property="description" /><%} else {%>(None
        provided)<%}%></TD>
    </TR>

    <%-- FIXME - show some of the design elements --%>

 
    <TR>
        <TD COLSPAN="2">
        <HR>
        </TD>
    </TR>
    <TR>
        <TD COLSPAN="2">
        <DIV align="right"><INPUT type="button"
            onclick="javascript:selectButton(0)" value="Back"></DIV>
        </TD>
        <%--<r:isUserInRole role="admin">--%>
        <%--<authz:authorize ifAnyGranted="admin">--%>
        <authz:acl domainObject="${arrayDesign}" hasPermission="1,6">
            <TD COLSPAN="2">
            <DIV align="right"><INPUT type="button"
                onclick="javascript:selectButton(1)" value="Edit"></DIV>
            </TD>
        </authz:acl>
        <%--</authz:authorize>--%>
        <%--</r:isUserInRole> --%>
    </TR>
</TABLE>
</BODY>
</HTML>
