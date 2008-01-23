/*
* Version: $Id$
*/
var ds;
var form;


var handleLoadSuccess = function(scope,b,arg) {
	Ext.DomHelper.overwrite("messages", scope.getCount() + " found" ); 
 	form.findById('submit-button').setDisabled(false);
};

var handleLoadError = function(scope,b,message,exception) {
	 Ext.DomHelper.overwrite('messages', {tag : 'img', src:'/Gemma/images/icons/warning.png' });  
	 Ext.DomHelper.append('messages', {tag : 'span', html : '&nbsp;&nbsp;' + message });  
	 form.findById('submit-button').setDisabled(false);
};

var search = function(t, event) {
	if (!form.getForm().findField('query').isValid() ) {
		return;
	}
	var query = form.getForm().findField('query').getValue();
	var searchProbes = form.getForm().findField('searchProbes').getValue();
	var searchGenes = form.getForm().findField('searchGenes').getValue();
	var searchExperiments = form.getForm().findField('searchExperiments').getValue();
	var searchArrays = form.getForm().findField('searchArrays').getValue();
	var searchSequences = form.getForm().findField('searchSequences').getValue();
	
	sm = Ext.state.Manager;
	sm.set('searchProbes', searchProbes);
	sm.set('searchGenes', searchGenes);
	sm.set('searchExperiments', searchExperiments);
	sm.set('searchArrays', searchArrays);
	sm.set('searchSequences', searchSequences);
	
	ds.load({params:[{query:query,
		searchProbes:searchProbes,
		searchBioSequences:searchSequences,
		searchArrays:searchArrays,
		searchExperiments:searchExperiments,
		searchGenes:searchGenes}]
	});
	
	Ext.DomHelper.overwrite('messages',""); 
	form.findById('submit-button').setDisabled(true);
	Ext.DomHelper.overwrite('search-bookmark', {tag: 'a', href : "/Gemma/searcher.html?query=" + query, html : 'Bookmarkable link'}); 
};

var searchForm = function() { 
 	sm = Ext.state.Manager;
    form = new Ext.form.FormPanel({
    	frame: true, 
    	autoHeight : true,
    	width:300,
    	renderTo:'general-search-form',
    	items : [ 
    		{
    			xtype:'panel',
    			layout:'column',
    			items:[
	    			   new Ext.form.TextField({ 
	    			   id:'search-text-field',
	    			   fieldLabel: 'Search term(s)',
					   name: 'query',
					   columnWidth: 0.75,
					     allowBlank:false,
					   regex : new RegExp("[\\w\\s]{3,}\\*?"),
					   regexText : "Query contains invalid characters",
					   minLengthText : "Query must be at least 3 characters long",
					   msgTarget : "validation-messages",
					   validateOnBlur:false,
					   minLength : 3}),
    				 
    					new Ext.Button({
    					id : 'submit-button',
    					text : 'Submit', 
    					name :'Submit',
    					columnWidth: 0.25, 
    					setSize : function(){},
    					handler : search}) 
    				 ]
    		},
    		{
    			xtype:'fieldset',
    			collapsible:true,
    			autoHeight:true,
    			defaultType:'checkbox',
    			title:'Items to search for',
    			width:180, 
    			items:[
    	 			{ name : "searchGenes", boxLabel : "Genes", checked: sm.get('searchGenes',true), hideLabel:true}, 
    				{ name : "searchSequences",boxLabel : "Sequences", checked: sm.get('searchSequences',false), hideLabel:true}, 
    				{ name : "searchExperiments",boxLabel : "Experiments", checked : sm.get('searchExperiments',true), hideLabel:true}, 
    				{ name : "searchArrays",boxLabel : "Arrays", checked : sm.get('searchArrays',true), hideLabel:true}, 
    				{ name : "searchProbes",boxLabel : "Probes", checked : sm.get('searchProbes',false), hideLabel:true}
    			]
    		} 
    	] 
    });
    
   form.findById('search-text-field').on('specialKey', function(r, e) {
		if (e.getKey() == e.RETURN ) {
			search();
		}
	});
};

