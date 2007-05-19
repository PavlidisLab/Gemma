<%@ include file="/common/taglibs.jsp"%>
<head>
	<script
		src="<c:url value='/scripts/ext/adapter/prototype/ext-prototype-adapter.js'/>"
		type="text/javascript"></script>
	<script src="<c:url value='/scripts/ext/ext-all-debug.js'/>"
		type="text/javascript"></script>

	<script type="text/javascript"
		src="<c:url value='/scripts/ext/data/DwrProxy.js'/>"></script>

	<script type="text/javascript"
		src="<c:url value='/scripts/ext/data/ListRangeReader.js'/>"></script>

	<script type='text/javascript'
		src='/Gemma/dwr/interface/CompositeSequenceController.js'></script>

	<script type='text/javascript'
		src='/Gemma/dwr/interface/ArrayDesignController.js'></script>

	<script type='text/javascript' src='/Gemma/dwr/engine.js'></script>
	<script type='text/javascript' src='/Gemma/dwr/util.js'></script>

	<script type="text/javascript"
		src="<c:url value='/scripts/ajax/probe.grid.js'/>"
		type="text/javascript"></script>
</head>


<title><c:if test="${arrayDesign.id != null}">
		Probes for : ${arrayDesign.name}
	</c:if> <c:if test="${gene != null}">
		Probes for : ${gene.officialSymbol}
	</c:if></title>

<div id="pagetitle">
	<h2>
		<a class="helpLink" href="?"
			onclick="showHelpTip(event, 'This page displays information on multiple probes (array design elements). Enter a gene symbol or probe identifier into the form to find matches.'); return false"><img
				src="/Gemma/images/help.png" /> </a> Probe Viewer
		<c:if test="${arrayDesign.id != null}">		
for <br />
		${arrayDesign.name}
(
<a
				href="<c:url value='/arrays/showArrayDesign.html?id=${arrayDesign.id }' />">${arrayDesign.shortName
				}</a>
)
</c:if>


		<c:if test="${gene != null}">
 for : ${gene.officialSymbol}
</c:if>
	</h2>


	
	 
 
	<c:if test="${arrayDesign.id != null}">
		<h4>
			 Enter search criteria for finding specific probes on this platform.
		</h4>
		<input type="text" id="searchString" name="filter" />
		<input type="submit" value="Find" onClick="search(event);return;" />
		</form>
	</c:if>

</div>
<div style="height:20px;padding:5px;" id="messages"></div>
<div id="padding" style="padding:15px; width:610px;"></div>
<div id="details-title"
	style="background-color:#EEEEEE; margin:0 0 10px 0; padding: 5px; width:620px;height:60px;"><em>Details about individual probes can be shown here.</em></div>
<div id="probe-details"
	style="margin:0 0 10px 0; padding: 10px; border: 1px solid #EEEEEE; overflow: hidden; width:610px; height:150px;">
	
</div>
<div id="padding" style="padding:5px; width:610px;">
	<a class="helpLink" href="?"
		onclick="showWideHelpTip(event, 'Display of probe information. Columns in the table below are: <ul><li>Array design: the name of the platform</li><li>Probe name: the manufacturer probe identifier. Click to view details above.</li><li>Sequence name</li><li>#Hits: Number of distinct high-quality genome alignments for this sequence.</li><li>Genes that this probe is predicted to assay</li></ul>'); return false"><img
			src="/Gemma/images/help.png" /> </a>
</div>

<div id="probe-grid" class="x-grid-mso"
	style="border: 1px solid #c3daf9; overflow: hidden; width:630px; height:350px;"></div>



<input type="hidden" name="cslist" id="cslist"
	value="${compositeSequenceIdList}" />

<input type="hidden" name="arrayDesignId" id="arrayDesignId"
	value="${arrayDesign.id}" />

