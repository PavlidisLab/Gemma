<%@ include file="/common/taglibs.jsp"%>

<%-- $Id$ --%>
<%-- Shows the results of a search for pubmed references. --%>

<jsp:useBean id="bibliographicReference" scope="request"
	class="ubic.gemma.model.common.description.BibliographicReferenceImpl"></jsp:useBean>

<title>Bibliographic Reference</title>


<h2>
	Bibliographic Reference record
</h2>

				<c:if test="${byAccession}">
<security:authorize access="hasRole('GROUP_ADMIN')">
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
</c:if>
<div id="detailPanel"></div>

<script type="text/javascript">
Ext.namespace('Gemma');
Ext.onReady(function() {
	Ext.QuickTips.init();
	var detailPanel = new Gemma.BibliographicReference.DetailsPanel({
 		//loadBibRefId : ${bibliographicReferenceVO.id}
		//height: 400,
		//width: 400,
		renderTo: 'detailPanel'
 	});
	
	detailPanel.loadFromId(${bibliographicReferenceId});
});
</script>


<br />
<security:authorize access="hasRole('GROUP_ADMIN')">
	<table>
		<tr>
			<td align="left">
				<c:if test="${byAccession}">
					<c:if test="${!requestScope.existsInSystem}">
					<div align="left">
						<form method="GET" action="<c:url value="/bibRef/bibRefAdd.html"/>">
							<input type="hidden" name="acc"
							value="${pubMedId}">
						<input type="submit" value="Add to Gemma Database">
					</form>
				</div>
				</c:if>
			</c:if>
		</td>
	
	</tr>
</table>
</security:authorize>