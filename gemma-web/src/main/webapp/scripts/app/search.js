/**
 * The javascript search interface.
 * 
 * @authors kelsey, paul
 * @version: $Id$
 */
Ext.namespace("Gemma.Search");
Ext.BLANK_IMAGE_URL = '/Gemma/images/default/s.gif';
Gemma.Search.app = function() {
	return {
		init : function() {

			this.form = new Gemma.SearchForm({
				renderTo : 'general-search-form'
			});

			this.resultGrid = new Gemma.SearchGrid({
				renderTo : 'search-results-grid',
				form : this.form
			});

			this.form.on("search", this.search.createDelegate(this));

			/*
			 * Search from url if we have to.
			 */
			var url = document.URL;
			if (url.indexOf("?") > -1) {
				var sq = url.substr(url.indexOf("?") + 1);
				if (Ext.urlDecode(sq).query) {
					this.form.getForm().findField('query').setValue(Ext
							.urlDecode(sq).query);
					this.search();
				}
			}

		},

		/**
		 * 
		 */
		search : search = function(t, event) {
			if (!this.form.getForm().findField('query').isValid()) {
				return;
			}
			var query = Ext.getCmp('search-text-field').getValue();
			var searchProbes = Ext.getCmp('search-prbs-chkbx').getValue();
			var searchGenes = Ext.getCmp('search-genes-chkbx').getValue();
			var searchExperiments = Ext.getCmp('search-exps-chkbx').getValue();
			var searchArrays = Ext.getCmp('search-ars-chkbx').getValue();
			var searchSequences = Ext.getCmp('search-seqs-chkbx').getValue();
			//
			// var sm = Ext.state.Manager;
			//
			// // FIXME combine these into a single cookie.
			// sm.set('searchProbes', searchProbes);
			// sm.set('searchGenes', searchGenes);
			// sm.set('searchExperiments', searchExperiments);
			// sm.set('searchArrays', searchArrays);
			// sm.set('searchSequences', searchSequences);

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

			this.resultGrid.getStore().load({
				params : [{
					query : query,
					searchProbes : searchProbes,
					searchBioSequences : searchSequences,
					searchArrays : searchArrays,
					searchExperiments : searchExperiments,
					searchGenes : searchGenes
				}]
			});

			Ext.DomHelper.overwrite('messages', "");
			this.form.findById('submit-button').setDisabled(true);
			Ext.DomHelper.overwrite('search-bookmark', {
				tag : 'a',
				href : "/Gemma/searcher.html?query=" + query + scopes,
				html : 'Bookmarkable link'
			});
		}
	};
}();

Gemma.Search.MAX_AUTO_EXPAND_SIZE = 15;

