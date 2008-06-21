<%@ include file="/common/taglibs.jsp"%>
<head>

	<script src="<c:url value='/scripts/ext/adapter/prototype/ext-prototype-adapter.js'/>" type="text/javascript"></script>
	<script src="<c:url value='/scripts/ext/ext-all.js'/>" type="text/javascript"></script>
	<script src="<c:url value='/scripts/excanvas.js'/>" type="text/javascript"></script>
	<script type='text/javascript' src='/Gemma/dwr/engine.js'></script>
	<script type='text/javascript' src='/Gemma/dwr/util.js'></script>
	<script type='text/javascript' src='/Gemma/dwr/interface/VisualizationController.js'></script>
	<script type="text/javascript" src="<c:url value='/scripts/ajax/visualization/vectorVis.js'/>"></script>


</head>
<body>

	<form>
		<input type="button" value="draw" onClick="draw();" />
	</form>

	<div id="vis" style="width: 600px; height: 300px;">
		<div>
</body>