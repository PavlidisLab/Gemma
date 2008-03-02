<%@ include file="/common/taglibs.jsp"%>


<title>Differential expression results for ${geneOfficialSymbol}</title>

<h3>
	Displaying
	<b> ${numDiffResults}</b> dataset(s) where differential expression of
	<b> ${geneOfficialSymbol} </b> meet your threhsold
	<br />
	<br />
	Threshold Q-value:
	<b><c:out value="${threshold}" /> </b>
	<br />
</h3>

<display:table export="true" name="differentialExpressionValueObjects" decorator="ubic.gemma.web.taglib.displaytag.diff.DiffExResultDecorator"  pagesize="200" sort="list" defaultsort="5" class="list" >
	<display:column property="shortName" title="Experiment" sortable="true" />
	<display:column property="name" title ="Experiment name" sortable="true" />
	<display:column property="experimentalFactors" title="Factors" sortable="false" />
	<display:column property="probe" title="Probe" sortable="true" />
	<display:column property="p" title="FDR (q)" sortable="true" comparator="ubic.gemma.web.taglib.displaytag.NumberComparator"/>
</display:table>

<br />
<a href="/Gemma/diff/diffExpressionSearch.html"> Differential Expression Search </a>
