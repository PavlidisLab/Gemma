<%@ include file="/common/taglibs.jsp"%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
    "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<head>
<title>
Gene link analysis manager
</title>
<link rel="stylesheet" type="text/css" media="all" href="<c:url value='/styles/${appConfig["theme"]}/theme.css'/>" />
<script src="<c:url value='/scripts/ext/adapter/prototype/ext-prototype-adapter.js'/>" type="text/javascript"></script>
<script src="<c:url value='/scripts/ext/ext-all.js'/>" type="text/javascript"></script>
<script src="<c:url value='/scripts/ext/RowExpander.js'/>" type="text/javascript"></script>
<script type="text/javascript" src="<c:url value='/scripts/ext/data/ListRangeReader.js'/>"></script>
<script type="text/javascript" src="<c:url value='/scripts/ext/data/DwrProxy.js'/>"></script>
<script type='text/javascript' src='/Gemma/dwr/engine.js'></script>
<script type='text/javascript' src='/Gemma/dwr/util.js'></script>
<script type='text/javascript' src='/Gemma/dwr/interface/ExpressionExperimentController.js'></script>

<script type='text/javascript' src='/Gemma/dwr/interface/ExtCoexpressionSearchController.js'></script>
<!--  for findexpressionexperiment -->
<script type='text/javascript' src='/Gemma/dwr/interface/GeneLinkAnalysisManagerController.js'></script>
<script type='text/javascript' src='/Gemma/dwr/interface/GenePickerController.js'></script>
<!--  for taxoncombo -->
<script type='text/javascript' src='/Gemma/scripts/ajax/entities/DatasetSearchField.js'></script>
<script type='text/javascript' src='/Gemma/scripts/ajax/util/GemmaGridPanel.js'></script>
<script type='text/javascript' src='/Gemma/scripts/ajax/util/PagingDataStore.js'></script>
<script type='text/javascript' src='/Gemma/scripts/ajax/util/PagingToolbar.js'></script>
<script type='text/javascript' src='/Gemma/scripts/ajax/entities/TaxonCombo.js'></script>
<script type='text/javascript' src='/Gemma/scripts/ajax/entities/ExpressionExperimentGrid.js'></script>
<script type='text/javascript' src='/Gemma/scripts/ajax/coexpression/GeneLinkAnalysisManager.js'></script>
</head>

<h1>
Gene link analysis manager
</h1>

<authz:authorize ifAnyGranted="admin">
	<input type="hidden" name="hasAdmin" id="hasAdmin" value="true" />
</authz:authorize>
<authz:authorize ifNotGranted="admin">
	<input type="hidden" name="hasAdmin" id="hasAdmin" value="" />
</authz:authorize>
<div id='createAnalysisDialog' class="x-hidden"></div>
<div id='messages' style='width: 600px; height: 1.6em; margin: 0.2em; padding-bottom: 0.4em;'></div>
<div id='genelinkanalysis-analysisgrid' style='width: 910px; margin-bottom: 1em;'></div>
<div style='width:930px;height:600px;'>
<div id='genelinkanalysis-datasetgrid' style='width:450px;position: absolute; '></div>
<div id='genelinkanalysis-newanalysis' style='width:450px;position: absolute; left: 470px;'></div>

</div>
