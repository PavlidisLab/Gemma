<%@ include file="/common/taglibs.jsp"%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
    "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<head>
	<title><fmt:message key="searchCoexpression.title" /></title>
	<link rel="stylesheet" type="text/css" media="all" href="<c:url value='/styles/${appConfig["theme"]}/theme.css'/>" />
	<script src="<c:url value='/scripts/ext/adapter/prototype/ext-prototype-adapter.js'/>" type="text/javascript"></script>
	<script src="<c:url value='/scripts/ext/ext-all-debug.js'/>" type="text/javascript"></script>
	<script src="<c:url value='/scripts/ext/RowExpander.js'/>" type="text/javascript"></script>
	<script type="text/javascript" src="<c:url value='/scripts/ext/data/ListRangeReader.js'/>"></script>
	<script type="text/javascript" src="<c:url value='/scripts/ext/data/DwrProxy.js'/>"></script>
	<script type='text/javascript' src='/Gemma/dwr/engine.js'></script>
	<script type='text/javascript' src='/Gemma/dwr/util.js'></script>
	<script type='text/javascript' src='/Gemma/dwr/interface/GenePickerController.js'></script>
	<script type='text/javascript' src='/Gemma/dwr/interface/ExtCoexpressionSearchController.js'></script>
	<script type='text/javascript' src='/Gemma/dwr/interface/DifferentialExpressionSearchController.js'></script>
	<script type='text/javascript' src='/Gemma/scripts/ajax/coexpression/GeneCombo.js'></script>
	<script type='text/javascript' src='/Gemma/scripts/ajax/coexpression/TaxonCombo.js'></script>
	<script type='text/javascript' src='/Gemma/scripts/ajax/coexpression/GeneChooserPanel.js'></script>
	<script type='text/javascript' src='/Gemma/scripts/ajax/util/GemmaGridPanel.js'></script>
	<script type='text/javascript' src='/Gemma/scripts/ajax/util/PagingDataStore.js'></script>
	<script type='text/javascript' src='/Gemma/scripts/ajax/util/PagingToolbar.js'></script>
	<script type='text/javascript' src='/Gemma/scripts/ajax/coexpression/DifferentialExpressionGrid.js'></script>
	<script type='text/javascript' src='/Gemma/scripts/ajax/coexpression/CoexpressionGrid.js'></script>
	<script type='text/javascript' src='/Gemma/scripts/ajax/coexpression/CoexpressionDatasetGrid.js'></script>
	<script type='text/javascript' src='/Gemma/scripts/ajax/coexpression/CoexpressionSearchForm.js'></script>
	<script type='text/javascript' src='/Gemma/scripts/ajax/coexpression/CoexpressionSearch.js'></script>
</head>

<h1>
	<fmt:message key="searchCoexpression.heading" />
</h1>

<authz:authorize ifAnyGranted="admin">
<input type="hidden" name="hasAdmin" id="hasAdmin" value="true" />
</authz:authorize>
<authz:authorize ifNotGranted="admin">
<input type="hidden" name="hasAdmin" id="hasAdmin" value="" />
</authz:authorize>

<div id='coexpression-summary' style='width: 250px; float: right; margin: 1em;'></div>
<div id='coexpression-form' style='width: 420px; margin-bottom: 1em;'></div>
<div id='coexpression-results' style=''></div>