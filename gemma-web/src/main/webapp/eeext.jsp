<%@ include file="/common/taglibs.jsp"%>
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
	<title>XML Grid Example</title>
	<link rel="stylesheet" type="text/css"
		href="<c:url value='/styles/ext-all.css'/>" />

	<!-- Common Styles for the examples -->
	<link rel="stylesheet" type="text/css"
		href="<c:url value='/styles/examples.css'/>" />
	<script type="text/javascript"
		src="<c:url value='/scripts/ext/adapter/prototype/ext-prototype-adapter.js'/>"></script>

	<script type="text/javascript"
		src="<c:url value='/scripts/ext/ext-all.js'/>"></script>

	<script type="text/javascript">Ext.onReady(function(){
    // create the Data Store  
    var ds = new Ext.data.Store({
        // load using HTTP
        proxy: new Ext.data.HttpProxy({url: '/Gemma/getData.html?type=json&qt=1407&maxRows=20'}),

        
        reader: new Ext.data.JsonReader({
               root: 'rows',
               id: 'id',
               totalProperty: 'numRows'
           }, [
               {name: 'id'}, {name : 'sequence'}, {name : 'data'}
           ])
    });

    var cm = new Ext.grid.ColumnModel([
	    {header: "Probe", width: 120, dataIndex: 'id'},
	    {header: "Seq", width: 120, dataIndex: 'sequence'},
		{header: "Data", width: 180, dataIndex: 'data'},
	]);
    cm.defaultSortable = true;

    // create the grid
    var grid = new Ext.grid.Grid('example-grid', {
        ds: ds,
        cm: cm
    });
    grid.render();
    ds.load();
   
    
});
	</script>

	<link rel="stylesheet" type="text/css"
		href="<c:url value='/styles/grid-examples.css'/>" />
</head>
<body>
	<script type="text/javascript"
		src="<c:url value='/scripts/ext/examples.js'/>"></script>

	<h1>
		Ext Grid Example
	</h1>

	<!-- a place holder for the grid. requires the unique id to be passed in the javascript function, and width and height ! -->
	<div id="example-grid" class="x-grid-mso"
		style="border: 1px solid #c3daf9; overflow: hidden; width:520px;"></div>

</body>
