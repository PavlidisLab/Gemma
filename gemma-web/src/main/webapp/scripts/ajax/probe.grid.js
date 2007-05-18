var showprobes = function(id) {
	var ids =  id.split(",");
	// note how we pass the new array in directly, without wraping it in an object first.
	ds.load({params:[ids]});
};

var showArrayDesignProbes = function(id) {
	ds.load({params:[{id:id}]});
};

var grid;
var ds;
var detailsDataSource;


/**
 * Initialize the main grid.
 * @param {Object} isArrayDesign
 */
var init = function(isArrayDesign) {
	
	var convertgps = function(d) {
		var r = "";
		for(var gp in d) {
			r = r + d[gp].name + ",";
		}
		r = r.substr(0, r.length - 2);
		return r;
	};
		
	var convertgenes = function(d) {
		var r = "";
		for(var g in d) {
			r = r + d[g].officialSymbol + ",";
		}
		r = r.substr(0, r.length - 2);// trim tailing comma.
		return r;
	};
		
	//   CompositeSequenceMapValueObject
	var	recordType = Ext.data.Record.create([
			{name:"compositeSequenceId", type:"int"}, 
			{name:"compositeSequenceName", type:"string"},
			{name:"arrayDesignName", type:"string"},
			{name:"arrayDesignId", type:"int"},
			{name:"bioSequenceId", type:"int" },
			{name:"bioSequenceName", type:"string" },
			{name:"numBlatHits",type:"int" }, 
			{name:"bioSequenceNcbiId", type:"string" }, 
			{name:"geneProducts", convert : convertgps }, // map of gp ids to geneproductvalueobjects
			{name:"genes" , convert : convertgenes}]); //  map of gene ids to geneproductvalueobjects

	var proxy;
	if (isArrayDesign) {
		proxy = new Ext.data.DWRProxy(ArrayDesignController.getCsSummaries);
 	} else {
		proxy = new Ext.data.DWRProxy(CompositeSequenceController.getCsSummaries);
	}
	
	ds = new Ext.data.Store(
	{
		proxy:proxy,
		reader:new Ext.data.ListRangeReader({id:"compositeSequenceId"}, recordType), 
		remoteSort:false,
		sortInfo:{field:'arrayDesignName'}
	});
	 
		
};


/**
 * Separate grid for 'details' about the probe and its alignment results.
 */
var initDetails = function() {
	var recordType = Ext.data.Record.create([
		{name:"identity", type : "float" }, 
		{name:"score", type: "float" },
		{name:"blatResult"},
		{name:"geneProductIdMap"},
		{name:"geneProductIdGeneMap"}
		]); 

	detailsDataSource = new Ext.data.Store(
		{
		proxy:new Ext.data.DWRProxy(CompositeSequenceController.getBlatMappingSummary), 
		reader:new Ext.data.ListRangeReader({id:"blatResultId"}, recordType), 
		remoteSort:false,
		sortInfo:{field:"score", direction:"DESC"}
		}); 
	
	var cm = new Ext.grid.ColumnModel([
		{header: "Alignment",  width: 160, dataIndex:"blatResult", renderer:blatResRender}, 
		{header: "Score", width: 60, dataIndex:"score", renderer:numberformat },
		{header: "Identity", width: 60, dataIndex:"identity", renderer:numberformat },  
		{header: "Genes", width: 120, dataIndex:"geneProductIdGeneMap", renderer:geneMapRender  },
		{header: "Products", width: 120, dataIndex:"geneProductIdMap", renderer:gpMapRender  }
		]);
	cm.defaultSortable = true;
	cm.setColumnTooltip(0, "Alignment genomic location");
	cm.setColumnTooltip(1, "BLAT score");
	cm.setColumnTooltip(2, "Sequence alignment identity");
	var blgrid = new Ext.grid.Grid("probe-details", {ds:detailsDataSource, cm:cm, loadMask: true });
	
	blgrid.render();
};
 
