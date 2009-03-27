<%@ include file="/common/taglibs.jsp"%>
<head>
	<title><fmt:message key="searchCoexpression.title" /></title>

	<jwr:script src='/scripts/ajax/coexpression/CoexpressionSearchForm.js' useRandomParam="false" />
	<jwr:script src='/scripts/app/CoexpressionSearch.js' useRandomParam="false" />
	<jwr:script src='/scripts/ajax/coexpression/CoexpressionGrid.js' useRandomParam="false" />
	<jwr:script src='/scripts/ajax/visualization/CoexpressionVisualizationWidget.js' useRandomParam="false" />


	<jwr:script src='/scripts/app/test/vectorVis.js' />

	<content tag="heading">
	<fmt:message key="searchCoexpression.heading" />
	</content>

</head>


<security:authorize ifAnyGranted="admin">
	<input type="hidden" name="hasAdmin" id="hasAdmin" value="true" />
</security:authorize>
<security:authorize ifNotGranted="admin">
	<input type="hidden" name="hasAdmin" id="hasAdmin" value="" />
</security:authorize>

<security:authorize ifAnyGranted="user">
	<input type="hidden" name="hasUser" id="hasUser" value="true" />
</security:authorize>
<security:authorize ifNotGranted="user">
	<input type="hidden" name="hasUser" id="hasUser" value="" />
</security:authorize>

<div id='coexpression-messages' style='width: 100%; height: 2.2em; margin: 5px'></div>
<div id='coexpression-experiments' class="x-hidden"></div>
<div id='coexpression-genes' class="x-hidden"></div>

<div id="coexpression-wrap">
	<div id='coexpression-all'></div>
</div>

	<form>
		<input type="button" value="draw" onClick="draw();" />
	</form>

	<div id="vis" style="width: 600px; height: 300px;">
		</div>