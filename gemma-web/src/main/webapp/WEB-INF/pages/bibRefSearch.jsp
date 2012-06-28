<%@ include file="/common/taglibs.jsp"%>
<jsp:useBean id="searchCriteria" scope="request"
	class="ubic.gemma.web.controller.common.description.bibref.PubMedSearchCommand"></jsp:useBean>

<title><fmt:message key="bibliographicReference.search.heading" />
</title>
<%-- FIXME Not used?? --%>
<h2>Search for a reference</h2>
<spring:hasBindErrors name="searchCriteria">
	<div class="error">
		There were the following error(s) with your submission:
		<ul>
			<c:forEach var="errMsgObj" items="${errors.allErrors}">
				<li><spring:message code="${errMsgObj.code}"
						text="${errMsgObj.defaultMessage}" /></li>
			</c:forEach>
		</ul>
	</div>
</spring:hasBindErrors>
<form method="post" name="searchForm"
	action="<c:url value="/bibRefSearch.html"/>">
	<table>
		<tr>
			<td><Gemma:label styleClass="desc" key="pubMed.id" /></td>

			<td><spring:bind path="searchCriteria.accession">
					<input type="text" name="${status.expression}"
						value="${status.value}">
					<span class="fieldError" />
				</spring:bind></td>

			<td align="left"><input type="submit" name="submit"
				value="Submit" onclick="bCancel=false" class="button" /></td>

		</tr>

	</table>
</form>

<p>
	<a href="<c:url value="/bibRef/showAllEeBibRefs.html" />">View all
		bibliographic references in Gemma</a>
</p>
