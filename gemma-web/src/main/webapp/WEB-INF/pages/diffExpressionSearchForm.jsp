<%-- 
author: keshav
version: $Id$
--%>
<%@ include file="/common/taglibs.jsp"%>
<head>
	<title><fmt:message key="diffExpressionSearch.title" />
	</title>

	<jwr:script src='/scripts/ajax/diff/DiffExpressionSearchForm.js' useRandomParam='false' />
	<jwr:script src='/scripts/app/DiffExpressionSearch.js' useRandomParam='false' />
	<jwr:script src='/Gemma/dwr/interface/DEDVController.js' useRandomParam='false'/>
	<jwr:script src='/scripts/ajax/visualization/VisualizationWidget.js' useRandomParam='false' />
	


	<content tag="heading">
	<fmt:message key="diffExpressionSearch.title" />
	</content>

</head>

<security:authorize ifAnyGranted="admin">
	<input type="hidden" name="hasAdmin" id="hasAdmin" value="true" />
</security:authorize>
<security:authorize ifNotGranted="admin">
	<input type="hidden" name="hasAdmin" id="hasAdmin" value="" />
</security:authorize>

<div id="diff-wrap">
	
	<div id='diffExpression-messages' style='width: 100%; height: 2.4em; margin: 5px'></div>
	<div id='diffExpression-form' style='width: 500px; margin-bottom: 1em;'></div>
	<div id='diffExpression-results'></div>

</div>