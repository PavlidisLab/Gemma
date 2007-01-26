<%@ include file="/common/taglibs.jsp"%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">

<title>Bibligraphic Reference List</title>

<h2>
	Bibliographic Reference List
</h2>

<display:table pagesize="100" name="bibliographicReferences" class="list"
	requestURI="" id="bibliographicReferenceList">
	<display:column sortable="true" href="bibRefView.html" paramId="accession"
		paramProperty="pubAccession.accession" title="View details">
		<%="Details"%>
	</display:column>

	<display:column property="title" sortable="true" titleKey="pubMed.title"
		maxWords="20" />
	<display:column property="publication" sortable="true"
		titleKey="pubMed.publication" />
	<display:column property="authorList" sortable="true"
		titleKey="pubMed.authors" />
	<display:column property="publicationDate" sortable="true"
		decorator="ubic.gemma.web.taglib.displaytag.DateColumnDecorator"
		titleKey="pubMed.year" />
	<display:column property="volume" sortable="true" titleKey="pubMed.volume" />
	<display:column property="pages" sortable="true" titleKey="pubMed.pages" />
	<display:setProperty name="basic.empty.showtable" value="true" />
	<display:column sortable="true"
		href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=pubmed&dopt=Abstract&list_uids=ID&query_hl=3"
		paramId="list_uids" paramProperty="pubAccession.accession"
		title="Go to PubMed">
		<%="PubMed"%>
	</display:column>
</display:table>

<div align="right">
	<input type="button"
		onclick="javascript:document.newSearchForm.submit()"
		value="New Search">
</div>