/**
 * Event handler for clicks on probe name column in bottom grid.
 * @param {Object} event
 * @param {Object} id
 */
var showDetails = function(event, id) {
	var record = ds.getById(id);
	
	detailsDataSource.load({params:[{id:id}]});
	
	if (record === undefined) {
		return;
	}
		var csname = record.get("compositeSequenceName");
		var seqName = record.get("bioSequenceName");
	
		var dh = Ext.DomHelper;
		dh.overwrite("details-title", {tag : 'h2', html : "Details for: " + csname + " on " + record.get("arrayDesignName")});
		dh.append("details-title", {tag : 'ul', children : [
			{tag : 'li' , html: "Sequence: " + seqName}
		]});
};

/**
 * Event handler for searches. Update the lower grid.
 * @param {Object} event
 */
var search = function(event) {
	id = dwr.util.getValue("arrayDesignId");
	query = dwr.util.getValue("searchString");
	var oldprox = ds.proxy;
	ds.proxy = new Ext.data.DWRProxy(CompositeSequenceController.search);
	ds.load({params:[ query, id]});
	ds.proxy = oldprox;
}

/**
 * Prepare main grid that shows the probes.
 */
Ext.onReady(function() {

	var id = dwr.util.getValue("cslist");
	var isArrayDesign = id === "";
	init(isArrayDesign);
	
	initDetails();
	
	if (Ext.get("probe-grid") === null ) { return; }
	 
	var cm = new Ext.grid.ColumnModel([
			{header: "ArrayDesign", width: 100, dataIndex:"arrayDesignName", renderer: arraylink },
			{header: "Probe Name",  width: 130, dataIndex:"compositeSequenceName", renderer: probelink}, 
			{header: "Sequence", width: 130, dataIndex:"bioSequenceName" },
			{header: "#Hits", width: 50, dataIndex: "numBlatHits"},  
			{header: "Genes", width: 130, dataIndex:"genes" }
			//{header: "Products", width: 140, dataIndex:"geneProducts"  }
			]);
	cm.defaultSortable = true;
	cm.setColumnTooltip(0, "Name of array design (click for details - leaves this page)");
	cm.setColumnTooltip(1, "Name of probe (click for details)");
	cm.setColumnTooltip(2, "Name of sequence");
	cm.setColumnTooltip(3, "Number of high-quality BLAT alignments");

	grid = new Ext.grid.Grid("probe-grid", {ds:ds, cm:cm, loadMask: true });
	
	    // make the grid resizable, do before render for better performance
    var rz = new Ext.Resizable("probe-grid", {
        wrap:true,
        minHeight:100,
        pinned:true,
        handles: 's'
    });
    rz.on('resize', grid.autoSize, grid);
	
	grid.render();
	
	if (isArrayDesign) {
		id = dwr.util.getValue("arrayDesignId");
		showArrayDesignProbes(id);
	} else {
		showprobes(id);
	}
	
});

/*
 * Renderers
 */
var probelink = function( data, metadata, record, row, column, store  ) {
	return "<a onclick=\"showDetails(event, " + record.get("compositeSequenceId") + ");\">" + data + "</a>";
};

var arraylink = function( data, metadata, record, row, column, store  ) {
	return "<a href='/Gemma/arrays/showArrayDesign.html?id=" + record.get("arrayDesignId") +  "'>" + record.get("arrayDesignName") +  "</a>";
};

var numberformat = function(d) {
	return Math.round(d*100)/100;
};

var gpMapRender = function(data) {
	var res = "";
	for(var id in data) {
		res = res + data[id].name + "<br />";
	}
	return res;
};

var geneMapRender = function(data) {
	var res = "";
	for(var id in data) {
		res = res + data[id].officialSymbol + "<br />";
	}
	return res;
};



var blatResRender = function(d, metadata, record, row, column, store  ) {
	var res = "chr" + d.targetChromosome.name + " " + d.targetStart + "-" + d.targetEnd;
	return res;
};
