<%@ include file="/common/taglibs.jsp"%>
<head>
	<title><fmt:message key="searchCoexpression.title" /></title>

	<jwr:script src='/scripts/ajax/coexpression/CoexpressionSearchForm.js' useRandomParam="false"/>
	<jwr:script src='/scripts/app/CoexpressionSearch.js'  useRandomParam="false"/>
	<jwr:script src='/scripts/ajax/coexpression/CoexpressionGrid.js'  useRandomParam="false"/>

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
<div id='coexpression-summary' style='width: 350px; float: right; margin: 1em;'></div>
<div id='coexpression-form' style='margin-bottom: 1em;'></div>
<div id='coexpression-results'></div>
<div id='coexpression-experiments' class="x-hidden"></div>
<div id='coexpression-genes' class="x-hidden"></div>
<div id='coexpression-visualization' style='width: 300px; height: 300px; margin: 5px'></div>
