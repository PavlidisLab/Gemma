<%@ include file="/common/taglibs.jsp"%>

<P>
<content tag="heading">
<fmt:message key="menu.compassSearcher"/>
</content>

<FORM method="GET">
	<spring:bind path="command.query">
	 <INPUT type="text" size="20" name="query" value="<c:out value="${status.value}"/>" />
	</spring:bind>
  <INPUT type = "submit" value="Search"/>
</FORM>
<%--
<c:if test="${! empty searchResults}">

  Search took <c:out value="${searchResults.searchTime}" />ms

  <c:forEach var="hit" items="${searchResults.hits}">
   <c:choose>
     <c:when test="${hit.alias == 'pet'}">
       <P>
         <a href="<c:url value="/editPet.htm?petId=${hit.data.id}"/>">
           <c:out value="${hit.data.name}" /> (Pet)
         </a>
         <BR>Birthdate: <fmt:formatDate value="${hit.data.birthDate}" pattern="yyyy-MM-dd"/>, 
         Type: <c:out value="${hit.data.type.name}" />
         <BR>Visits:
          <c:forEach var="visit" items="${hit.data.visits}">
            <c:out value="${visit.date}"/> (<c:out value="${visit.description}"/>), 
          </c:forEach>
          <c:if test="${empty hit.data.visits}">
            none
          </c:if>
     </c:when>

     <c:when test="${hit.alias == 'owner'}">
       <P>
         <a href="<c:url value="/editOwner.htm?ownerId=${hit.data.id}"/>">
           <c:out value="${hit.data.firstName}" /> <c:out value="${hit.data.lastName}" /> (Owner)
         </a>
         <BR>Name: <c:out value="${hit.data.firstName}" /> <c:out value="${hit.data.lastName}" />,
         Address: <c:out value="${hit.data.address}" />, City: <c:out value="${hit.data.city}" />,
         Telephone: <c:out value="${hit.data.telephone}" />
         <BR>Pets:
          <c:forEach var="pet" items="${hit.data.pets}">
            <a href="<c:url value="/editPet.htm?petId=${pet.id}"/>">
                <c:out value="${pet.name}" />
            </a> 
          </c:forEach>
          <c:if test="${empty hit.data.pets}">
            none
          </c:if>
     </c:when>

     <c:when test="${hit.alias == 'vet'}">
       <P>
         <a href="#">
           <c:out value="${hit.data.firstName}" /> <c:out value="${hit.data.lastName}" /> (Vet)
         </a>
         <BR>Name: <c:out value="${hit.data.firstName}" /> <c:out value="${hit.data.lastName}" />,
         Address: <c:out value="${hit.data.address}" />, City: <c:out value="${hit.data.city}" />,
         Telephone: <c:out value="${hit.data.telephone}" />
         <BR>Specialities:
          <c:forEach var="specialty" items="${hit.data.specialties}">
            <c:out value="${specialty.name}"/>, 
          </c:forEach>
          <c:if test="${hit.data.nrOfSpecialties == 0}">
            none
          </c:if>
     </c:when>

     <c:when test="${hit.alias == 'visit'}">
       <P>
         <a href="#">
           <c:out value="${hit.data.date}" /> (Visit)
         </a>
         <BR>Description: <c:out value="${hit.data.description}" />
         <BR>Pet: <a href="<c:url value="/editPet.htm?petId=${hit.data.pet.id}"/>">
           <c:out value="${hit.data.pet.name}" />
         </a>
     </c:when>

   </c:choose>

  </c:forEach>

  <c:if test="${! empty searchResults.pages}">

    <BR><BR><BR>
    <table><tr>
    <c:forEach var="page" items="${searchResults.pages}" varStatus="pagesStatus">
      <td>
      <c:choose>
        <c:when test="${page.selected}">
          <c:out value="${page.from}" />-<c:out value="${page.to}" />
        </c:when>
        <c:otherwise>
          <FORM method="GET">
            <spring:bind path="command.query">
               <INPUT type="hidden" name="query" value="<c:out value="${status.value}"/>" />
            </spring:bind>
            <spring:bind path="command.page">
               <INPUT type="hidden" name="page" value="<c:out value="${pagesStatus.index}"/>" />
            </spring:bind>
            <INPUT type = "submit" value="<c:out value="${page.from}" />-<c:out value="${page.to}" />"/>
          </FORM>
        </c:otherwise>
      </c:choose>
      </td>
    </c:forEach>
    </tr></table>

  </c:if>

</c:if>

<P>
<BR>
--%>