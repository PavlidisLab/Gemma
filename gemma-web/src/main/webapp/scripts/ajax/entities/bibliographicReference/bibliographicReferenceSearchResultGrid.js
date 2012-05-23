Ext.namespace('Gemma.BibliographicReference');

Gemma.BibliographicReference.Record = new Ext.data.Record.create([{
	name: "id",
	type: "int"
}, {
	name: "volume"
}, {
	name: "title"
}, {
	name: "publicationDate",
	type: 'date'
}, {
	name: "publication"
}, {
	name: "pubAccession"
}, {
	name: "pages"
}, {
	name: "citation"
}, {
	name: "authorList"
}, {
	name: "abstractText"
}, {
	name: "experiments"
}, {
	name: "meshTerms"
}, {
	name: "chemicalsTerms"
}, {
	name: "bibliographicPhenotypes"
}
]);

Gemma.BibliographicReference.SearchStore = Ext.extend(Ext.data.Store, {
	initComponent: function(){
		Gemma.BibliographicReference.SearchStore.superclass.initComponent.call(this);
	},
	remoteSort: false,
	proxy: new Ext.data.DWRProxy({
		apiActionToHandlerMap: {
			read: {
				dwrFunction: BibliographicReferenceController.search
			}
		}
	}),

	reader: new Ext.data.JsonReader({
		root: 'records', // required.
		successProperty: 'success', // same as default.
		messageProperty: 'message', // optional
		totalProperty: 'total', // default is 'total'; optional unless paging.
		idProperty: "id", // same as default
		fields: Gemma.BibliographicReference.Record
	})

});

Gemma.BibliographicReference.ColumnModel = new Ext.grid.ColumnModel({
	defaultSortable: true,
	columns: [{
		header: "Authors",
		dataIndex: 'authorList',
		width: 175
	}, {
		header: "Title",
		dataIndex: 'title',
		id: 'title',
		width: 350
	}, {
		header: "Publication",
		dataIndex: 'publication',
		width: 135
	}, {
		header: "Date",
		dataIndex: 'publicationDate',
		width: 70,
		renderer: Ext.util.Format.dateRenderer("Y")
	}, {
		header: "Experiments",
		dataIndex: 'experiments',
		width: 80,
		renderer: function(value){
			var result = "";
			for (var i = 0; i < value.length; i++) {
				result = result +
				'&nbsp<a target="_blank" ext:qtip="View details of ' +
				value[i].shortName +
				' (' +
				value[i].name +
				')" href="/Gemma/expressionExperiment/showExpressionExperiment.html?id=' +
				value[i].id +
				'">' +
				value[i].shortName +
				'</a>';
			}
			return result;
		}

	}, {
		header: "Phenotypes",
		dataIndex: 'bibliographicPhenotypes',
		width: 80,
		renderer: function(value){
			var maxPhenotypesToDisplay = 3;
			var result = "";
			var phenotypeStrings = {};
			for (var i = 0; i < value.length && i < maxPhenotypesToDisplay ; i++) {
				var phenotypesValues = value[i].phenotypesValues;
				for (var j = 0; j < phenotypesValues.length && j < maxPhenotypesToDisplay ; j++) {
					if( phenotypeStrings[phenotypesValues[j].value] == true ){
						//already made link for this phenotype
						continue;
					}
					if(result.length != 0){
						result += ",";
					}
					result = result +
					'&nbsp<a target="_blank" ext:qtip="View all associations for &quot;' +
					phenotypesValues[j].value +
					'&quot; (' +
					phenotypesValues[j].urlId +
					')" href="' + Gemma.LinkRoots.phenotypePage +
					phenotypesValues[j].urlId +
					'">' +
					phenotypesValues[j].value +
					'</a>';
					phenotypeStrings[phenotypesValues[j].value] = true;
				}
			}
			return result;
		}
	}, {
		header: "PubMed",
		dataIndex: 'citation',
		width: 70,
		renderer: function(value){
			if (value && value.pubmedURL) {
				return (new Ext.Template( Gemma.Common.tpl.pubmedLink.simple )).apply({
					pubmedURL: value.pubmedURL
				});
			}
			return '';
		},
		sortable: false
	}]
});

