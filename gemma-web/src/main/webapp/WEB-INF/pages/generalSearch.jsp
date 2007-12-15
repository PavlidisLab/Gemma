<%@ include file="/common/taglibs.jsp"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<head>
	<script src="<c:url value='/scripts/ext/adapter/prototype/ext-prototype-adapter.js'/>" type="text/javascript"></script>
	<script src="<c:url value='/scripts/ext/ext-all-debug.js'/>" type="text/javascript"></script>

	<script type="text/javascript" src="<c:url value='/scripts/ext/data/DwrProxy.js'/>"></script>
	<script type="text/javascript" src="<c:url value='/scripts/ext/data/ListRangeReader.js'/>"></script>
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

	<div id="messages"></div>
	<div style="width: 500px">
		<div class="x-box-tl">
			<div class="x-box-tr">
				<div class="x-box-tc"></div>
			</div>
		</div>
		<div class="x-box-ml">
			<div class="x-box-mr">
				<div class="x-box-mc">
					<div id="search-form"></div>
				</div>
			</div>
		</div>
		<div class="x-box-bl">
			<div class="x-box-br">
				<div class="x-box-bc"></div>
			</div>
		</div>
	</div>
	<div id="grid-panel" style="width: 600px; height: 300px;">
		<div id="results-grid"
			style="border: 1px solid #99bbe8; overflow: hidden; width: 665px; height: 300px; position: relative; left: 0; top: 0;"></div>
	</div>
</body>