var initGrid = function(id) {
	 
	 var recordType = Ext.data.Record.create([
			{name:"score", type:"float"},
			{name:"resultClass", type:"string"},
			{name:"id",type:"int"},
			{name:"resultObject" },
			{name:"highlightedText", type:"string"},
			{name:"indexSearchResult", type:"boolean"}
	]);
	
	
	var cm = new Ext.grid.ColumnModel([
	 			{header: "Category", width: 150, dataIndex:"resultClass", renderer:renderEntityClass },
				{header: "Item", width: 480, dataIndex:"resultObject", renderer:renderEntity },
				{header: "Score", width: 60, dataIndex:"score" },
				{header: "Text", width: 180, dataIndex:"highlightedText" }
	]);
	cm.setHidden(0, true); // don't show the item class column by default.
	cm.defaultSortable = true;
 
    var proxy = new Ext.data.DWRProxy(SearchService.search );
 
	proxy.on("loadexception", handleLoadError.createDelegate(this, [], true));
	
 	var pgSize = 20;
 	
	ds = new Ext.data.GroupingStore(
	{
		proxy:proxy,
		reader:new Ext.data.ListRangeReader({id:"id", root:"data",totalProperty:"totalSize"}, recordType), 
		remoteSort:false,
		pageSize : pgSize, 
		groupField : 'resultClass',
		sortInfo:{field:"score", direction:"DESC"} 
	});

	ds.on("load", handleLoadSuccess.createDelegate(this, [], true) );

	grid = new Ext.grid.GridPanel( {
		el : 'search-results-grid',
		width : 800,
		height : 500,
		store:ds, cm:cm, 
		loadMask: true, 
		view : new Ext.grid.GroupingView({startCollapsed: true, forceFit : true, groupTextTpl : '{text}s ({[values.rs.length]} {[values.rs.length > 1 ? "Items" : "Item"]})'}),
		viewConfig : {
			forceFit : true,
			enableRowBody : true
		},
		collapsible : true
	 });

	grid.render();
	
	var url = document.URL;
	if (url.indexOf("?") > -1) {
		var sq = url.substr(url.indexOf("?") + 1);
		if (Ext.urlDecode(sq).query ) {
			form.getForm().findField('query').setValue(Ext.urlDecode(sq).query);
			search();
		}
	}
	
};

/*
 * Renderers
 */
var renderEntityClass = function(data, metadata, record, row, column, store  ) {
	var clazz = record.get("resultClass");
	if (clazz == "ExpressionExperimentValueObject") {
		return "Expression dataset";		
	} else if (clazz == "CompositeSequence") {
		return "Probe";
	} else if (clazz == "ArrayDesignValueObject") {
		return "Array";
	} else if (clazz == "BioSequence") {
		return "Sequence";
	} else if (clazz == "Gene") {
		return "Gene";
	} else{
		return clazz;
	}
};

var renderEntity = function( data, metadata, record, row, column, store  ) {
	var dh = Ext.DomHelper;
	var clazz = record.get("resultClass");
	if (clazz == "ExpressionExperimentValueObject") {
		return "<a href=\"/Gemma/expressionExperiment/showExpressionExperiment.html?id=" + data.id + "\">" + data.shortName + "</a> - " + data.name ;
	} else if (clazz == "CompositeSequence") {
		return "<a href=\"/Gemma/compositeSequence/show.html?id=" + data.id + "\">" + data.name  + "</a> - " + data.description + "; Array: " + data.arrayDesign.shortName;
	} else if (clazz == "ArrayDesignValueObject") {
		return "<a href=\"/Gemma/arrays/showArrayDesign.html?id=" + data.id + "\">" + data.shortName + "</a>  " + data.name;
	}else if (clazz == "BioSequence") {
		return "<a href=\"/Gemma/genome/bioSequence/showBioSequence.html?id=" + data.id + "\">" + data.name + "</a> - " + data.taxon.commonName + " " + data.description ;
	} else if (clazz == "Gene") {
		return "<a href=\"/Gemma/gene/showGene.html?id=" + data.id + "\">" + data.officialSymbol + "</a>  - Species: " + data.taxon.commonName + " Desc: " + data.officialName;
	}else if (clazz == "Bibliographicreference") {
		return "<a href=\"/Gemma/gene/showGene.html?id=" + data.id + "\">" + data.title + "</a> [" + data.pubmedId + "]";
	}
};


Ext.onReady(function() {
	Ext.state.Manager.setProvider(new Ext.state.CookieProvider({expires:new Date(new Date().getTime()+(1000*60*60*24*30))}));
	searchForm();
	initGrid(); 
});