Gemma.SearchForm = Ext.extend(Ext.form.FormPanel, {
	frame : true,
	autoHeight : true,
	width : 300,

	/**
	 * Restore state ... fixme.
	 */
	restoreState : function() {

		// Override with info from the URL (bookmarkable link)
		var params = Ext.urlDecode(window.location.href);

		if (params.scope) {
			var searchGenes = params.scope.match("G") !== null;
			var searchExp = params.scope.match("E") !== null;
			var searchSeq = params.scope.match("S") !== null;
			var searchProbes = params.scope.match("P") !== null;
			var searchArrays = params.scope.match("A") !== null;

			// Populate the form with the values. Note we force false if not
			// present, even if cookie demands it. This makes the bookmark
			// accurate
			Ext.getCmp('search-genes-chkbx').setValue(searchGenes);
			Ext.getCmp('search-seqs-chkbx').setValue(searchSeq);
			Ext.getCmp('search-exps-chkbx').setValue(searchExp);
			Ext.getCmp('search-ars-chkbx').setValue(searchArrays);
			Ext.getCmp('search-prbs-chkbx').setValue(searchProbes);

		}

	},

	initComponent : function() {

		Ext.apply(this, {
			items : [{
				xtype : 'panel',
				layout : 'column',
				items : [new Ext.form.TextField({
					id : 'search-text-field',
					fieldLabel : 'Search term(s)',
					name : 'query',
					columnWidth : 0.75,
					allowBlank : false,
					regex : new RegExp("[\\w\\s]{3,}\\*?"),
					regexText : "Query contains invalid characters",
					minLengthText : "Query must be at least 3 characters long",
					msgTarget : "validation-messages",
					validateOnBlur : false,
					value : this.query,
					minLength : 3,
					listeners : {
						'specialkey' : {
							fn : function(r, e) {
								if (e.getKey() == e.ENTER) {
									this.fireEvent("search");
								}
							}.createDelegate(this),
							scope : this
						}
					}
				}),

				new Ext.Button({
					id : 'submit-button',
					text : 'Submit',
					name : 'Submit',
					columnWidth : 0.25,
					setSize : function() {
					},
					handler : function() {
						this.fireEvent("search");
					}.createDelegate(this)
				})]
			}, {
				xtype : 'fieldset',
				collapsible : true,
				autoHeight : true,
				defaultType : 'checkbox',
				title : 'Items to search for',
				width : 180,
				items : [{
					id : 'search-genes-chkbx',
					name : "searchGenes",
					boxLabel : "Genes",
					stateful : true,
					stateEvents : ['check'],
					hideLabel : true,
					getState : function() {
						return {
							value : this.getValue()
						};
					},
					applyState : function(state) {
						this.setValue(state.value);
					}
				}, {
					id : 'search-seqs-chkbx',
					name : "searchSequences",
					boxLabel : "Sequences",
					stateful : true,
					stateEvents : ['check'],
					getState : function() {
						return {
							value : this.getValue()
						};
					},
					applyState : function(state) {
						this.setValue(state.value);
					},
					hideLabel : true
				}, {
					id : 'search-exps-chkbx',
					name : "searchExperiments",
					stateful : true,
					stateEvents : ['check'],
					getState : function() {
						return {
							value : this.getValue()
						};
					},
					applyState : function(state) {
						this.setValue(state.value);
					},
					boxLabel : "Experiments",
					hideLabel : true
				}, {
					id : 'search-ars-chkbx',
					name : "searchArrays",
					boxLabel : "Arrays",
					stateful : true,
					stateEvents : ['check'],
					getState : function() {
						return {
							value : this.getValue()
						};
					},
					applyState : function(state) {
						this.setValue(state.value);
					},
					hideLabel : true
				}, {
					id : 'search-prbs-chkbx',
					name : "searchProbes",
					boxLabel : "Probes",
					stateful : true,
					stateEvents : ['check'],
					getState : function() {
						return {
							value : this.getValue()
						};
					},
					applyState : function(state) {
						this.setValue(state.value);
					},
					hideLabel : true
				}]
			}]
		});
		Gemma.SearchForm.superclass.initComponent.call(this);
		this.addEvents("search");

		this.restoreState();
	}

});

