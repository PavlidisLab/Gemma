<%@ include file="/common/taglibs.jsp"%>
<head>
	<title><fmt:message key="searchCoexpression.title" /></title>
	<link rel="stylesheet" type="text/css" media="all" href="<c:url value='/styles/${appConfig["theme"]}/theme.css'/>" />
	<link rel="stylesheet" type="text/css" media="all" href="<c:url value='/styles/spinner.css'/>" />
	<script src="<c:url value='/scripts/ext/adapter/prototype/ext-prototype-adapter.js'/>" type="text/javascript"></script>
	<script src="<c:url value='/scripts/ext/ext-all-debug.js'/>" type="text/javascript"></script>
	<script src="<c:url value='/scripts/ext/RowExpander.js'/>" type="text/javascript"></script>
	<script src="<c:url value='/scripts/ext/data/PagingMemoryProxy.js'/>" type="text/javascript"></script>
	<script type="text/javascript" src="<c:url value='/scripts/ext/data/ListRangeReader.js'/>"></script>
	<script type="text/javascript" src="<c:url value='/scripts/ext/data/DwrProxy.js'/>"></script>
	<script type="text/javascript" src="<c:url value='/scripts/ext/Spinner.js'/>"></script>
	<script type="text/javascript" src="<c:url value='/scripts/ext/SpinnerStrategy.js'/>"></script>
	<script type='text/javascript' src='/Gemma/dwr/engine.js'></script>
	<script type='text/javascript' src='/Gemma/dwr/util.js'></script>
	<script type='text/javascript' src='/Gemma/dwr/interface/GenePickerController.js'></script>
	<script type='text/javascript' src='/Gemma/dwr/interface/ExtCoexpressionSearchController.js'></script>
	<script type='text/javascript' src='/Gemma/dwr/interface/ExpressionExperimentController.js'></script>
	<script type='text/javascript' src='/Gemma/dwr/interface/DifferentialExpressionSearchController.js'></script>
	<script type='text/javascript' src='/Gemma/dwr/interface/ExpressionExperimentSetController.js'></script>
	<script type='text/javascript' src='/Gemma/scripts/ajax/util/GemmaGridPanel.js'></script>
	<script type='text/javascript' src='/Gemma/scripts/ajax/util/PagingDataStore.js'></script>
	<script type='text/javascript' src='/Gemma/scripts/ajax/util/PagingToolbar.js'></script>
	<script type='text/javascript' src='/Gemma/scripts/ajax/entities/DatasetSearchField.js'></script>
	<script type='text/javascript' src='/Gemma/scripts/ajax/entities/DatasetSearchToolbar.js'></script>
	<script type='text/javascript' src='/Gemma/scripts/ajax/entities/DatasetChooserPanel.js'></script>
	<script type='text/javascript' src='/Gemma/scripts/ajax/entities/ExpressionExperimentGrid.js'></script>
	<script type='text/javascript' src='/Gemma/scripts/ajax/entities/AnalysisCombo.js'></script>
	<script type='text/javascript' src='/Gemma/scripts/ajax/entities/GeneCombo.js'></script>
	<script type='text/javascript' src='/Gemma/scripts/ajax/entities/GeneImportPanel.js'></script>
	<script type='text/javascript' src='/Gemma/scripts/ajax/entities/TaxonCombo.js'></script>
	<script type='text/javascript' src='/Gemma/scripts/ajax/entities/GeneChooserPanel.js'></script>
	<script type='text/javascript' src='/Gemma/scripts/ajax/coexpression/DifferentialExpressionGrid.js'></script>
	<script type='text/javascript' src='/Gemma/scripts/ajax/coexpression/CoexpressionGrid.js'></script>
	<script type='text/javascript' src='/Gemma/scripts/ajax/coexpression/CoexpressionGridRowExpander.js'></script>
	<script type='text/javascript' src='/Gemma/scripts/ajax/coexpression/CoexpressionDatasetGrid.js'></script>
	<script type='text/javascript' src='/Gemma/scripts/ajax/coexpression/CoexpressionSearchForm.js'></script>
	<script type='text/javascript' src='/Gemma/scripts/ajax/coexpression/CoexpressionSummaryGrid.js'></script>
	
	<script type='text/javascript' src='/Gemma/scripts/ajax/coexpression/CoexpressionSearch.js'></script>
	
	<content tag="heading"><fmt:message key="searchCoexpression.heading" /></content>
	
</head>

<authz:authorize ifAnyGranted="admin">
<input type="hidden" name="hasAdmin" id="hasAdmin" value="true" />
</authz:authorize>
<authz:authorize ifNotGranted="admin">
<input type="hidden" name="hasAdmin" id="hasAdmin" value="" />
</authz:authorize>

<div id='coexpression-messages' style='width:100%;height:1.2em;margin:5px'></div>
<div id='coexpression-summary' style='float: right; margin: 1em;'></div>
<div id='coexpression-form' style='margin-bottom: 1em;'></div>
<div id='coexpression-results' ></div>
 
<div id='coexpression-experiments' class="x-hidden"></div> 
<div id='coexpression-genes' class="x-hidden" ></div> 