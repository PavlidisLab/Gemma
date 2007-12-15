<%@ include file="/common/taglibs.jsp"%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
    "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<head>
	<title><fmt:message key="characteristicBrowser.title" /></title>
	<link rel="stylesheet" type="text/css" media="all" href="<c:url value='/styles/${appConfig["theme"]}/theme.css'/>" />
	<authz:authorize ifAnyGranted="admin">
		<script src="<c:url value='/scripts/ext/adapter/prototype/ext-prototype-adapter.js'/>" type="text/javascript"></script>
		<script src="<c:url value='/scripts/ext/ext-all-debug.js'/>" type="text/javascript"></script>
		<script type="text/javascript" src="<c:url value='/scripts/ext/data/ListRangeReader.js'/>"></script>
		<script type="text/javascript" src="<c:url value='/scripts/ext/data/DwrProxy.js'/>"></script>
		<script type='text/javascript' src='/Gemma/dwr/engine.js'></script>
		<script type='text/javascript' src='/Gemma/dwr/util.js'></script>
		<script type="text/javascript" src='/Gemma/dwr/interface/OntologyService.js'></script>
		<script type="text/javascript" src='/Gemma/dwr/interface/MgedOntologyService.js'></script>
		<script type='text/javascript' src='/Gemma/dwr/interface/CharacteristicBrowserController.js'></script>
		<script type='text/javascript' src="<c:url value='/scripts/ajax/annotation/AnnotationGrid.js'/>"></script>
		<script type='text/javascript' src="<c:url value='/scripts/ajax/annotation/CharacteristicCombo.js'/>"></script>
		<script type='text/javascript' src="<c:url value='/scripts/ajax/annotation/MGEDCombo.js'/>"></script>
		<script type='text/javascript' src="<c:url value='/scripts/ajax/util/PagingDataStore.js'/>"></script>
		<script type='text/javascript' src="<c:url value='/scripts/ajax/util/PagingToolbar.js'/>"></script>
		<script type='text/javascript' src="<c:url value='/scripts/ajax/annotation/CharacteristicBrowser.js'/>"></script>
	</authz:authorize>
</head>

		<h1>
			<fmt:message key="characteristicBrowser.heading" />
		</h1>

<authz:authorize ifAnyGranted="admin">
		<div id="characteristicBrowser" class="x-grid-mso" style="border: 1px solid #c3daf9; overflow: hidden;"></div>
</authz:authorize>
<authz:authorize ifNotGranted="admin">
	<p>
		Permission denied.
	</p>
</authz:authorize>