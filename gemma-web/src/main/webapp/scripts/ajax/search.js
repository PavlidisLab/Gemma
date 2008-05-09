/*
 * The javascript search interface.  The web interface for our powerful search engine.
* Version: $Id$
*/
//Global Variable
var ds;
var form;
var grid;

//Contants
var MAX_AUTO_EXPAND_SIZE = 15;

var handleLoadSuccess = function(scope,b,arg) {
	Ext.DomHelper.overwrite("messages", scope.getCount() + " found" ); 
 	form.findById('submit-button').setDisabled(false);
 	

	//If possible to expand all and not scroll then expand
 	if (ds.getCount() < MAX_AUTO_EXPAND_SIZE){
 		grid.getView().expandAllGroups();
 		return; 		//no point in checking below
 	}


 	//If there is only 1 returned group then expand it regardless of its size.   	
 	var lastResultClass = ds.getAt(0).data.resultClass;
 	var expand = true;
 	
 	for(var i=1; i<ds.getCount(); i++) {
 		var record = ds.getAt(i).data;
 	 		 if  (record.resultClass !== lastResultClass)
 	 		 	expand = false;
 	}

	if(expand)
		grid.getView().expandAllGroups();
	 	
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
	sm.set('query', query);
	
	var scopes = "&scope=";
	if (searchProbes) {
		scopes = scopes + "P";
	}
	if (searchGenes) {
		scopes = scopes + "G";
	}
	if (searchExperiments) {
		scopes = scopes + "E";
	}
	if (searchArrays) {
		scopes = scopes + "A";
	}
	if (searchSequences) {
		scopes = scopes + "S";
	}
	
	ds.load({params:[{query:query,
		searchProbes:searchProbes,
		searchBioSequences:searchSequences,
		searchArrays:searchArrays,
		searchExperiments:searchExperiments,
		searchGenes:searchGenes}]
	});
	
	Ext.DomHelper.overwrite('messages',""); 
	form.findById('submit-button').setDisabled(true);
	Ext.DomHelper.overwrite('search-bookmark', {tag: 'a', href : "/Gemma/searcher.html?query=" + query + scopes, html : 'Bookmarkable link'}); 
};

var searchForm = function() { 
 	sm = Ext.state.Manager;
 	
 	/*
 	 * If we came here by a direct request ('bookmarkable link'), override the cookie.
 	 */
 	//Get the info from the state manager if there is any
 	var query = sm.get('query', 'Enter Search Term'); 	
 	var searchGenes =  sm.get('searchGenes');
 	var searchExp =   sm.get('searchExperiments');
 	var searchSeq =  sm.get('searchSequences');
 	var searchProbes =  sm.get('searchProbes');
 	var searchArrays =  sm.get('searchArrays');
 	
 	//Override with info from the URL (bookmarkable link)
 	var params = Ext.urlDecode(window.location.href);
 	
 	if (params.scope) {
 	 searchGenes =  params.scope.match("G") ;
 	 searchExp =  params.scope.match("E")  ;
 	 searchSeq =  params.scope.match("S")  ;
 	 searchProbes =   params.scope.match("P") ;
 	 searchArrays =  params.scope.match("A")  ;
 	}
 	
 	if (params.query){
 		query = params.query;
 	}

 	
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
					   value : query,
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
    	 			{ name : "searchGenes", boxLabel : "Genes", checked: searchGenes, hideLabel:true}, 
    				{ name : "searchSequences",boxLabel : "Sequences", checked: searchSeq, hideLabel:true}, 
    				{ name : "searchExperiments",boxLabel : "Experiments", checked :searchExp, hideLabel:true}, 
    				{ name : "searchArrays",boxLabel : "Arrays", checked : searchArrays, hideLabel:true}, 
    				{ name : "searchProbes",boxLabel : "Probes", checked : searchProbes, hideLabel:true}
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
	 			{header: "Category", width: 150, dataIndex:"resultClass", renderer:renderEntityClass, tooltip:"Type of search result" },
				{header: "Item", width: 480, dataIndex:"resultObject", renderer:renderEntity, tooltip:"a link to search result" },
				{header: "Score", width: 60, dataIndex:"score", tooltip:"How good of a match" },
				{header: "Matching text", width: 180, dataIndex:"highlightedText", tooltip:"The text that matched the search" }
	]);
	cm.setHidden(0, true); // don't show the item class column by default.
	cm.setHidden(2,true); // don't show the score by default (usefully for debugging)
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

	grid = new Ext.Gemma.GemmaGridPanel( {
		el : 'search-results-grid',
		width : 800,
		height : 500,
		store:ds, cm:cm, 
		loadMask: true, 
		view : new Ext.grid.GroupingView({startCollapsed: true, forceFit : true, groupTextTpl : '{text}s ({[values.rs.length]} {[values.rs.length > 1 ? "Items" : "Item"]})'}),
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
	} else if ( /^BioSequence.*/.exec(clazz)) { // because we get proxies.
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
	}else if ( /^BioSequence.*/.exec(clazz) ) {
		return "<a href=\"/Gemma/genome/bioSequence/showBioSequence.html?id=" + data.id + "\">" + data.name + "</a> - " + data.taxon.commonName + " " + data.description ;
	} else if (clazz == "Gene" || clazz== "PredictedGene" || clazz== "ProbeAlignedRegion") {
		return "<a href=\"/Gemma/gene/showGene.html?id=" + data.id + "\">" + data.officialSymbol + "</a>  - Species: " + data.taxon.commonName + " Desc: " + data.officialName;
	}else if (clazz == "Bibliographicreference") {
		return "<a href=\"/Gemma/gene/showGene.html?id=" + data.id + "\">" + data.title + "</a> [" + data.pubmedId + "]";
	}
};


Ext.onReady(function() {
	Ext.state.Manager.setProvider(new Ext.state.CookieProvider({expires:new Date(new Date().getTime()+(1000*60*60*24*30))}));
	searchForm();
	initGrid(); 
	Ext.QuickTips.init();
});