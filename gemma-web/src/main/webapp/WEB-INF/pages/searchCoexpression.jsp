<%@ include file="/common/taglibs.jsp"%>
<head>
	<title><fmt:message key="searchCoexpression.title" /></title>

	<jwr:script src='/scripts/ajax/coexpression/CoexpressionSearchForm.js' useRandomParam="false" />
	<jwr:script src='/scripts/app/CoexpressionSearch.js' useRandomParam="false" />
	<jwr:script src='/scripts/ajax/coexpression/CoexpressionGrid.js' useRandomParam="false" />
	<jwr:script src='/scripts/ajax/visualization/VisualizationWidget.js' useRandomParam="false" />

	<content tag="heading">
	<fmt:message key="searchCoexpression.heading" />
	</content>

</head>


<authz:authorize ifAnyGranted="admin">
	<input type="hidden" name="hasAdmin" id="hasAdmin" value="true" />
</authz:authorize>
<authz:authorize ifNotGranted="admin">
	<input type="hidden" name="hasAdmin" id="hasAdmin" value="" />
</authz:authorize>

<div id='coexpression-messages' style='width: 100%; height: 1.2em; margin: 5px'></div>
<div id='coexpression-experiments' class="x-hidden"></div>
<div id='coexpression-genes' class="x-hidden"></div>

<div id="coexpression-wrap">
	<div id="loading-mask"
		style="width: 900px; height: 900px; background: #FFFFFF; position: absolute; z-index: 120000; left: 0; top: 0;">
		<div id="loading">
			<div>
				<img src="images/default/tree/loading.gif" style="margin-right: 8px;" align="absmiddle" />
				Loading interface...
			</div>
		</div>
	</div>
	<div id='coexpression-all'></div>
</div>