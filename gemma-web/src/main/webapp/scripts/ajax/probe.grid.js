/*
 * The 'probe viewer' application. 
 * 
 * This handles situations where we're viewing probes from an array design, or those for a specific gene. For array designs it allows searches.
 * 
 * @author Paul
 * @version $Id$ 
 */
Ext.namespace('Ext.Gemma');

var start = 0; 
var size = 50; // page size
var grid;
var ds;
var detailsDataSource;


var showprobes = function(id) {
	var ids =  id.split(",");
	// note how we pass the new array in directly, without wrapping it in an object first.
	ds.load({params:[ids], 
	callback: function(r, options, success, scope ) {  
		if (success) { 
			Ext.DomHelper.overwrite("messages", this.getCount() + " probes shown" ); 
		} else { 
			Ext.DomHelper.overwrite("messages", "There was an error." );  
		} 
		}
	});
};


var showArrayDesignProbes = function(id) {
	
	ds.load({params: [ { id:id, classDelegatingFor:"ExpressionExperimentImpl" } ],
		callback: function(r, options, success, scope ) {  
		if (!success) {
			Ext.DomHelper.overwrite("messages", "There was an error." );  
		} 
		}});
};

/**
 * Initialize the main grid.
 * @param {boolean} isArrayDesign	
 */
var init = function(isArrayDesign, id) {
	
	
	var convertgps = function(d) {
		var r = "";
		for(var gp in d) {
			r = r + d[gp].name + ",";
		}
		r = r.substr(0, r.length - 1);
		return r;
	};
		
	var convertgenes = function(d) {
		var r = "";
		var count = 0;
		for(var g in d) {
			r = r + "&nbsp;<a href='/Gemma/gene/showGene.html?id=" + d[g].id + "'>" + d[g].officialSymbol + "</a>,";
			++count;
		}
		if (count > 3) {
			r =  "(" +count + ")" + r;
		}
		r = r.substr(0, r.length - 1);// trim tailing comma.
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
			{name:"genes" , convert : convertgenes}]); //  map of gene ids to geneproductvalueobjects

	var proxy;
	var reader = new Ext.data.ListRangeReader({id:"compositeSequenceId"}, recordType);
	
	if (isArrayDesign) {
		proxy = new Ext.data.DWRProxy(ArrayDesignController.getCsSummaries);
		
 	} else {
		proxy = new Ext.data.DWRProxy(CompositeSequenceController.getCsSummaries);
	}
	
	proxy.on("loadexception", handleLoadError);
	 
	ds = new Ext.Gemma.PagingDataStore(
	{
		proxy :  proxy,
		reader : reader,
		pageSize : size
	});
	
	ds.on("load", loadHandler); 
		
};



/**
 * Separate grid for 'details' about the probe and its alignment results.
 */
