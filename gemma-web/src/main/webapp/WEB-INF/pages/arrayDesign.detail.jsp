<%@ include file="/common/taglibs.jsp"%>
<%@ page import="java.util.*"%>
<%@ page import="ubic.gemma.model.expression.arrayDesign.ArrayDesignImpl" %>
<jsp:useBean id="arrayDesign" scope="request" class="ubic.gemma.model.expression.arrayDesign.ArrayDesignImpl" />

<!DOCTYPE html PUBLIC "-//W3C//Dtd html 4.01 transitional//EN">
<html>

    <head></head>
    <BODY>

        <FORM name="arrayDesignDetail" action="">
            <input type="hidden" name="name" value="<%=request.getAttribute("name") %>">
        </FORM>

        <TABLE width="100%">
            <tr>
                <td>
                    <b>Array Design Details</b>
                </td>
            </tr>
            <tr>
                <td COLSPAN="2">
                    <HR>
                </td>
            </tr>
            <tr>
                <td>
                    <B>Name</B>
                </td>
                <td>
                    <jsp:getProperty name="arrayDesign" property="name" />
                </td>
            </tr>
            <tr>
                <td>
                    <B>Provider</B>
                </td>
                <td>
                    <%
						if ( arrayDesign.getDesignProvider() != null ) {

                	%>
                    <%=arrayDesign.getDesignProvider().getName()%>
                    <%} else {%>
                    (Not listed)
                    <%}%>
                </td>
            </tr>
            <tr>
                <td>
                    <B>Number Of Features (according to provider)</B>
                </td>
                <td>
                    <jsp:getProperty name="arrayDesign" property="advertisedNumberOfDesignElements" />
                </td>
            </tr>
            <tr>
                <td>
                    <B>Description</B>
                </td>
                <td>
                    <%
						if ( arrayDesign.getDescription() != null && arrayDesign.getDescription().length() > 0 ) {

            		%>
                    <textarea name="" rows=5 cols=60><jsp:getProperty name="arrayDesign" property="description" /></textarea>
                    <%} else {%>
                    (None provided)
                    <%}%>
                </td>
            </tr>

            <%-- FIXME - show some of the design elements --%>


            <tr>
                <td COLSPAN="2">
                    <HR>
                </td>
            </tr>
            <tr>
                <td COLSPAN="2">
                    <div align="left">
                        <input type="button" onclick="location.href='showAllArrayDesigns.html'" value="Back">
                    </div>
                </td>
                <%--<r:isUserInRole role="admin">--%>
                <%--<authz:authorize ifAnyGranted="admin">--%>
                <authz:acl domainObject="${arrayDesign}" hasPermission="1,6">
                    <td COLSPAN="2">
                        <div align="left">
                            <input type="button"
                                onclick="location.href='/Gemma/arrayDesign/editArrayDesign.html?name=<%=request.getAttribute("name")%>'"
                                value="Edit">
                        </div>
                    </td>
                </authz:acl>
                <%--</authz:authorize>--%>
                <%--</r:isUserInRole> --%>
            </tr>
        </TABLE>
    </BODY>
</html>
