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
        <%-- Uncomment to use webflow implementation of "getting all"
        if(target == 1){
            document.searchForm._eventId.value="getBibRef"
            document.searchForm._flowId.value="bibRef.GetAll" 
            document.searchForm.submit();
        }
        --%>
    }
    </SCRIPT>
</HEAD>
<BODY>
<DIV align="left">
<FORM name="searchForm" action="flowController.htm"><INPUT type="hidden"
    name="_flowExecutionId"
    value="<%=request.getAttribute("flowExecutionId") %>"> <INPUT
    type="hidden" name="_currentStateId" value="criteria.view"> <INPUT
    type="hidden" name="_eventId" value=""> <INPUT type="hidden"
    name="_flowId" value="">
<TABLE width="40%">
    <TR>
        <TD>
        <DIV align="left"><b>Search PubMed for a reference</b></DIV>
        </TD>
    </TR>
    <TR>
        <TD COLSPAN="2">
        <HR>
        </TD>
    </TR>
    <spring:hasBindErrors name="bibliographicReference">
        <TR>
            <TD COLSPAN="2">
            <div class="error">There were the following error(s) with
            your submission:
            <ul>
                <c:forEach var="errMsgObj" items="${errors.allErrors}">
                    <li><spring:message code="${errMsgObj.code}"
                        text="${errMsgObj.defaultMessage}" /></li>
                </c:forEach>
            </ul>
            </div>
            </TD>
        </TR>
    </spring:hasBindErrors>
    <TR>
        <TD>PubMed ID</TD>
        <TD><INPUT type="text" name="pubMedId" value="15699352"></TD>
    </TR>
    <TR>
        <TD COLSPAN="2">
        <HR>
        </TD>
    </TR>
    <TR>
        <TD COLSPAN="2" align="left">
        <DIV align="right"><INPUT type="button"
            onclick="javascript:selectButton(0)" value="Search"></DIV>
        </TD>
    </TR>


    <TR>
        <TD>
        <DIV align="left"><b>View All Gemma Bibliographic References</b></DIV>
        </TD>
    </TR>
    <TR>
        <TD COLSPAN="2">
        <HR>
        </TD>
    </TR>
    <TR>
        <TD COLSPAN="2">
        <DIV align="left"><INPUT type="button"
            onclick="location.href='bibRefs.htm'" value="Select"></DIV>
    </TR>
    <TR>
        <TD COLSPAN="2">
        <HR>
        </TD>
    </TR>


</TABLE>
</DIV>
</BODY>
</HTML>