Gemma.BibliographicReference.SearchResultGrid = Ext.extend( Ext.grid.GridPanel, {
	loadMask: true,
	autoScroll: true,
	layout:'fit',
	view: new Ext.ux.InitialTextGridView({
		initialText: 'Use search boxes above to find papers.',
		emptyText: 'No results found.',
		deferEmptyText: true,
		forceFit:true
	}),
	colModel: Gemma.BibliographicReference.ColumnModel,
	store: new Gemma.BibliographicReference.SearchStore({
		autoLoad:false
	}),
	runKeywordSearch: function( query ){
		if (query.length > 0) {
			this.getStore().load({
				params:[query]
			});
		}
	},
	runPubmedSearch : function(query) {
		if (query.length > 0) {
			BibliographicReferenceController.loadFromPubmedID( query, {
				callback : function(data) {
					if(data !== null){
						var newRecord = new Gemma.BibliographicReference.Record(data);
						this.getStore().removeAll();
						this.getStore().add(newRecord);
						this.updateTitle(1, "pubmed ID = " + query);
						this.getSelectionModel().selectFirstRow();
						this.enableFiltering();
					}else{
						this.getStore().removeAll();
						this.updateTitle(0, "pubmed ID = " + query);
					}
					
				}.createDelegate(this)
			});
		}
	},	
	updateTitle: function(recordCount, query){
		var papers = (recordCount == 1)? " paper" : " papers";
		query = (query && query != null && query.length > 0)? 
					" for \""+query+"\" " : " - ";
		this.setTitle("Bibliographic reference search" +query+ "found "+recordCount+ papers);
	},
	enableFiltering: function(){
		this.getTopToolbar().filterInGridField.enable();
		this.getTopToolbar().filterInGridButton.enable();
	},
	initComponent: function(){
		
		this.loadPubmedFromURL = (document.URL.indexOf("?") > -1 && (document.URL.indexOf("pubmedID=") > -1));
        if (this.loadPubmedFromURL ) {
            var subsetDetails = document.URL.substr(document.URL.indexOf("?") + 1);
            var param = Ext.urlDecode(subsetDetails);
            if (param.pubmedID) {
                var pubmedIDsToLoad = param.pubmedID.split(',');
                this.runPubmedSearch( pubmedIDsToLoad[0] );
            }else{
            	this.pubmedFromURL = null;
            }
        }
		
		this.store.on('load', function( store, records, options){
			var query = (options.params && options.params[0] && options.params[0].length > 0)? 
					options.params[0] : null;
			this.updateTitle(records.length, query);
			this.getSelectionModel().selectFirstRow();
			this.enableFiltering();
		}, this);
		
		var searchInGridFieldKeyword = new Ext.form.TwinTriggerField({
        	emptyText: 'keyword',
        	trigger1Class: 'x-form-clear-trigger',
            trigger2Class: 'x-form-search-trigger',
            hideTrigger1: true,
        	hideTrigger2: true,
        	enableKeyEvents: true,
        	onTrigger1Click: function(event){
        		this.setValue('');
				this.triggers[0].hide();
	            this.triggers[1].hide();
        		this.fireEvent('keywordSearchCleared');
        	},
        	onTrigger2Click: function(event){
    			var txtValue = this.getValue();
        		this.fireEvent('runKeywordSearch', txtValue);
        	},
        	listeners: {
        		'keyup': function( field, e ){
        			var txtValue = this.getValue();
        			if (txtValue.length > 0) {
        				this.triggers[0].show();
        	            this.triggers[1].show();
                		this.fireEvent('keywordSearchBeingTyped');
        			}
        			if (txtValue.length == 0) {
        				this.triggers[0].hide();
        	            this.triggers[1].hide();
                		this.fireEvent('keywordSearchCleared');
        			}
        		},
        		'specialkey': function(field, e){
        			if (e.getKey() == e.ENTER) {
            			var txtValue = this.getValue();
                		this.fireEvent('runKeywordSearch', txtValue);
        			}
        		}
        	}
        });

		var searchInGridFieldPubmed = new Ext.form.TwinTriggerField({
        	emptyText: 'pubmed ID',
        	trigger1Class: 'x-form-clear-trigger',
            trigger2Class: 'x-form-search-trigger',
            hideTrigger1: true,
        	hideTrigger2: true,
        	enableKeyEvents: true,
        	onTrigger1Click: function(event){
        		this.setValue('');
				this.triggers[0].hide();
	            this.triggers[1].hide();
        		this.fireEvent('pubmedSearchCleared');
        	},
        	onTrigger2Click: function(event){
    			var txtValue = this.getValue();
        		this.fireEvent('runPubmedSearch', txtValue);
        	},
        	listeners: {
        		'keyup': function( field, e ){
        			var txtValue = this.getValue();
        			if (txtValue.length > 0) {
        				this.triggers[0].show();
        	            this.triggers[1].show();
                		this.fireEvent('pubmedSearchBeingTyped');
        			}
        			if (txtValue.length == 0) {
        				this.triggers[0].hide();
        	            this.triggers[1].hide();
                		this.fireEvent('pubmedSearchCleared');
        			}
        		},
        		'specialkey': function(field, e){
        			if (e.getKey() == e.ENTER) {
            			var txtValue = this.getValue();
                		this.fireEvent('runPubmedSearch', txtValue);
        			}
        		}
        	}
        });
		
		searchInGridFieldKeyword.on('keywordSearchCleared', function(){
			searchInGridFieldPubmed.enable();
		}, this);		
		searchInGridFieldKeyword.on('keywordSearchBeingTyped', function(){
			searchInGridFieldPubmed.setValue('');
			searchInGridFieldPubmed.disable();
		}, this);		
		searchInGridFieldKeyword.on('runKeywordSearch', function( query ){
			searchInGridFieldPubmed.setValue('');
			searchInGridFieldPubmed.enable();
			this.runKeywordSearch( query );
		}, this);
		
		searchInGridFieldPubmed.on('pubmedSearchCleared', function(){
			searchInGridFieldKeyword.enable();
		}, this);		
		searchInGridFieldPubmed.on('pubmedSearchBeingTyped', function(){
			searchInGridFieldKeyword.setValue('');
			searchInGridFieldKeyword.disable();
		}, this);		
		searchInGridFieldPubmed.on('runPubmedSearch', function( query ){
			searchInGridFieldKeyword.setValue('');
			searchInGridFieldKeyword.enable();
			this.runPubmedSearch( query );
		}, this);
		
		var mytbar = new Ext.Toolbar({
			items: ['Search for papers by ',
			        searchInGridFieldKeyword, ' or ', searchInGridFieldPubmed, '-',
			        new Ext.CycleButton({
						ref: 'filterInGridButton',
						showText: true,
						disabled: true,
						prependText: 'Filter by ',
						items: [{
							text: 'Authors',
							id: 'authorList',
							iconCls: 'view-text',
							checked: true
						}, {
							text: 'Title',
							id: 'title',
							iconCls: 'view-text'
						}, {
							text: 'PudMed ID',
							id: 'pubAccession',
							iconCls: 'view-text'
						}, {
							text: 'Mesh Terms',
							id: 'meshTerms',
							iconCls: 'view-text'
						
						}]
					}), new Ext.form.TextField({
						ref: 'filterInGridField',
						enableKeyEvents: true,
						emptyText: 'Filter',
						disabled: true,
						listeners: {
							'keyup': function(){
								var txtValue = this.getTopToolbar().filterInGridField.getValue();
								this.getStore().clearFilter();
								
								if (txtValue.length > 1) {
									this.getStore().filter(this.getTopToolbar().filterInGridButton.getActiveItem().id, txtValue, true, false);
								}
							},
							scope:this
						}
					})]
		});

		Ext.apply(this, {

			sm: new Ext.grid.RowSelectionModel({
				singleSelect: true,
				listeners: {

					// when a row is selected trigger an action, ex: populate a details panel about the selected row
					rowselect: function(sm, index, record){
						this.fireEvent('bibRefSelected', record);
					},
					scope: this
				}
			}),
			tbar: mytbar
		});

		Gemma.BibliographicReference.SearchResultGrid.superclass.initComponent.call(this);

	}// eo initC
});


function doUpdate(id){
	var callParams = [];
	callParams.push(id);

	var delegate = updateDone.createDelegate(this, [], true);
	var errorHandler = handleFailure.createDelegate(this, [], true);

	callParams.push({
		callback: delegate,
		errorHandler: errorHandler
	});

	BibliographicReferenceController.update.apply(this, callParams);
	Ext.DomHelper.overwrite("messages", {
		tag: 'img',
		src: '/Gemma/images/default/tree/loading.gif'
	});
	Ext.DomHelper.append("messages", {
		tag: 'span',
		html: "&nbsp;Please wait..."
	});

}

function updateDone(data){
	Ext.DomHelper.overwrite("messages", {
		tag: 'img',
		src: '/Gemma/images/icons/ok.png'
	});
	Ext.DomHelper.append("messages", {
		tag: 'span',
		html: "&nbsp;Updated"
	});
}

function handleFailure(data, e){
	Ext.DomHelper.overwrite("messages", {
		tag: 'img',
		src: '/Gemma/images/icons/warning.png'
	});
	Ext.DomHelper.append("messages", {
		tag: 'span',
		html: "&nbsp;There was an error: " + data
	});
}
