<%@ include file="/common/taglibs.jsp"%>
<%@ page import="java.util.*"%>
<%@ page import="edu.columbia.gemma.expression.arrayDesign.ArrayDesign"%>

<jsp:useBean id="arrayDesign" scope="request"
    class="edu.columbia.gemma.expression.arrayDesign.ArrayDesignImpl" />

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<HTML>

<HEAD></HEAD>
<BODY>

<FORM name="arrayDesignDetail" action=""><input type="hidden" name="name"
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
            <DIV align="left"><input type="button"
            onclick="location.href='showAllArrayDesigns.html'"
            value="Back"></DIV>
            </TD>
        <%--<r:isUserInRole role="admin">--%>
        <%--<authz:authorize ifAnyGranted="admin">--%>
        <authz:acl domainObject="${arrayDesign}" hasPermission="1,6">
            <TD COLSPAN="2">    
            <DIV align="left"><input type="button"
            onclick="location.href='editArrayDesign.html?name=<%=request.getAttribute("name")%>'"
            value="Edit"></DIV>
            </TD>
        </authz:acl>
        <%--</authz:authorize>--%>
        <%--</r:isUserInRole> --%>
    </TR>
</TABLE>
</BODY>
</HTML>
