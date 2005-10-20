<%-- $Id$ --%>
<%-- Shows the results of a NCBI search for pubmed references. --%>
<%@ include file="/common/taglibs.jsp"%>
<%@ page
    import="edu.columbia.gemma.common.description.BibliographicReference"%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
<head>
<script language="JavaScript">
    function selectAction(event){
        document.actionForm._eventId.value=event;
        document.actionForm.submit();
    }
</script>
</head>
<body>
 
<form method="post" name="actionForm" action="flowController.htm"><INPUT
    type="hidden" name="_flowExecutionId"
    value="<%=request.getAttribute("flowExecutionId") %>"> <INPUT
    type="hidden" name="_eventId" value=""> <INPUT type="hidden"
    name="pubMedId" value="<%=request.getAttribute("pubMedId") %>"></FORM>

<TABLE width="100%">
    <TR>
        <TD colspan="2"><b>Bibliographic Reference</b></TD>
    </TR>
    <TR>
        <TD COLSPAN="2">
        <HR>
        </TD>
    </TR>
    <%BibliographicReference bibliographicReference = ( BibliographicReference ) request
                    .getAttribute( "bibliographicReference" );

            %>
    <TR>
        <TD><B>Pubmed ID</B></TD>
        <TD><%=bibliographicReference.getPubAccession().getAccession() %></TD>
    </TR>
    <TR>
        <TD><B>Authors</B></TD>
        <TD><%=bibliographicReference.getAuthorList() %></TD>
    </TR>
    <TR>
        <TD><B>Year</B></TD>
        <TD><Gemma:date
            date="<%=bibliographicReference.getPublicationDate()%>" /></TD>
    </TR>

    <TR>
        <TD><B>Title</B></TD>
        <TD><%=bibliographicReference.getTitle() %></TD>
    </TR>
    <TR>
        <TD><B>Publication</B></TD>
        <TD><%=bibliographicReference.getPublication() %></TD>
    </TR>
    <TR>
        <TD><B>Volume</B></TD>
        <TD><%=bibliographicReference.getVolume() %></TD>
    </TR>
    <TR>
        <TD><B>Pages</B></TD>
        <TD><%=bibliographicReference.getPages() %></TD>
    </TR>
    <TR>
        <TD><B>Abstract Text</B></TD>
        <TD><%=bibliographicReference.getAbstractText() %></TD>
    </TR>



    <TR>
        <TD COLSPAN="2">
        <HR>
        </TD>
    </TR>
    <TR>

        <td colspan="2">
        <table>
            <tr>
                <TD align="left"><c:if
                    test="${!requestScope.existsInSystem}">
                    <DIV align="left"><INPUT type="button"
                        onclick="javascript:selectAction('saveBibRef')"
                        value="Add to Gemma Database"></DIV>
                </c:if></TD>


                <TD align="right"><c:if
                    test="${requestScope.existsInSystem}">
                    <DIV align="right"><INPUT type="button"
                        onclick="javascript:selectAction('viewRecord')"
                        value="View Gemma Record"></DIV>
                </c:if></TD>


                <TD align="right">
                <DIV align="right"><INPUT type="button"
                    onclick="javascript:selectAction('newSearch')"
                    value="New Search"></DIV>
                </TD>
            </tr>
        </table>
        </td>
    </TR>
</table>
</body>
</html>
