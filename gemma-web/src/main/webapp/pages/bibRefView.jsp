<%@ include file="/common/taglibs.jsp"%>


<%-- Shows the results of a search for pubmed references. --%>

<jsp:useBean id="bibliographicReference" scope="request"
	class="ubic.gemma.model.common.description.BibliographicReference"></jsp:useBean>

<title>Bibliographic Reference</title>


<h2>Bibliographic Reference record</h2>

<c:if test="${byAccession}">
	<security:authorize access="hasAuthority('GROUP_ADMIN')">
		<c:if test="${!requestScope.existsInSystem}">
			<p>
				This reference was obtained from PubMed; it is not in the Gemma
				system. You can add it to Gemma by clicking the button on the bottom
				of the page, or do a <a
					href="<c:url value="/bibRef/bibRefSearch.html"/>">new search</a>.
			</p>
		</c:if>

		<c:if test="${requestScope.incompleteEntry}">
			<p>The entry is incomplete. You can attempt to complete it from
				pubmed by clicking the 'add' button below.</p>
			<div align="left">
				<form method="POST" action="<c:url value="/bibRef/bibRefAdd.html"/>">
					<input type="hidden" name="accession" value="${accession}"><input
						type="hidden" name="refresh" value="true"> <input
						type="submit" value="Add to Gemma Database">
				</form>
			</div>
		</c:if>
	</security:authorize>

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
