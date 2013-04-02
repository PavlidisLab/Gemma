<%-- 
author: keshav
version: $Id$
@deprecated - I don't think this is used.
--%>
<%@ include file="/common/taglibs.jsp"%>
<head>
	<title><fmt:message key="diffExpressionSearch.title" />
	</title>

	<jwr:script src='/scripts/ajax/diff/DiffExpressionSearchForm.js' />
	<jwr:script src='/scripts/app/DiffExpressionSearch.js' />
	<jwr:script src='/scripts/ajax/visualization/VisualizationWidget.js' />

	<content tag="heading">
	<fmt:message key="diffExpressionSearch.title" />
	</content>

</head>

<div id="diff-wrap">

	<div id='diffExpression-messages' style='width: 100%; height: 2.4em; margin: 5px'></div>
	<div id='diffExpression-form' style='width: 500px; margin-bottom: 1em;'></div>
	<div id='diffExpression-results'></div>

</div>