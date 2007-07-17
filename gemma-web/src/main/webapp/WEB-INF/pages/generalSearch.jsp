<%@ include file="/common/taglibs.jsp"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">


<jsp:useBean id="coexpressionSearchCommand" scope="request"
	class="ubic.gemma.web.controller.coexpressionSearch.CoexpressionSearchCommand" />

<spring:bind path="coexpressionSearchCommand.*">
	<c:if test="${not empty status.errorMessages}">
		<div class="error">
			<c:forEach var="error" items="${status.errorMessages}">
				<img src="<c:url value="/images/iconWarning.gif"/>"
					alt="<fmt:message key="icon.warning"/>" class="icon" />
				<c:out value="${error}" escapeXml="false" />
				<br />
			</c:forEach>
		</div>
	</c:if>
</spring:bind>

<title><fmt:message key="generalSearch.title" />
</title>

<h2>
	General search tool for searching Gemma
	<br />
	<br />
</h2>

<form name="generalSearch" action="searcher.html" method="POST">
	<h4>
		Enter search criteria for searching the Gemma database here
	</h4>
	<input type="text" name="searchString" size="76" />
	<input type="submit" value="search" />


	<script type='text/javascript'
		src='/Gemma/scripts/expandableObjects.js'></script>

	<script language="javascript">
		function toggleDisable()
		{
			if (document.getElementById('advancedSelect').disabled){
				document.getElementById('advancedSelect').disabled = false;
				document.getElementById('advancedTaxon').disabled = false;
				}
			else{
				document.getElementById('advancedSelect').disabled = true;
				document.getElementById('advancedTaxon').disabled = true;
				}
		}
 	</script>

	<!-- Toggles for the expand/hide datasetList table -->
	<span class="advancedSearch"
		onclick="toggleDisable(); return toggleVisibility('advancedSearch')">
		<img src="/Gemma/images/chart_organisation_add.png" /> </span>
	<span class="advancedSearch" style="display:none"
		onclick="toggleDisable(); return toggleVisibility('advancedSearch')">
		<img src="/Gemma/images/chart_organisation_delete.png" /> </span>

	<a href="#"
		onclick="toggleDisable(); return toggleVisibility('advancedSearch')">(Advanced
		Search)</a>

	<br />
	<br />
	<div class="advancedSearch" style="display:none">
		<h4>
			Select a search Mode:
		</h4>
		<select id="advancedSelect" name="advancedSelect" multiple size=5
			disabled="true">
			<option value="GoID">
				Find Genes by Gene Ontology Id
			</option>
			<option value="ontology">
				Search Ontology Database
			</option>
			<option value="bibliographicReference">
				Search Bibliographic Database
			</option>
			<option selected value="Gene">
				Search Gene Database
			</option>
			<option selected value="DataSet">
				Search DataSet Database
			</option>
			<option selected value="Array">
				Search Array Database
			</option>
		</select>
		<br />
		<h4>
			Reduce Results by Taxon:
		</h4>
		<spring:bind path="coexpressionSearchCommand.taxon">
			<select id="advancedTaxon" name="${status.expression}"
				disabled="true">
				<c:forEach items="${taxa}" var="taxon">
					<spring:transform value="${taxon}" var="scientificName" />
					<option value="${scientificName}"
						<c:if test="${status.value == scientificName}">selected </c:if>>
						${scientificName}
					</option>
				</c:forEach>
			</select>

		</spring:bind>
	</div>
</form>

<br />

<h4>

	<a
		href="/Gemma/searcher.html?searchString=<c:out value="${SearchString}"/>&taxon=<c:out value="${searchTaxon}"/>&advancedSelect=<c:out value="${searchDataset}"/>&advancedSelect=<c:out value="${searchArray}"/>&advancedSelect=<c:out value="${searchGene}"/>&advancedSelect=<c:out value="${searchGene}"/>&advancedSelect=<c:out value="${searchGoID}"/>&advancedSelect=<c:out value="${searchOntology}"/> ">
		(Bookmarkable link) </a>

</h4>
<c:if test="${numGenes != null}">
	<h3>
		Your search for
		<b> <c:out value="${SearchString}" /> </b> found
		<b> <c:out value="${numGenes}" /> </b> Genes.
	</h3>
	<br />
</c:if>


<display:table name="geneList" class="list" requestURI="" id="genesList"
	decorator="ubic.gemma.web.taglib.displaytag.gene.GeneFinderWrapper"
	pagesize="20">
	<display:column property="nameLink" sortable="true"
		titleKey="gene.officialSymbol" maxWords="20" />
	<display:column property="taxon" sortable="true" titleKey="taxon.title"
		maxWords="20" />
	<display:column property="officialName" sortable="true"
		titleKey="gene.officialName" maxWords="20" />
	<display:setProperty name="basic.empty.showtable" value="false" />
</display:table>

<c:if test="${numEEs != null}">
	<h3>
		Your search for
		<b> <c:out value="${SearchString}" /> </b> found
		<b> <c:out value="${numEEs}" /> </b> Datasets
	</h3>
	<br />
</c:if>

