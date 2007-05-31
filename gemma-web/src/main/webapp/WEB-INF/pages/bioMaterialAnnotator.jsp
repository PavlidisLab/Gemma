<%@ include file="/common/taglibs.jsp"%>
<html>
	<head>

		<title>BioMaterial annotation interface</title>

		<script
			src="<c:url value='/scripts/ext/adapter/prototype/ext-prototype-adapter.js'/>"
			type="text/javascript"></script>
		<script src="<c:url value='/scripts/ext/ext-all-debug.js'/>"
			type="text/javascript"></script>

		<script type="text/javascript"	src="<c:url value='/scripts/DwrTreeLoader.js'/>"></script>
		<script type="text/javascript" src="<c:url value='/scripts/ext/data/DwrProxy.js'/>"></script>
		<script type="text/javascript" src="<c:url value='/scripts/ext/data/ListRangeReader.js'/>"></script>

		<script type='text/javascript'
			src='/Gemma/dwr/interface/BioMaterialController.js'></script>

		<script type='text/javascript' src='/Gemma/dwr/engine.js'></script>
		<script type='text/javascript' src='/Gemma/dwr/util.js'></script>

		<script type="text/javascript"
			src="<c:url value='/scripts/ajax/bioMaterialAnnotator.js'/>"></script>
		<script type='text/javascript' src='/Gemma/dwr/interface/MgedOntologyService.js'></script>	
	</head>

	<body>


	<h1>Some other content</h1>
		<input type="hidden" name="cslist" id="cslist"
			value="${bioMaterialIdList}" />

		<div id="layout" style="height:1000px;">
			<div id="north-div">
			</div>
			<div id="south-div">
			</div>
			<div id="east-div">				
			</div>
			<div id="west-div">				
			</div>
			<div id="center-div">
			</div>
		</div>

	</body>
</html>
