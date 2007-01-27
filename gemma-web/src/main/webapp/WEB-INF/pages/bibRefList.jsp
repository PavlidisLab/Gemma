<%@ include file="/common/taglibs.jsp"%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">

<title>Bibligraphic Reference List</title>

<h2>
	Bibliographic Reference List
</h2>

<p>
	Found
	<%
if ( request.getAttribute( "bibliographicReferences" ) != null ) {
%>
	<%=( ( java.util.Collection ) request.getAttribute( "bibliographicReferences" ) ).size()%>
	<%
	} else {
	%>
	<%="0"%>
	<%
	}
	%>
	references.
</p>

<display:table cellpadding="4" pagesize="100"
	name="bibliographicReferences" class="list" requestURI=""
	id="bibliographicReferenceList"
	decorator="ubic.gemma.web.taglib.displaytag.common.description.BibliographicReferenceWrapper">
	<display:column sortable="true" href="bibRefView.html"
		paramId="accession" paramProperty="pubAccession.accession"
		title="View details">
		<%="Details"%>
	</display:column>

	<display:column property="title" sortable="true"
		titleKey="pubMed.title" maxLength="50" />
	<display:column property="publication" sortable="true"
		titleKey="pubMed.publication" />
	<display:column property="authorList" sortable="true"
		titleKey="pubMed.authors" maxLength="20" />
	<display:column property="citation" sortable="true"
		titleKey="pubMed.cite" />
	<display:setProperty name="basic.empty.showtable" value="true" />
	<display:column sortable="true"
		href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=pubmed&dopt=Abstract&list_uids=ID&query_hl=3"
		paramId="list_uids" paramProperty="pubAccession.accession"
		title="PubMed">
		<%="<img src='/Gemma/images/pubmed.gif' />"%>
	</display:column>
</display:table>

<div align="right">
	<input type="button"
		onclick="javascript:document.newSearchForm.submit()"
		value="New Search">
</div>



