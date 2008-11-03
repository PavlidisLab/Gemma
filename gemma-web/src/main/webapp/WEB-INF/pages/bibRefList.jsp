<%@ include file="/common/taglibs.jsp"%>
<head>
	<title>Bibligraphic Reference List</title>

	<jwr:script src='/scripts/ext/data/DwrProxy.js' />
	<jwr:script src='/scripts/app/bibRef.js' />

</head>
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

<div style="padding: 4px;" id="messages"></div>

<display:table cellpadding="4" pagesize="100" name="bibliographicReferences" class="list" requestURI=""
	id="bibliographicReferenceList"
	decorator="ubic.gemma.web.taglib.displaytag.common.description.BibliographicReferenceWrapper">
	<display:column sortable="true" href="bibRefView.html" paramId="accession" paramProperty="pubAccession.accession" title="">
		<img src="/Gemma/images/magnifier.png" />
	</display:column>
	<display:column property="title" sortable="true" titleKey="pubMed.title" maxLength="50" />
	<display:column property="authorList" sortable="true" titleKey="pubMed.authors" maxLength="20" />
	<display:column property="year" titleKey="pubMed.year" />
	<display:column property="citation" sortable="true" titleKey="pubMed.cite" />
	<display:setProperty name="basic.empty.showtable" value="true" />
	<display:column sortable="true"
		href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=pubmed&dopt=Abstract&list_uids=ID&query_hl=3"
		paramId="list_uids" paramProperty="pubAccession.accession" title="PubMed">
		<%="<img src='/Gemma/images/pubmed.gif' />"%>
	</display:column>
	<display:column title="Experiments" property="experiments" />
	<security:authorize ifAnyGranted="admin">
		<display:column property="update" sortable="false" title="Update from NCBI" />
	</security:authorize>
</display:table>

<div align="right">
	<input type="button" onclick="javascript:document.newSearchForm.submit()" value="New Search">
</div>



