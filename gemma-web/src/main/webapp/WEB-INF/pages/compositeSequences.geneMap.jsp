<%@ include file="/common/taglibs.jsp"%>
<head>
<jwr:script src='/scripts/ajax/ext/data/DwrProxy.js' />
<jwr:script src='/scripts/app/probe.grid.js' />

<script type="text/javascript">
   Ext.onReady(Gemma.ProbeBrowser.app.init, Gemma.ProbeBrowser.app);
</script>
</head>


<title><c:if test="${arrayDesign.id != null}">&nbsp; 
		Platform elements for : ${arrayDesign.name} 
	</c:if> <c:if test="${gene != null}">
		Platform elements for : ${gene.officialSymbol}
	</c:if></title>


<h2>
	Viewer for
	<c:if test="${arrayDesign.id != null}"> array design: 
		<a href="<c:url value='/arrays/showArrayDesign.html?id=${arrayDesign.id }' />">${arrayDesign.shortName}</a>

		<span>Full name: ${arrayDesign.name}</span>
	</c:if>
	<c:if test="${gene != null}"> gene: 
 			${gene.officialSymbol}
		</c:if>
</h2>
<div id="messages" style="font-size: smaller; height: 12px;"></div>

<div style="background-color: #EEEEEE; margin: 0 0 10px 0; padding: 5px; width: 620px;">
	<div id="details-title"
		style="font-size: smaller; background-color: #EEEEEE; margin: 0 0 10px 0; padding: 5px; width: 600px; height: 150px">
		<em>Details about individual elements can be shown here.</em>
	</div>
	<div id="probe-details"></div>
</div>

<div id="probe-grid"></div>
<c:if test="${arrayDesign.id != null}">
	<div
		style="font-size: smaller; border-width: thin; border-style: dotted; border-color: #CCCCCC; padding: 3px; margin-top: 13px; width: 100%">
		When viewing elements for platforms, only 500 elements will be available above. Use the search function to find
		specific probes.</div>
</c:if>
<c:if test="${gene != null}">
	<div
		style="font-size: smaller; border-width: thin; border-style: dotted; border-color: #CCCCCC; padding: 3px; margin-top: 13px; width: 100%">
		In a few cases, to avoid accessing too much data, not all elements will be shown for a gene.</div>
</c:if>

<input type="hidden" name="cslist" id="cslist" value="${compositeSequenceIdList}" />

<input type="hidden" name="arrayDesignId" id="arrayDesignId" value="${arrayDesign.id}" />
