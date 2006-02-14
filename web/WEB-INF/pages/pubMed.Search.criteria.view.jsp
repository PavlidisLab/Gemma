<%@ include file="/common/taglibs.jsp"%>

<!DOCTYPE HTML PUBLIC "-//W3C//Dtd HTML 4.01 Transitional//EN">
<HTML>
<HEAD>
<title>NCBI PubMed search</title>
</HEAD>
<BODY>

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
        <td>PubMed ID</td>
        <td>
        <form name="searchForm" action="flowController.html"><spring:bind
            path="bibliographicReference.pubAccession.accession">
            <input type="text" name="${status.expression}"
                value="${status.value}">
        </spring:bind> <input type="hidden" name="_flowExecutionId"
            value="<%=request.getAttribute("flowExecutionId") %>"> <input
            type="hidden" name="_currentStateId" value="criteria.view">
        <input type="hidden" name="_eventId" value="searchPubMed"> <input
            type="hidden" name="_flowId" value=""><input type="submit"
            value="Search"></form>
        </td>

        <td align="left">
        <DIV align="right"></DIV>
        </td>
    </TR>
    <TR>
        <td colspan="3">
        <HR />
        </td>
    <tr>
    <tr>
        <td colspan="2">
        <DIV align="left"><b>View All Gemma Bibliographic References</b></DIV>
        </td>

        <td>
        <DIV align="left"><input type="button"
            onclick="location.href='bibRef/showAllBibRef.html'"
            value="Select"></DIV>
        </td>
    </TR>

</table>
</body>
</html>
