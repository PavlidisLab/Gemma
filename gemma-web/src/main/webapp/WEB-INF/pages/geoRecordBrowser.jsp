<%@ include file="/common/taglibs.jsp"%>

<title><fmt:message key="geoBrowser.title" /></title>


<authz:authorize ifAnyGranted="admin">
	<p>
		Displaying
		<b> <c:out value="${numGeoRecords}" /> </b> GEO records. Records are
		not shown for taxa not in the Gemma system.
	</p>
	<form action="<c:url value="/geoBrowser/showBatch.html" />"
		method="POST">
		<input type="submit" name="prev" value="Show Last Batch" />
		<input type="submit" name="next" value="Show Next Batch" />
		<input type="hidden" name="start" value="${start}" />
		<input type="hidden" name="count" value="50" />
	</form>

	<display:table pagesize="5000" name="geoRecords" sort="list"
		class="list" requestURI="" id="geoRecordList"
		decorator="ubic.gemma.web.taglib.displaytag.expression.experiment.GeoRecordWrapper">

		<display:column property="geoAccessionLink" sortable="true"
			sortProperty="geoAccession" title="GEO Acc."
			comparator="ubic.gemma.web.taglib.displaytag.StringComparator" />

		<display:column property="title" sortable="true" sortProperty="title"
			title="Name"
			comparator="ubic.gemma.web.taglib.displaytag.StringComparator" />

		<display:column property="releaseDateNoTime" sortable="true" title="Released" />

		<display:column property="numSamples" sortable="true"
			sortProperty="numSamples" titleKey="bioAssays.title"
			comparator="ubic.gemma.web.taglib.displaytag.NumberComparator" />

		<display:column property="taxa" sortable="true" titleKey="taxon.title" />

		<display:column property="inGemma" sortable="true" title="In Gemma?" />

		<display:setProperty name="basic.empty.showtable" value="true" />
	</display:table>
</authz:authorize>
