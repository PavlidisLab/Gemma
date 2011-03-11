<%@ include file="/common/taglibs.jsp"%>
<head>
	<title>Details for ${expressionExperiment.shortName}</title>
	<jwr:script src='/scripts/ajax/ext/data/DwrProxy.js'
		useRandomParam="false" />
	<jwr:script src='/scripts/app/eeDataFetch.js' useRandomParam="false" />
	<jwr:script src='/scripts/app/ExpressionExperimentDetails.js'
		useRandomParam="false" />
	<jwr:script
		src='/scripts/ajax/visualization/EEDetailsVisualizationWidget.js'
		useRandomParam="false" />

</head>


<div id="loading-mask" style=""></div>
<div id="loading">
	<div class="loading-indicator">
		&nbsp;Loading details for ${expressionExperiment.shortName} ...
	</div>
</div>

<input id="eeId" type="hidden" value="${eeId}" />
<input id="taxonName" type="hidden" value="${taxonName}  " />

<div id="eedetails">

	<div id="messages"></div>
	<div id="basics" style="padding: 5px;"></div>
	<div id="annotator" style="padding: 5px;"></div>

</div>


<div class="clearfix" id="design" style="padding: 5px;">
	<h3>
		<fmt:message key="experimentalDesign.title" />
		&nbsp;
		<a title="Go to the design details"
			href="/Gemma/experimentalDesign/showExperimentalDesign.html?eeid=${expressionExperiment.id}">
			<img src="/Gemma/images/magnifier.png" /> </a>
	</h3>
	<div id="eeDesignMatrix"></div>
	<c:if test="${ hasBatchInformation}">
		<span style="font-size: smaller">This experimental design also
			has information on batches, not shown.</span>
		<br />
		<span style="color:#DD2222;font-size: smaller"> ${batchConfound} </span>
	</c:if>
</div>



<div class="clearfix" id="visualization" style="padding: 5px;">
	<h3>
		Visualization
	</h3>
</div>

<div id="qc" style="padding: 5px;">
	<h3>
		Diagnostic plots
		<security:accesscontrollist hasPermission="ADMINISTRATION,WRITE"
			domainObject="${expressionExperiment}">
			<span> <a
				href="refreshCorrMatrix.html?id=${expressionExperiment.id}"><img
						src="/Gemma/images/icons/arrow_refresh_small.png" title="refresh"
						alt="refresh" /> </a> </span>
		</security:accesscontrollist>
	</h3>
	<Gemma:expressionQC ee="${expressionExperiment.id}"
		hasCorrDist="${hasCorrDist}" hasCorrMat="${hasCorrMat}"
		hasPCA="${hasPCA}" hasNodeDegreeDist="${hasNodeDegreeDist}"
		hasPvalueDist="${hasPvalueDist}" numFactors="${numFactors}" />

</div>

<div style="padding-bottom: 12px;" id="qts">
	<h3>
		Quantitation types
	</h3>
	<display:table name="quantitationTypes" class="scrollTable" id="qtList"
		pagesize="100"
		decorator="ubic.gemma.web.taglib.displaytag.quantitationType.QuantitationTypeWrapper">
		<display:column escapeXml="true" property="qtName" sortable="true"
			maxWords="20" titleKey="name" />
		<display:column escapeXml="true" property="description"
			sortable="true" maxLength="20" titleKey="description" />
		<display:column property="qtPreferredStatus" sortable="true"
			maxWords="20" titleKey="quantitationType.preferred" />
		<display:column property="qtRatioStatus" sortable="true" maxWords="20"
			titleKey="quantitationType.ratio" />
		<display:column property="qtBackground" sortable="true" maxWords="20"
			titleKey="quantitationType.background" />
		<display:column property="qtBackgroundSubtracted" sortable="true"
			maxWords="20" titleKey="quantitationType.backgroundSubtracted" />
		<display:column property="qtNormalized" sortable="true" maxWords="20"
			titleKey="quantitationType.normalized" />
		<display:column property="generalType" sortable="true" />
		<display:column property="type" sortable="true" />
		<display:column property="representation" sortable="true"
			title="Repr." />
		<display:column property="scale" sortable="true" />
		<display:setProperty name="basic.empty.showtable" value="false" />
	</display:table>

</div>
<%-- Only printed if user has write permissions to object --%>
<security:accesscontrollist hasPermission="ADMINISTRATION,WRITE"
	domainObject="${expressionExperiment}">
	<div id="history" style="padding: 5px;">
	</div>
	<c:if test="${ lastArrayDesignUpdate != null}">
		<p style="font-size: smaller; padding: 5px">
			The last time an array design associated with this experiment was
			updated: ${lastArrayDesignUpdate.date}
		</p>
	</c:if>
	<input type="hidden" name="hasWritePermission" id="hasWritePermission"
		value="true" />
</security:accesscontrollist>
