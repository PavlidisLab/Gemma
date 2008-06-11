<%@ include file="/common/taglibs.jsp"%>
<head>
	<script src="<c:url value='/scripts/ext/adapter/prototype/ext-prototype-adapter.js'/>" type="text/javascript"></script>
	<script src="<c:url value='/scripts/ext/ext-all-debug.js'/>" type="text/javascript"></script>

	<script type="text/javascript" src="<c:url value='/scripts/ext/data/DwrProxy.js'/>"></script>
	<script type="text/javascript" src="<c:url value='/scripts/ext/data/ListRangeReader.js'/>"></script>
	<script type='text/javascript' src='/Gemma/dwr/interface/CompositeSequenceController.js'></script>
	<script type='text/javascript' src='/Gemma/dwr/interface/ArrayDesignController.js'></script>
	<script type='text/javascript' src='/Gemma/dwr/engine.js'></script>
	<script type='text/javascript' src='/Gemma/dwr/util.js'></script>
	<script type='text/javascript' src='/Gemma/scripts/ajax/util/PagingDataStore.js'></script>
	<script type='text/javascript' src='/Gemma/scripts/ajax/util/PagingToolbar.js'></script>

	<script type="text/javascript" src="<c:url value='/scripts/ajax/probe.grid.js'/>"></script>

	<script type="text/javascript">
	Ext.onReady(Gemma.ProbeBrowser.app.init, Ext.Gemma.ProbeBrowser.app);
	</script>
</head>


<title><c:if test="${arrayDesign.id != null}">&nbsp; 
		Probes for : ${arrayDesign.name} 
	</c:if> <c:if test="${gene != null}">
		Probes for : ${gene.officialSymbol}
	</c:if></title>


<div id="toparea" style="height: 60px">
	<div id="pagetitle">
		<h2>
			Probe Viewer for:
			<c:if test="${arrayDesign.id != null}">
				<a href="<c:url value='/arrays/showArrayDesign.html?id=${arrayDesign.id }' />">${arrayDesign.shortName}</a>

				<span>Full name: ${arrayDesign.name}</span>
			</c:if>
			<c:if test="${gene != null}">
 			${gene.officialSymbol}
		</c:if>
		</h2>
	</div>
</div>

<div style="height: 10px; padding: 5px;" id="messages">
</div>
<div id="padding" style="padding: 15px; width: 610px;"></div>
<div style="background-color: #EEEEEE; margin: 0 0 10px 0; padding: 5px; width: 620px;">
	<div id="details-title"
		style="font-size: smaller; background-color: #EEEEEE; margin: 0 0 10px 0; padding: 5px; width: 600px; height: 150px">
		<em>Details about individual probes can be shown here.</em>
	</div>
	<div id="probe-details" style="overflow: hidden; width: 590px">
	</div>
</div>

<div id="probe-grid" style="overflow: hidden; width: 730px; height: 350px;"></div>
<c:if test="${arrayDesign.id != null}">
	<div
		style="font-size: smaller; border-width: thin; border-style: dotted; border-color: #CCCCCC; padding: 3px; margin-top: 13px; width: 100%">
		When viewing probes for array designs, only 500 probes will be available above. Use the search function to find specific
		probes.
	</div>
</c:if>
<c:if test="${gene != null}">
	<div
		style="font-size: smaller; border-width: thin; border-style: dotted; border-color: #CCCCCC; padding: 3px; margin-top: 13px; width: 100%">
		In a few cases, to avoid accessing huge amounts of data, not all probes will be shown for a gene.
	</div>
</c:if>

<input type="hidden" name="cslist" id="cslist" value="${compositeSequenceIdList}" />

<input type="hidden" name="arrayDesignId" id="arrayDesignId" value="${arrayDesign.id}" />
