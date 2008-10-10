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
	<script type='text/javascript' src='/Gemma/dwr/interface/DEDVController.js useRandomParam='false'></script>


	<content tag="heading">
	<fmt:message key="diffExpressionSearch.title" />
	</content>

</head>

<authz:authorize ifAnyGranted="admin">
	<input type="hidden" name="hasAdmin" id="hasAdmin" value="true" />
</authz:authorize>
<authz:authorize ifNotGranted="admin">
	<input type="hidden" name="hasAdmin" id="hasAdmin" value="" />
</authz:authorize>

<div id="diff-wrap">
	<div id="loading-mask"
		style="width: 900%; height: 800px; background: #FFFFFF; position: absolute; z-index: 120000; left: 0; top: 0;">
		<div id="loading">
			<div>
				<img src="/Gemma/images/default/tree/loading.gif" style="margin-right: 8px;" align="absmiddle" />
				Loading interface...
			</div>
		</div>

	</div>
	<div id='diffExpression-messages' style='width: 100%; height: 1.2em; margin: 5px'></div>
	<div id='diffExpression-form' style='width: 500px; margin-bottom: 1em;'></div>
	<div id='diffExpression-results'></div>

</div>