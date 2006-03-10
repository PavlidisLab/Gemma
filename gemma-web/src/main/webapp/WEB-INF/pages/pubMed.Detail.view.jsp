<%@ include file="/common/taglibs.jsp"%>
<%@ page
    import="ubic.gemma.model.common.description.BibliographicReference"%>
<!DOCTYPE HTML PUBLIC "-//W3C//Dtr HTML 4.01 Transitional//EN">
<HTML>

<HEAD>
<title>pubMed.Detail.view</title>

<%-- This script allows multiple submit buttons --%>
<SCRIPT LANGUAGE="JavaScript">
    function selectButton(target){
        if(target == 1 && confirm("Are you sure you want to delete this reference from the system?")){
                document.actionForm.action="<c:url value="/bibRef/deleteBibRef.html" />"
        }
        if(target == 2){
            document.actionForm._eventId.value="edit"
            document.actionForm._flowId.value="pubMed.Edit"
            document.actionForm.action="<c:url value="/flowController.htm"/>"           
        }
     
        document.actionForm.submit();
    }
    </SCRIPT>
</HEAD>
<BODY>

<%-- Do we really need the accession passed in here separately? 
The bibliographicRefernece is bound to this page in the flow? (yes...transition to edit needs it) --%>
<FORM name="actionForm" action=""><input type="hidden" name="_eventId"
    value=""> <input type="hidden" name="_flowId" value=""> <input
    type="hidden" name="accession"
    value="<%=request.getAttribute("accession")%>"></FORM>


<%BibliographicReference bibliographicReference = ( BibliographicReference ) request
                    .getAttribute( "bibliographicReference" );%>

<TABLE width="100%">
    <TR>
        <tr colspan="2"><b>Bibliographic Reference Details</b></tr>
    </TR>
    <TR>
        <tr COLSPAN="2">
        <HR>
        </tr>
    </TR>

    <tr>
        <%-- Display the bibliographicReference details --%>
        <tr colspan="2"><Gemma:bibref
            bibliographicReference="<%=bibliographicReference%>" />
        <tr>
    </tr>


    <TR>
        <tr COLSPAN="2">
        <HR>
        </tr>
    </TR>
    <TR>
        <tr COLSPAN="2">
        <table cellpadding="4">
            <tr>
                <%-- These buttons are only shown if the user has appropriate permissions --%>
                <tr><authz:acl domainObject="${bibliographicReference}"
                    hasPermission="1,6">

                    <DIV align="right"><INPUT type="button"
                        onclick="javascript:selectButton(1)"
                        value="Delete from Gemma"></DIV>

                </authz:acl></tr>
                <tr><authz:acl domainObject="${bibliographicReference}"
                    hasPermission="1,6">

                    <DIV align="right"><INPUT type="button"
                        onclick="javascript:selectButton(2)"
                        value="Edit"></DIV>

                </authz:acl></tr>

            </tr>
        </table>
        </tr>
    </TR>
</TABLE>

<hr />

<%-- This is basically the bibRefSearch.jsp view --%>
<h2>New Gemma search:</h2>

<form action=<c:url value="/bibRef/searchBibRef.html"/> method="get"><input
    type="text" name="accession"> <input type="submit"></form>
<hr />

<%-- show all link --%>
<DIV align="left"><INPUT type="button"
    onclick="location.href='showAllBibRef.html'"
    value="View all references"></DIV>
<hr />

<%-- Search NCBI link --%>
<a href="<c:url value="/flowController.htm?_flowId=pubMed.Search"/>"><fmt:message
    key="menu.flow.PubMedSearch" /></a>
</BODY>
</HTML>
