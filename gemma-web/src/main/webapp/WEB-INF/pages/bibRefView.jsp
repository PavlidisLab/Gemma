<%@ include file="/common/taglibs.jsp"%>

<%-- $Id$ --%>
<%-- Shows the results of a search for pubmed references. --%>

<jsp:useBean id="bibliographicReference" scope="request"
	class="ubic.gemma.model.common.description.BibliographicReferenceImpl"></jsp:useBean>

<title>Bibliographic Reference record</title>


<h2>
	Bibliographic Reference record
</h2>
<security:authorize ifAnyGranted="GROUP_ADMIN">
	<c:if test="${!requestScope.existsInSystem}">
		<p>
			This reference was obtained from PubMed; it is not in the Gemma system. You can add it to Gemma by clicking the
			button on the bottom of the page, or do a
			<a href="<c:url value="/bibRef/bibRefSearch.html"/>">new search</a>.
		</p>
	</c:if>

	<c:if test="${requestScope.incompleteEntry}">
		<p>
			The entry is incomplete. You can attempt to complete it from pubmed by clicking the 'add' button below.
		</p>
	</c:if>
</security:authorize>

<spring:hasBindErrors name="bibliographicReference">
	<div class="error">
		There were the following error(s) with your submission:
		<ul>
			<c:forEach var="errMsgObj" items="${errors.allErrors}">
				<li>
					<spring:message code="${errMsgObj.code}" text="${errMsgObj.defaultMessage}" />
				</li>
			</c:forEach>
		</ul>
	</div>
</spring:hasBindErrors>

<Gemma:bibref bibliographicReference="${bibliographicReference}" />

<br />
<security:authorize ifAnyGranted="GROUP_ADMIN">
	<table>
		<tr>
			<td align="left">
				<c:if test="${!requestScope.existsInSystem}">
					<div align="left">
						<form method="GET" action="<c:url value="/bibRef/bibRefAdd.html"/>"
							<input type="hidden" name="acc"
							value="${bibliographicReference.pubAccession.accession}">
						<input type="submit" value="Add to Gemma Database">
					</form>
				</div>
			</c:if>
		</td>
		<td align="left">
			<c:if test="${requestScope.incompleteEntry}">
				<div align="left">
					<form method="GET" action="<c:url value="/bibRef/bibRefAdd.html"/>"
						<input type="hidden" name="acc"
							value="${bibliographicReference.pubAccession.accession}">
							<input type="hidden" name="refresh" value="1">
						<input type="submit" value="Add to Gemma Database">
					</form>
				</div>
			</c:if>
		</td>
		<td>
			<c:if test="${requestScope.existsInSystem}"> 
					<div align="right">
						<form method="GET" action="<c:url value="/bibRefEdit.html"/>">
							<input type="submit" value="Edit" />
							<input type="hidden" name="id"
								value="${bibliographicReference.id}">
						</form>
					</div>
			</c:if>
		</td>
		
		<td>
			<c:if test="${requestScope.existsInSystem}"> 
					<div align="right">
						<form method="get"
							action="<c:url value="/bibRef/deleteBibRef.html"/>" 
							<input type="hidden"  name="acc" value="${bibliographicReference.pubAccession.accession}"
							<input type="submit"  
										value="Delete" /></form>
					</div>
			</c:if>
		</td>
	</tr>
</table>
</security:authorize>