<%@ include file="/common/taglibs.jsp"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<head>
	<script src="<c:url value='/scripts/ext/adapter/prototype/ext-prototype-adapter.js'/>" type="text/javascript"></script>
	<script src="<c:url value='/scripts/ext/ext-all-debug.js'/>" type="text/javascript"></script>

	<script type="text/javascript" src="<c:url value='/scripts/ext/data/DwrProxy.js'/>"></script>
	<script type="text/javascript" src="<c:url value='/scripts/ext/data/ListRangeReader.js'/>"></script>
	<script type="text/javascript" src="<c:url value='/scripts/ajax/util/PagingDataStore.js'/>"></script>
	<script type="text/javascript" src="<c:url value='/scripts/ajax/util/PagingToolbar.js'/>"></script>
	<script type='text/javascript' src='/Gemma/dwr/interface/SearchService.js'></script>

	<script type='text/javascript' src='/Gemma/dwr/engine.js'></script>
	<script type='text/javascript' src='/Gemma/dwr/util.js'></script>

	<script type="text/javascript" src="<c:url value='/scripts/ajax/search.js'/>" type="text/javascript"></script>

</head>
<body>
	<title><fmt:message key="generalSearch.title" />
	</title>

	<h1>
		General search tool for Gemma
	</h1>

		<div id="messages" ><div id="validation-messages"></div></div><br />
	 
		<div id="search-form" ></div>
				  
		<div id="results-grid"></div>
	</div>
</body>