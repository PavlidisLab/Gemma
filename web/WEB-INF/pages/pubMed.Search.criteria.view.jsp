<%@ include file="/common/taglibs.jsp"%>

<jsp:useBean id="bibliographicReference" scope="request"
    class="edu.columbia.gemma.common.description.BibliographicReferenceImpl" />

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<HTML>


<HEAD>
<SCRIPT LANGUAGE="JavaScript">
    function selectButton(target){
        if(target == 0){
            document.searchForm._eventId.value="searchPubMed"
            document.searchForm._flowId.value="pubMed.Search" 
            if (document.searchForm.pubMedId.value==""){
                alert("Enter a valid PubMed ID.");
                return false;
            }
            else{
                document.searchForm.submit();
            }
        }
    }
    </SCRIPT>
</HEAD>
<BODY>

<FORM name="searchForm" action="flowController.htm"><INPUT type="hidden"
    name="_flowExecutionId"
    value="<%=request.getAttribute("flowExecutionId") %>"> <INPUT
    type="hidden" name="_currentStateId" value="criteria.view"> <INPUT
    type="hidden" name="_eventId" value=""> <INPUT type="hidden"
    name="_flowId" value="">



<h2>Search NCBI PubMed for a reference</h2>
<spring:hasBindErrors name="bibliographicReference">
    <div class="error">There were the following error(s) with your
    submission:
    <ul>
        <c:forEach var="errMsgObj" items="${errors.allErrors}">
            <li><spring:message code="${errMsgObj.code}"
                text="${errMsgObj.defaultMessage}" /></li>
        </c:forEach>
    </ul>
    </div>
</spring:hasBindErrors>

<table>
    <TR>
        <TD>PubMed ID</TD>
        <TD><INPUT type="text" name="pubMedId"></TD>

        <TD align="left">
        <DIV align="right"><INPUT type="button"
            onclick="javascript:selectButton(0)" value="Search"></DIV>
        </TD>
    </TR>
    <TR>
        <TD colspan="3">
        <HR />
        </td>
    <tr>
    <tr>
        <td colspan="2">
        <DIV align="left"><b>View All Gemma Bibliographic References</b></DIV>
        </TD>

        <TD>
        <DIV align="left"><INPUT type="button"
            onclick="location.href='bibRefs.htm'" value="Select"></DIV>
    </TR>

</table>
</body>
</html>
