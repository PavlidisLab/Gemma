<%@ include file="/common/taglibs.jsp"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">

<head>

	<title><fmt:message key="generalSearch.title" /></title>

	<script src="<c:url value='/scripts/ext/adapter/prototype/ext-prototype-adapter.js'/>" type="text/javascript"></script>
	<script src="<c:url value='/scripts/ext/ext-all.js'/>" type="text/javascript"></script>

	<script type="text/javascript" src="<c:url value='/scripts/ext/data/DwrProxy.js'/>"></script>
	<script type="text/javascript" src="<c:url value='/scripts/ext/data/ListRangeReader.js'/>"></script>
	<script type="text/javascript" src="<c:url value='/scripts/ajax/util/PagingDataStore.js'/>"></script>
	<script type="text/javascript" src="<c:url value='/scripts/ajax/util/PagingToolbar.js'/>"></script>
	<script type='text/javascript' src='/Gemma/dwr/interface/SearchService.js'></script>

	<script type='text/javascript' src='/Gemma/dwr/engine.js'></script>
	<script type='text/javascript' src='/Gemma/dwr/util.js'></script>

	<script type="text/javascript" src="<c:url value='/scripts/ajax/search.js'/>" type="text/javascript"></script>

</head>


<h1>
	General search tool for Gemma
</h1>

<div id="messages"></div>
<div style="height:1em;margin-bottom:" id="validation-messages"></div>
<div style="margin-bottom:10px" id="general-search-form"></div>
<div style="margin:5px"  id="search-bookmark"></div>
<div style="margin-top:2px"  id="search-results-grid"></div>

