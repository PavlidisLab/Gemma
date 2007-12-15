/*
* Version: $Id$
*/
var ds;

Ext.onReady(function() {
	searchForm();
	initGrid();
});

var search = function(event) { 
	query = dwr.util.getValue("query");
	ds.load({params:[{query:query}], callback: function(r, options, success, scope ) {  
		if (success) { 
			Ext.DomHelper.overwrite("messages", this.getCount() + " found" ); 
		} else { 
			Ext.DomHelper.overwrite("messages", "There was an error." );  
		} 
	}});
};

var searchForm = function() {
    Ext.form.Field.prototype.msgTarget = 'side';
    var simple = new Ext.form.Form({ 
    });
    simple.add(
        new Ext.form.TextField({
            fieldLabel: 'Search term(s)',
            name: 'query',
            width:275,
            allowBlank:false
        }) 
    );
    simple.addButton('Submit', function(event) {search(event);});
    simple.render('search-form');
};

var objectConvert = function(o) {
	return o;
};

var initGrid = function(id) {
	 
	 var recordType = Ext.data.Record.create([
			{name:"score", type:"float"},
			{name:"resultClass", type:"string"},
			{name:"id",type:"int"},
			{name:"resultObject", convert:objectConvert},
			{name:"highlightedText", type:"string"},
			{name:"indexSearchResult", type:"boolean"},
	]);
	
	 var cm = new Ext.grid.ColumnModel([
				{header: "Item id",  width: 80, dataIndex:"id" }, 
				{header: "Item class", width: 150, dataIndex:"resultClass" },
				{header: "Score", width: 60, dataIndex:"score" },
				{header: "Item", width: 180, dataIndex:"resultObject" },
				{header: "Text", width: 180, dataIndex:"highlightedText" }
	]);
	cm.defaultSortable = true;
 
	ds = new Ext.data.Store(
	{
		proxy:new Ext.data.DWRProxy(SearchService.search),
		reader:new Ext.data.ListRangeReader({id:"id", root:"data",totalProperty:"totalSize"}, recordType), 
		remoteSort:false, 
		sortInfo:{field:"score", direction:"DESC"} 
	});
	 
	grid = new Ext.grid.Grid("results-grid", {ds:ds, cm:cm, loadMask: true });
	rz = new Ext.Resizable("results-grid", {
	    wrap:true,
	    minHeight:200,
	    pinned:true,
	    handles: 's'
    });
    rz.on('resize', grid.autoSize, grid);
	grid.render();
	  gridFoot = grid.getView().getFooterPanel(true);
	     paging  = new Ext.PagingToolbar(gridFoot, ds, {
	     pageSize: 50
    });
};

var handleLoadError = function(scope,b,message,exception) {
	 Ext.DomHelper.overwrite("messages", {tag : 'img', src:'/Gemma/images/iconWarning.gif' });  
	 Ext.DomHelper.append("messages", {tag : 'span', html : "There was an error. Try again or contact the webmaster." });  
};