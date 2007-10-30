<%@ include file="/common/taglibs.jsp"%>
<head>
	<script src="<c:url value='/scripts/ext/adapter/prototype/ext-prototype-adapter.js'/>" type="text/javascript"></script>
	<script src="<c:url value='/scripts/ext/ext-all.js'/>" type="text/javascript"></script>

	<script type="text/javascript" src="<c:url value='/scripts/ext/data/DwrProxy.js'/>"></script>
	<script type="text/javascript" src="<c:url value='/scripts/ext/data/ListRangeReader.js'/>"></script>
	<script type='text/javascript' src='/Gemma/dwr/interface/CompositeSequenceController.js'></script>
	<script type='text/javascript' src='/Gemma/dwr/interface/ArrayDesignController.js'></script>
	<script type='text/javascript' src='/Gemma/dwr/engine.js'></script>
	<script type='text/javascript' src='/Gemma/dwr/util.js'></script>

	<script type="text/javascript" src="<c:url value='/scripts/ajax/probe.grid.js'/>" type="text/javascript"></script>
</head>


<title><c:if test="${arrayDesign.id != null}">&nbsp; 
		Probes for : ${arrayDesign.name} 
	</c:if> <c:if test="${gene != null}">
		Probes for : ${gene.officialSymbol}
	</c:if></title>


<div id="toparea" style="height:60px">
	<div id="pagetitle">
		<h2>
			Probe Viewer for:
			<c:if test="${arrayDesign.id != null}">
				<a href="<c:url value='/arrays/showArrayDesign.html?id=${arrayDesign.id }' />">${arrayDesign.shortName}</a>
		</h2>
		<span>Full name: ${arrayDesign.name}</span>
		</c:if>
		<c:if test="${gene != null}">
 			${gene.officialSymbol}</h2>
		</c:if>

	</div>

	<c:if test="${arrayDesign.id != null}">
		<div id="search"
			style="border-width:thin; border-style:dotted; background-color:#EEEEEE;padding:5px;position:absolute; left: 40%;top:0%;width:265px;">

			<span style="text-align:left"> Search for probes on this platform. </span>
			<input type="text" id="searchString" name="filter" onkeyup="if (event.keyCode == 13) search(event) ;return;" />
			<input type="submit" value="Find" onClick="search(event);return;" />
			<input type="button" value="Reset" onClick="reset();return;" />

		</div>
	</c:if>
</div>

<div style="height:10px;padding:5px;" id="messages">
</div>
<div id="padding" style="padding:15px; width:610px;"></div>
<div style="background-color:#EEEEEE; margin:0 0 10px 0; padding: 5px; width:620px;">
	<div id="details-title" style="background-color:#EEEEEE; margin:0 0 10px 0; padding: 5px; width:600px;">
		<em>Details about individual probes can be shown here.</em>
	</div>
	<div id="probe-details"
		style="margin:0 0 10px 0; padding: 10px; border: 1px solid #EEEEEE; overflow: hidden; width:590px; height:100px;">
	</div>
</div>

<div id="probe-grid" class="x-grid-mso" style="border: 1px solid #c3daf9; overflow: hidden; width:630px; height:350px;"></div>
<c:if test="${arrayDesign.id != null}">
	<div style="font-size:smaller;border-width:thin; border-style:dotted; border-color:#CCCCCC;padding:3px;margin-top:13px;width:40%">
		Note that for many array designs, not all probes will be available above. Use the search function to find specific probes
	</div>
</c:if>
<c:if test="${gene != null}">
		<div style="font-size:smaller;border-width:thin; border-style:dotted; border-color:#CCCCCC;padding:3px;margin-top:13px;width:40%">
		Note that in a few cases, to avoid accessing huge amounts of data, not all probes will be shown for a gene.
	</div>
	</c:if>

<input type="hidden" name="cslist" id="cslist" value="${compositeSequenceIdList}" />

<input type="hidden" name="arrayDesignId" id="arrayDesignId" value="${arrayDesign.id}" />