Gemma.SearchGrid = Ext.extend(Ext.grid.GridPanel, {

	width : 800,
	height : 500,
	loadMask : true,
	collapsible : false,
	title : "Search results",

	record : Ext.data.Record.create([{
		name : "score",
		type : "float"
	}, {
		name : "resultClass",
		type : "string"
	}, {
		name : "id",
		type : "int"
	}, {
		name : "resultObject"
	}, {
		name : "highlightedText",
		type : "string"
	}, {
		name : "indexSearchResult",
		type : "boolean"
	}]),

	initComponent : function() {
		var proxy = new Ext.data.DWRProxy(SearchService.search);

		proxy.on("loadexception", this.handleLoadError.createDelegate(this));

		Ext.apply(this, {
			view : new Ext.grid.GroupingView({
				startCollapsed : true,
				forceFit : true,
				groupTextTpl : '{text}s ({[values.rs.length]} {[values.rs.length > 1 ? "Items" : "Item"]})'
			}),
			columns : [{
				header : "Category",
				width : 150,
				dataIndex : "resultClass",
				renderer : this.renderEntityClass,
				tooltip : "Type of search result",
				hidden : true,
				sortable : true
			}, {
				header : "Item",
				width : 480,
				dataIndex : "resultObject",
				renderer : this.renderEntity,
				tooltip : "a link to search result",
				sortable : true
			}, {
				header : "Score",
				width : 60,
				dataIndex : "score",
				tooltip : "How good of a match",
				hidden : true,
				sortable : true
			}, {
				header : "Matching text",
				width : 180,
				dataIndex : "highlightedText",
				tooltip : "The text that matched the search",
				sortable : true
			}],
			store : new Ext.data.GroupingStore({
				proxy : proxy,
				reader : new Ext.data.ListRangeReader({
					id : "id",
					root : "data",
					totalProperty : "totalSize"
				}, this.record),
				remoteSort : false,
				pageSize : 20,
				groupField : 'resultClass',
				sortInfo : {
					field : "score",
					direction : "DESC"
				}
			})
		});
		Gemma.SearchGrid.superclass.initComponent.call(this);
		this.getStore().on("load", this.handleLoadSuccess.createDelegate(this));

	},

	handleLoadSuccess : function(scope, b, arg) {
		Ext.DomHelper.overwrite("messages", scope.getCount() + " found");
		this.form.findById('submit-button').setDisabled(false);

		// If possible to expand all and not scroll then expand
		if (this.getStore().getCount() < Gemma.Search.MAX_AUTO_EXPAND_SIZE) {
			this.getView().expandAllGroups();
			return; // no point in checking below
		}

		// If there is only 1 returned group then expand it regardless of its
		// size.
		var lastResultClass = this.getStore().getAt(0).data.resultClass;
		var expand = true;

		for (var i = 1; i < this.getStore().getCount(); i++) {
			var record = this.getStore().getAt(i).data;
			if (record.resultClass !== lastResultClass) {
				expand = false;
			}
		}

		if (expand) {
			this.getView().expandAllGroups();
		}

	},

	handleLoadError : function(scope, b, message, exception) {
		Ext.DomHelper.overwrite('messages', {
			tag : 'img',
			src : '/Gemma/images/icons/warning.png'
		});
		Ext.DomHelper.append('messages', {
			tag : 'span',
			html : '&nbsp;&nbsp;' + message
		});
		this.form.findById('submit-button').setDisabled(false);
	},

	/*
	 * Renderers
	 */
	renderEntityClass : function(data, metadata, record, row, column, store) {
		var clazz = record.get("resultClass");
		if (clazz == "ExpressionExperimentValueObject") {
			return "Expression dataset";
		} else if (clazz == "CompositeSequence") {
			return "Probe";
		} else if (clazz == "ArrayDesignValueObject") {
			return "Array";
		} else if (/^BioSequence.*/.exec(clazz)) { // because we get proxies.
			return "Sequence";
		} else if (clazz == "Gene") {
			return "Gene";
		} else {
			return clazz;
		}
	},

	renderEntity : function(data, metadata, record, row, column, store) {
		var dh = Ext.DomHelper;
		var clazz = record.get("resultClass");
		if (clazz == "ExpressionExperimentValueObject") {
			return "<a href=\"/Gemma/expressionExperiment/showExpressionExperiment.html?id="
					+ data.id + "\">" + data.shortName + "</a> - " + data.name;
		} else if (clazz == "CompositeSequence") {
			return "<a href=\"/Gemma/compositeSequence/show.html?id=" + data.id
					+ "\">" + data.name + "</a> - " + data.description
					+ "; Array: " + data.arrayDesign.shortName;
		} else if (clazz == "ArrayDesignValueObject") {
			return "<a href=\"/Gemma/arrays/showArrayDesign.html?id=" + data.id
					+ "\">" + data.shortName + "</a>  " + data.name;
		} else if (/^BioSequence.*/.exec(clazz)) {
			return "<a href=\"/Gemma/genome/bioSequence/showBioSequence.html?id="
					+ data.id
					+ "\">"
					+ data.name
					+ "</a> - "
					+ data.taxon.commonName + " " + data.description;
		} else if (clazz == "Gene" || clazz == "PredictedGene"
				|| clazz == "ProbeAlignedRegion") {
			return "<a href=\"/Gemma/gene/showGene.html?id=" + data.id + "\">"
					+ data.officialSymbol + "</a>  - Species: "
					+ data.taxon.commonName + " Desc: " + data.officialName;
		} else if (clazz == "Bibliographicreference") {
			return "<a href=\"/Gemma/gene/showGene.html?id=" + data.id + "\">"
					+ data.title + "</a> [" + data.pubmedId + "]";
		}
	}

});
