<%@ include file="/common/taglibs.jsp"%>

<p>
<content tag="heading">
<fmt:message key="menu.compassSearcher"/>
</content>

<form method="GET">
	<spring:bind path="command.query">
	 <input type="text" size="20" name="query" value="<c:out value="${status.value}"/>" />
	</spring:bind>
  <input type = "submit" value="Search"/>
</form>

<c:if test="${! empty searchResults}">

  Search took <c:out value="${searchResults.searchTime}" />ms

  <c:forEach var="hit" items="${searchResults.hits}">
   <c:choose>
     <c:when test="${hit.alias == 'expressionExperiment'}">
       <P>
         <a href="<c:url value="/expressionExperiment/editExpressionExperiment.html?id=${hit.data.id}"/>">
           <c:out value="${hit.data.name}" /> (ExpressionExperiment)
         </a>
     </c:when>
     
     <c:when test="${hit.alias == 'bioAssay'}">
       <P>
         <a href="<c:url value="/bioAssay/editBioAssay.html?id=${hit.data.id}"/>">
           <c:out value="${hit.data.name}" /> (BioAssay)
         </a>
     </c:when>

     <c:when test="${hit.alias == 'arrayDesign'}">
       <P>
         <a href="<c:url value="/arrayDesign/editArrayDesign.html?id=${hit.data.id}"/>">
           <c:out value="${hit.data.name}" /> (ArrayDesign)
         </a>
     </c:when>

     <c:when test="${hit.alias == 'contact'}">
       <p>
         <a href="#">
           <c:out value="${hit.data.name}" />
         </a>
         <br>
         Address: <c:out value="${hit.data.address}" /><br>
         Phone: <c:out value="${hit.data.phone}" /><br>
         Email: <c:out value="${hit.data.email}" /><br>
     </c:when>

   </c:choose>

  </c:forEach>
<%--
  <c:if test="${! empty searchResults.pages}">

    <br><br><br>
    <table><tr>
    <c:forEach var="page" items="${searchResults.pages}" varStatus="pagesStatus">
      <td>
      <c:choose>
        <c:when test="${page.selected}">
          <c:out value="${page.from}" />-<c:out value="${page.to}" />
        </c:when>
        <c:otherwise>
          <form method="GET">
            <spring:bind path="command.query">
               <input type="hidden" name="query" value="<c:out value="${status.value}"/>" />
            </spring:bind>
            <spring:bind path="command.page">
               <input type="hidden" name="page" value="<c:out value="${pagesStatus.index}"/>" />
            </spring:bind>
            <input type = "submit" value="<c:out value="${page.from}" />-<c:out value="${page.to}" />"/>
          </form>
        </c:otherwise>
      </c:choose>
      </td>
    </c:forEach>
    </tr></table>

  </c:if>
--%>
</c:if>

<p>
<br>