var initDetails = function() {
	var recordType = Ext.data.Record.create([
		{name:"identity", type : "float" }, 
		{name:"score", type: "float" },
		{name:"blatResult"},
		{name:"compositeSequence"},
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
		
	// add a listener.
	detailsDataSource.on("load", updateSequenceInfo);
	
	var cm = new Ext.grid.ColumnModel([
		{header: "Alignment",  width: 210, dataIndex:"blatResult", renderer:blatResRender}, 
		{header: "Score", width: 60, dataIndex:"score", renderer:numberformat },
		{header: "Identity", width: 60, dataIndex:"identity", renderer:numberformat },  
		{header: "Genes", width: 150, dataIndex:"geneProductIdGeneMap", renderer:geneMapRender  },
		{header: "Products", width: 150, dataIndex:"geneProductIdMap", renderer:gpMapRender  }
		]);
		
	cm.defaultSortable = true;
	cm.setColumnTooltip(0, "Alignment genomic location");
	cm.setColumnTooltip(1, "BLAT score");
	cm.setColumnTooltip(2, "Sequence alignment identity");

	var blgrid = new Ext.grid.GridPanel({renderTo: "probe-details", height: Ext.get("probe-details").getHeight(), store:detailsDataSource, cm:cm, loadMask: true });
	
    var rz = new Ext.Resizable("probe-details", {
	    wrap:true,
	    minHeight:100,
	    pinned:true,
	    handles: 's'
    });
    rz.on('resize', blgrid.doLayout, blgrid);
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
	if (!seqName) {
		seqName = "[Unavailable]";
	}
	var arName = record.get("arrayName") ?  " on " + record.get("arrayDesignName") : "";

	var dh = Ext.DomHelper;
	dh.overwrite("details-title", {tag : 'h2', html : "Details for probe: " + csname + arName});
	dh.append("details-title", {tag : 'ul', id : 'sequence-info', children : [
		{tag : 'li' , id : "probe-description", html: "Probe description: " + "[pending]"},
		{tag : 'li', id: "probe-sequence-name" , html: "Sequence name: " + seqName + "&nbsp;"}
	]});
	 
};

/**
 * Event handler for when the main grid loads: show the first sequence.
 */
var loadHandler = function() {
	if (ds.getCount() > 0) {
		var v = ds.getAt(0);
		var c = v.get("compositeSequenceId");
		showDetails(null, c);
	}
};

var handleLoadError = function(scope,b,message,exception) {
	 Ext.DomHelper.overwrite("messages", {tag : 'img', src:'/Gemma/images/iconWarning.gif' });  
	 Ext.DomHelper.append("messages", {tag : 'span', html : "There was an error while loading data. Try again or contact the webmaster." });  
};

/**
 * Event handler for loading of sequence information in details grid.
 * @param {Object} event
 */
var updateSequenceInfo = function(event) {
	
	var dh = Ext.DomHelper;
	
	if (detailsDataSource.getCount() === 0) {
		// This shouldn't happen any mor ebecause we always return at least a dummy record holding the sequence
		dh.overwrite("probe-description", {tag : 'li' , id : "probe-description", html: "Probe description: " + "[unavailable]"});
		return; 
	}
	var record = detailsDataSource.getAt(0);
	
	// Note this can be a dummy with  no real blat result.
	var	seq = record.get("blatResult").querySequence;
	var cs = record.get("compositeSequence");
	
	
	if (cs !== null) {
		var csDesc = cs.description !== null ?  cs.description : "[None provided]" ;
		dh.overwrite("probe-description", {tag : 'li' , id : "probe-description", html: "Probe description: " + csDesc , "ext:qtip": "Provider's description, may not be accurate"});
	}
	
	dh.append("sequence-info", { tag : 'li' , html: "Length: " + seq.length });
	dh.append("sequence-info", { tag : 'li' , html: "Type: " + seq.type.value });
	if ( seq.fractionRepeats ) {
		dh.append("sequence-info",  { tag : 'li' , html: "Repeat-masked bases: " + Math.round(seq.fractionRepeats * 1000)/10 + "%" });
	}
	dh.append("sequence-info", { tag : 'li' , html: "Sequence: <div class='clob' style='margin:3px;height:30px;font-size:smaller;font-style:courier'>" + seq.sequence + "</div>"});
	
	if (seq.sequenceDatabaseEntry) {
		dh.append("probe-sequence-name", {tag: 'a', id : "ncbiLink", target:"_blank", title: "view at NCBI", href: "http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=Nucleotide&cmd=search&term=" + seq.sequenceDatabaseEntry.accession, html: "<img src ='" + NCBI_ICON + "'/>"});
	}
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
	ds.load({params:[query, id], callback: function(r, options, success, scope ) {  
		if (success) { 
			Ext.DomHelper.overwrite("messages", this.getCount() + " found" ); 
		} else { 
			Ext.DomHelper.overwrite("messages", "There was an error." );  
		} 
	}});
	ds.proxy = oldprox;
	
	// greyout and disable the paging toolbar.
	if (paging) {
    	paging.getEl().mask();
    	paging.getEl().select("input,a,button").each(function(e){e.dom.disabled=true;});
	}
	
	

};

/**
 * Show first batch of data.
 * @param {Object} isArrayDesign
 * @param {Object} id
 */
var reset = function( ) {
	var id = dwr.util.getValue("cslist");
	var isArrayDesign = id === "";
	if (isArrayDesign) {
		id = dwr.util.getValue("arrayDesignId");
	}
	if (isArrayDesign) {
		showArrayDesignProbes(id);
	} else {
		showprobes(id);
	}
	
	// reset the toolbar.
	if (paging) {
    	paging.getEl().unmask();
    	paging.getEl().select("input,a,button").each(function(e){e.dom.disabled=false;});
	}
};

var paging;

/**
 * Prepare main grid that shows the probes.
 */
Ext.onReady(function() {

	var id = dwr.util.getValue("cslist");
	var isArrayDesign = id === "";
	if (isArrayDesign) {
		id = dwr.util.getValue("arrayDesignId");
	}
	init(isArrayDesign, id);
	
	initDetails();
	
	if (Ext.get("probe-grid") === null ) { return; }
	 
	var cm;
	
	if (isArrayDesign) {
		// omit array design column
		cm = new Ext.grid.ColumnModel([
				{header: "Probe Name",  width: 130, dataIndex:"compositeSequenceName", renderer: probelink}, 
				{header: "Sequence", width: 130, dataIndex:"bioSequenceName", renderer: sequencelink },
				{header: "#Hits", width: 50, dataIndex: "numBlatHits"},  
				{header: "Genes", width: 200, dataIndex:"genes" }
		]);

		cm.defaultSortable = true;
		cm.setColumnTooltip(0, "Name of probe (click for details)");
		cm.setColumnTooltip(1, "Name of sequence");
		cm.setColumnTooltip(2, "Number of high-quality BLAT alignments");
	} else {
		cm = new Ext.grid.ColumnModel([
				{header: "ArrayDesign", width: 100, dataIndex:"arrayDesignName", renderer: arraylink },
				{header: "Probe Name",  width: 130, dataIndex:"compositeSequenceName", renderer: probelink}, 
				{header: "Sequence", width: 130, dataIndex:"bioSequenceName", renderer: sequencelink },
				{header: "#Hits", width: 50, dataIndex: "numBlatHits"},  
				{header: "Genes", width: 200, dataIndex:"genes" }
				]);
	
		cm.defaultSortable = true;
		cm.setColumnTooltip(0, "Name of array design (click for details - leaves this page)");
		cm.setColumnTooltip(1, "Name of probe (click for details)");
		cm.setColumnTooltip(2, "Name of sequence");
		cm.setColumnTooltip(3, "Number of high-quality BLAT alignments");
		cm.setColumnTooltip(4, "Symbols of genes this probe potentially targets; if there are more than 3, the total count is provided in parentheses");
	}

	var gridConfig = {
		renderTo: "probe-grid",
		store:ds,
		cm:cm,
		loadMask: true,
		height: Ext.get("probe-grid").getHeight()
	};
	// add a paging toolbar to the grid's footer
	if ( isArrayDesign ) {
		paging = new Ext.Gemma.PagingToolbar({
			store: ds,
	        pageSize: size
	    });
		gridConfig.bbar = paging;
	}
	grid = new Ext.grid.GridPanel( gridConfig );
			
	    // make the grid resizable, do before render for better performance
    var rz = new Ext.Resizable("probe-grid", {
        wrap:true,
        minHeight:100,
        pinned:true,
        handles: 's'
    });
    rz.on('resize', grid.doLayout, grid);
	
	grid.render();
	
	reset();
	
	 	Ext.QuickTips.init();

});

/**************************************************
 * Renderers
 **************************************************/

var GEMMA_BASE_URL = "http://www.bioinformatics.ubc.ca/Gemma/";
var UCSC_ICON = "/Gemma/images/logo/ucsc.gif";
var NCBI_ICON = "/Gemma/images/logo/ncbi.gif";

var sequencelink = function( data, metadata, record, row, column, store  ) {
	if (data === "null") {
		return "<a title='[unavailable]'>-</a>";
	}
	return data;
	//return "<a onclick=\"showDetails(event, " + record.get("compositeSequenceId") + ");\">" + data + "</a>";
};


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
		res = res + "<a href='/Gemma/gene/showGene.html?id=" + data[id].id + "'>" + data[id].officialSymbol + "</a><br />";
	}
	return res;
};

var getDb = function(taxon) {
	if (taxon.externalDatabase) {
		return taxon.externalDatabase.name;
	}
};

var blatResRender = function(d, metadata, record, row, column, store  ) {
	
	if (!d.targetChromosome) {
		return "";
	}
	
	var res = "chr" + d.targetChromosome.name + " (" + d.strand + ") " + d.targetStart + "-" + d.targetEnd;
	
	var organism = d.targetChromosome.taxon;
	var database = getDb(organism);
	if (database) {
		var link = "http://genome.ucsc.edu/cgi-bin/hgTracks?org=" + organism + "&pix=850&db=" + database + "&hgt.customText=" + GEMMA_BASE_URL + "blatTrack.html?id=" + d.id;
		res = res + "&nbsp;<a title='Genome browser view (opens in new window)' target='_blank' href='" + link + "'><img src='" + UCSC_ICON + "'  /></a>";
	}
	return res;
};
