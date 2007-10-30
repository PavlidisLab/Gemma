<%@ include file="/common/taglibs.jsp"%>


<head>
	<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
	<title>XML Grid Example</title>

	<script
		src="<c:url value='/scripts/ext/adapter/prototype/ext-prototype-adapter.js'/>"
		type="text/javascript"></script>
	<script src="<c:url value='/scripts/ext/ext-all.js'/>"
		type="text/javascript"></script>
	
	 <script type="text/javascript"
		src="<c:url value='/scripts/ext/data/DwrProxy.js'/>"></script>
	<script type="text/javascript"
		src="<c:url value='/scripts/DwrTreeLoader.js'/>"></script>
	
	 
		
	<%--<script type="text/javascript" src="<c:url value='/scripts/grid.js'/>"></script>
	--%><script type="text/javascript" src="<c:url value='/scripts/tree.js'/>"></script>

	<script type='text/javascript'
		src='/Gemma/dwr/interface/MgedOntologyService.js'></script>
		
	<script type='text/javascript' src='/Gemma/dwr/engine.js'></script>
	<script type='text/javascript' src='/Gemma/dwr/util.js'></script>
	

</head>
<body>

	<%--<h1>
		Ext Grid Example
	</h1>

	<!-- a place holder for the grid. requires the unique id to be passed in the javascript function, and width and height ! -->
	<div id="mygrid" class="x-grid-mso"
		style="border: 1px solid #c3daf9; overflow: hidden; width:520px;"></div>


	--%><h1>
		Ext tree Example
	</h1>

	<!-- a place holder for the tree. requires the unique id to be passed in the javascript function, and width and height ! -->
	<div id="tree-div" class="x-grid-mso"
		style="overflow: hidden; width:520px;"></div>