<display:table pagesize="20" name="expressionList" sort="list"
	class="list" requestURI="" id="expressionExperimentList"
	decorator="ubic.gemma.web.taglib.displaytag.expression.experiment.ExpressionExperimentWrapper">
	<display:column property="nameLink" sortable="true" sortProperty="name"
		titleKey="expressionExperiment.name" />
	<display:column property="shortName" sortable="true"
		titleKey="expressionExperiment.shortName" />
	<authz:authorize ifAnyGranted="admin">
		<display:column property="arrayDesignLink" sortable="true"
			title="Arrays" />
	</authz:authorize>
	<display:column property="assaysLink" sortable="true"
		titleKey="bioAssays.title" />
	<display:column property="taxon" sortable="true" titleKey="taxon.title" />
	<display:setProperty name="basic.empty.showtable" value="false" />
</display:table>

<c:if test="${numADs != null}">
	<h3>
		Your search for
		<b> <c:out value="${SearchString}" /> </b> found
		<b> <c:out value="${numADs}" /> </b> Arrays
	</h3>
	<br />
</c:if>

<display:table name="arrayList" sort="list" class="list" requestURI=""
	id="arrayDesignList" pagesize="20"
	decorator="ubic.gemma.web.taglib.displaytag.expression.arrayDesign.ArrayDesignWrapper">
	<display:column property="name" sortable="true"
		href="arrays/showArrayDesign.html" paramId="id" paramProperty="id"
		titleKey="arrayDesign.name" />
	<display:column property="shortName" sortable="true"
		titleKey="arrayDesign.shortName" />
	<display:column property="expressionExperimentCountLink"
		sortable="true" title="Expts" />
	<authz:authorize ifAnyGranted="admin">
		<display:column property="color" sortable="true"
			titleKey="arrayDesign.technologyType" />
	</authz:authorize>
	<display:setProperty name="basic.empty.showtable" value="false" />
</display:table>


<c:if test="${numGoGenes != null}">
	<h3>
		The GO Term
		<b> <c:out value="${SearchString}" /> </b> is related to
		<b> <c:out value="${numGoGenes}" /> </b> Genes.
	</h3>
	<br />
</c:if>


<display:table name="goGeneList" class="list" requestURI=""
	id="goGeneList"
	decorator="ubic.gemma.web.taglib.displaytag.gene.GeneFinderWrapper"
	pagesize="20">
	<display:column property="nameLink" sortable="true"
		titleKey="gene.officialSymbol" maxWords="20" />
	<display:column property="taxon" sortable="true" titleKey="taxon.title"
		maxWords="20" />
	<display:column property="officialName" sortable="true"
		titleKey="gene.officialName" maxWords="20" />
	<display:setProperty name="basic.empty.showtable" value="false" />
</display:table>


<c:if test="${numOntologyList > 0 }">
	<h3>
		Your search for
		<b> <c:out value="${SearchString}" /> </b> found
		<b> <c:out value="${numOntologyList}" /> </b> Ontology Terms
	</h3>
</c:if>

<display:table name="ontologyList" class="list" requestURI=""
	id="ontologyList" pagesize="10"
	decorator="ubic.gemma.web.taglib.displaytag.OntologyWrapper">
	<display:column property="accession" sortable="true" maxWords="20" />
	<display:column property="value" sortable="true" maxWords="20" />
	<display:column property="category" sortable="true" maxWords="20" />
	<display:column property="description" sortable="true" maxWords="20" />
	<display:setProperty name="basic.empty.showtable" value="false" />
</display:table>

<c:if test="${numGoADs != null}">
	<h3>
		The GO term
		<b> <c:out value="${SearchString}" /> </b> is related to
		<b> <c:out value="${numGoADs}" /> </b> Arrays
	</h3>
	<br />
</c:if>

<display:table name="goArrayList" sort="list" class="list" requestURI=""
	id="goArrayList" pagesize="20"
	decorator="ubic.gemma.web.taglib.displaytag.expression.arrayDesign.ArrayDesignWrapper">
	<display:column property="name" sortable="true"
		href="arrays/showArrayDesign.html" paramId="id" paramProperty="id"
		titleKey="arrayDesign.name" />
	<display:column property="shortName" sortable="true"
		titleKey="arrayDesign.shortName" />
	<display:column property="expressionExperimentCountLink"
		sortable="true" title="Expts" />
	<authz:authorize ifAnyGranted="admin">
		<display:column property="color" sortable="true"
			titleKey="arrayDesign.technologyType" />
	</authz:authorize>
	<display:setProperty name="basic.empty.showtable" value="false" />
</display:table>

<c:if test="${numBibliographicReferenceList != null}">
	<h3>
		The Bibliographic Reference term
		<b> <c:out value="${SearchString}" /> </b> is related to
		<b> <c:out value="${numBibliographicReferenceList}" /> </b>
		Bibliographic References
	</h3>
	<br />
</c:if>

<display:table name="bibliographicReferenceList" sort="list"
	class="list" requestURI="" id="bibliographicReferenceList"
	pagesize="20"
	decorator="ubic.gemma.web.taglib.displaytag.common.description.BibliographicReferenceWrapper">
	<display:column property="title" sortable="true" 
		titleKey="pubMed.title" />
	<display:column property="authorList" sortable="true"
		titleKey="pubMed.authors" />
	<%-- <display:column property="year" sortable="true" title="year" /> --%>
	<display:setProperty name="basic.empty.showtable" value="false" />
</display:table>



