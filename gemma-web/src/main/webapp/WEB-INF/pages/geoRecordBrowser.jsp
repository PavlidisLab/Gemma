<%@ include file="/common/taglibs.jsp"%>

<head>
	<title>GEO Record browser</title>

	<script type="text/javascript" src="<c:url value='/scripts/progressbar.js'/>"></script>
	<script type="text/javascript" src="<c:url value='/scripts/ext/data/DwrProxy.js'/>"></script>
	<script type='text/javascript' src='/Gemma/dwr/interface/ExpressionExperimentLoadController.js'></script>
	<script type='text/javascript' src='/Gemma/dwr/interface/GeoBrowserService.js'></script>
	<script type='text/javascript' src='/Gemma/dwr/interface/TaskCompletionController.js'></script>
	<script type='text/javascript' src='/Gemma/dwr/interface/ProgressStatusService.js'></script>
	<script type='text/javascript' src='/Gemma/dwr/engine.js'></script>
	<script type='text/javascript' src='/Gemma/dwr/util.js'></script>

	<script type="text/javascript" src="<c:url value='/scripts/ajax/loadExpressionExperiment.js'/>" type="text/javascript"></script>
	<script type="text/javascript" src="<c:url value='/scripts/ajax/geoBrowse.js'/>" type="text/javascript"></script>

	<link rel="stylesheet" type="text/css" media="all" href="<c:url value='/styles/progressbar.css'/>" />
</head>
<body>
	<authz:authorize ifAnyGranted="admin">

		<div id="messages" style="margin: 10px; width: 400px"></div>
		<div id="taskId" style="display: none;"></div>
		<div id="progress-area" style="padding: 5px;"></div>
		<br />
		<p>
			Displaying
			<b> <c:out value="${numGeoRecords}" /> </b> GEO records. Records are not shown for taxa not in the Gemma system. If
			you choose to load an experiment, please be careful: experiments that have two (or more) array designs should be
			loaded using the regular load form if you need to suppress the sample-matching functions.
			<strong>Details</strong> will display more information about the dataset, if available from GEO, including
			information about platforms. This information is often not available for a day or two after the data sets becomes
			publicly available.
		</p>
		<form action="<c:url value="/geoBrowser/showBatch.html" />" method="POST">
			<input type="submit" name="prev" value="Show Last Batch" />
			<input type="submit" name="next" value="Show Next Batch" />
			<input type="hidden" name="start" value="${start}" />
			<input type="hidden" name="count" value="50" />
		</form>

		<display:table pagesize="5000" name="geoRecords" sort="list" class="list" requestURI="" id="geoRecordList"
			decorator="ubic.gemma.web.taglib.displaytag.expression.experiment.GeoRecordWrapper">

			<display:column property="geoAccessionLink" sortable="true" sortProperty="geoAccession" title="GEO Acc."
				comparator="ubic.gemma.web.taglib.displaytag.StringComparator" />
			<display:column property="details" sortable="false" title="Details" />
			<display:column property="title" sortable="true" sortProperty="title" title="Name"
				comparator="ubic.gemma.web.taglib.displaytag.StringComparator" />

			<display:column property="releaseDateNoTime" sortable="true" title="Released" />

			<display:column property="numSamples" sortable="true" sortProperty="numSamples" titleKey="bioAssays.title"
				comparator="ubic.gemma.web.taglib.displaytag.NumberComparator" />

			<display:column property="taxa" sortable="true" titleKey="taxon.title" />

			<display:column property="inGemma" sortable="true" title="In Gemma?" />

			<display:setProperty name="basic.empty.showtable" value="true" />
		</display:table>
	</authz:authorize>
</body>