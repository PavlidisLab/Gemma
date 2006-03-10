<%-- $Id$ --%>
<%-- Shows the results of a NCBI search for pubmed references. --%>
<%@ include file="/common/taglibs.jsp"%>
<%@ page
    import="ubic.gemma.model.common.description.BibliographicReference"%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
<head>
<title>pubMed.Search.results.view</title>
<script language="JavaScript">
    function selectAction( event){
        document.actionForm._eventId.value=event;
        document.actionForm.submit();
    }
</script>
</head>
<body>

<%-- This form is very similar to the pubMed.Detail.view.jsp --%>
<form method="post" name="actionForm" action="flowController.htm"><INPUT
    type="hidden" name="_flowExecutionId"
    value="<%=request.getAttribute("flowExecutionId") %>"> <INPUT
    type="hidden" name="_eventId" value=""></FORM>


<%BibliographicReference bibliographicReference = ( BibliographicReference ) request
                    .getAttribute( "bibliographicReference" );

            %>
<TABLE width="100%">
    <tr>
        <td colspan="2"><b>Bibliographic Reference Search Results</b></td>
    </tr>
    <tr>
        <td COLSPAN="2">
        <HR>
        </td>
    </tr>
    <tr>
        <td colspan="2"><Gemma:bibref
            bibliographicReference="<%=bibliographicReference %>" />
        <td>
    </tr>
    <tr>
        <td COLSPAN="2">
        <HR>
        </td>
    </tr>
    <tr>

        <td colspan="2">
        <table>
            <tr>
                <td align="left"><c:if
                    test="${!requestScope.existsInSystem}">
                    <DIV align="left"><INPUT type="button"
                        onclick="javascript:selectAction(  'saveBibRef')"
                        value="Add to Gemma Database"></DIV>
                </c:if></td>
                <TD><c:if test="${requestScope.existsInSystem}">
                    <authz:acl domainObject="${bibliographicReference}"
                        hasPermission="1,6">
                        <DIV align="right"><INPUT type="button"
                            onclick="javascript:selectAction(  'delete')"
                            value="Delete from Gemma"></DIV>
                    </authz:acl>
                </c:if></td>

                <TD><c:if test="${requestScope.existsInSystem}">
                    <authz:acl domainObject="${bibliographicReference}"
                        hasPermission="1,6">

                        <DIV align="right"><INPUT type="button"
                            onclick="javascript:selectAction( 'edit')"
                            value="Edit"></DIV>

                    </authz:acl>
                </c:if></td>
                <td align="right">
                <DIV align="right"><INPUT type="button"
                    onclick="javascript:selectAction('newSearch')"
                    value="New NCBI Search"></DIV>
                </td>
            </tr>
        </table>
        </td>
    </tr>
</table>
</body>
</html>
