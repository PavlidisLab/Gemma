<%@ include file="/common/taglibs.jsp"%>

<%-- 
The purpose of this page is to prevent reloads from re-running a completed action.
--%>

<c:if test="${applicationScope.secureLogin == 'true'}">
    <Gemma:secure/>
</c:if>

<c:redirect url="/candidateGeneList.htm"/>