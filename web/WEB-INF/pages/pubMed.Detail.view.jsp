<%@ include file="/common/taglibs.jsp"%>
<%@ page import="java.util.*"%>
<%@ page
    import="edu.columbia.gemma.common.description.BibliographibibliographicReferenceeBean id="bibliographicRefereedu.columbia.gemma.common.description.BibliographicReferenceImplption.BibliographicReferenceImpl" />

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<HTML>

<HEAD>
<SCRIPT LANGUAGE="JavaScript">
    function selectButton(target){
        if(target == 1 && confirm("Are you sure you want to delete this reference from the system?")){
                document.backForm._flowId.value="pubMed.Edit" 
                document.backForm._eventId.value="delete"
                document.backForm.action="<c:url value="/bibRef/deleteBibRef.html" />"
        }
        if(target == 2){
            document.backForm._eventId.value="edit"
            document.backForm._flowId.value="pubMed.Edit"
            document.backForm.action="<c:url value="/flowController.htm"/>"           
        }
        
        document.backForm.submit();
    }
    </SCRIPT>
</HEAD>
<BODY>

<FORM name="backForm" action=""><input type="hidden" name="_eventId"
    value=""> <input type="hidden" name="_flowId" value=""> <input
    type="hidden" name="bibliographicReference"
    value="<%=request.getAttribute("pubMedId")%>"></FORM>

<TABLE width="100%">
    <TR>
        <TD colspan="2"><b>Bibliographic Reference Details</b></TD>
    </TR>
    <TR>
        <TD COLSPAN="2">
        <HR>
        </TD>
    </TR>
    <TR>
        <TD><B>Authors</B></TD>
        <TD><jsp:getProperty name="bibliographicReference"
 bibliographicReference.getPubAccession().getAccession() TR>
        <TD><B>Title</B></TD>
        <TD><jsp:getProperty name="biblibibliographicReference.getAuthorList() ="title" /></TD>
    </TR>
    <TR>
        <TD><B>Volume</B></TD>
        <TD><jsp:getProperty name="bibliographicReference"
            property="volume" /></TD>
    </TR>
    <TR>
        <TD><B>Issue</B></TD>
       bibliographicReference.getTitle() graphicReference"
            property="issue" /></TD>
    </TR>
    <TR>
    bibliographicReference.getPublication()      <%-- FIXME: Format this into just a year --%>
        <TD><jsp:getPrbibliographicReference.getVolume() "
            property="publicationDate" /></TD>

    </TR>
    <TR>
   bibliographicReference.getPages()        <TD><jsp:getProperty name="bibliographicReference"
            property="bibliographicReference.getAbstractText()        <TD><B>Abstract</B></TD>
        <TD><jsp:getProperty name="bibliographicReference"
            property="abstractText" /></TD>
    </TR>


    <TR>
        <TD COLSPAN="2">
        <HR>
        </TD>
    </TR>
    <TR>
        <TD COLSPAN="2">
        <table cellpadding="4">
            <tr>
                <TD><authz:acl domainObject="${bibliographicReference}"
                    hasPermission="1,6">

                    <DIV align="right"><INPUT type="button"
                        onclick="javascript:selectButton(1)"
                        value="Delete from Gemma"></DIV>

                </authz:acl></TD>
                <TD><authz:acl domainObject="${bibliographicReference}"
                    hasPermission="1,6">

                    <DIV align="right"><INPUT type="button"
                        onclick="javascript:selectButton(2)"
                        value="Edit"></DIV>

                </authz:acl></td>
            </tr>
        </table>
        </td>
    </TR>
</TABLE>
</BODY>
</HTML>
