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
			if (this.form.restoreState()) {
				this.search();
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
			var searchGeneSets = Ext.getCmp('search-genesets-chkbx').getValue();
			var searchEESets = Ext.getCmp('search-eesets-chkbx').getValue();

			var searchDatabase = true;
			var searchIndices = true;
			var searchCharacteristics = true;
			if (Ext.get('hasAdmin').getValue()) {
				searchDatabase = Ext.getCmp('search-database-chkbx').getValue();
				searchIndices = Ext.getCmp('search-indices-chkbx').getValue();
				searchCharacteristics = Ext.getCmp('search-characteristics-chkbx').getValue();
			}

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
			if (searchGeneSets) {
				scopes = scopes + "M";
			}
			if (searchEESets) {
				scopes = scopes + "N";
			}

			this.resultGrid.getStore().load({
						params : [{
									query : query,
									searchProbes : searchProbes,
									searchBioSequences : searchSequences,
									searchArrays : searchArrays,
									searchExperiments : searchExperiments,
									searchGenes : searchGenes,
									searchGeneSets : searchGeneSets,
									searchExperimentSets : searchEESets,
									useDatabase : searchDatabase,
									useIndices : searchIndices,
									useCharacteristics : searchCharacteristics
								}]
					});

			if (typeof pageTracker !== 'undefined') {
				pageTracker._trackPageview("/Gemma/searcher.search?query=" + escape(query) + scopes);
			}

			Ext.DomHelper.overwrite('messages', "");
			this.form.findById('submit-button').setDisabled(true);
			Ext.DomHelper.overwrite('search-bookmark', {
						tag : 'a',
						href : "/Gemma/searcher.html?query=" + escape(query) + scopes,
						html : 'Bookmarkable link'
					});
		}
	};
};

Gemma.Search.MAX_AUTO_EXPAND_SIZE = 15;

Gemma.SearchForm = Ext.extend(Ext.form.FormPanel, {
			frame : true,
			autoHeight : true,
			width : 300,

			/**
			 * Restore state ... fixme.
			 */
			restoreState : function() {

				var url = document.URL;
				if (url.indexOf("?") > -1) {
					var sq = url.substr(url.indexOf("?") + 1);
					var params = Ext.urlDecode(sq);

					if ((params.termUri) && (params.termUri.length !== 0)) {
						this.form.findField('query').setValue(params.termUri);
					}
					else 
						if (params.query) {
							this.form.findField('query').setValue(params.query);
						}
						else {
							// NO Query object (just a random ? in string uri)
							return false;
						}

					if (params.scope) {
						if (params.scope.indexOf('E') > -1) {
							Ext.getCmp('search-exps-chkbx').setValue(true);
						} else {
							Ext.getCmp('search-exps-chkbx').setValue(false);
						}
						if (params.scope.indexOf('A') > -1) {
							Ext.getCmp('search-ars-chkbx').setValue(true);
						} else {
							Ext.getCmp('search-ars-chkbx').setValue(false);
						}
						if (params.scope.indexOf('P') > -1) {
							Ext.getCmp('search-prbs-chkbx').setValue(true);
						} else {
							Ext.getCmp('search-prbs-chkbx').setValue(false);
						}
						if (params.scope.indexOf('G') > -1) {
							Ext.getCmp('search-genes-chkbx').setValue(true);
						} else {
							Ext.getCmp('search-genes-chkbx').setValue(false);
						}
						if (params.scope.indexOf('S') > -1) {
							Ext.getCmp('search-seqs-chkbx').setValue(true);
						} else {
							Ext.getCmp('search-seqs-chkbx').setValue(false);
						}
						if (params.scope.indexOf('M') > -1) {
							Ext.getCmp('search-genesets-chkbx').setValue(true);
						} else {
							Ext.getCmp('search-genesets-chkbx').setValue(false);
						}
						if (params.scope.indexOf('N') > -1) {
							Ext.getCmp('search-eesets-chkbx').setValue(true);
						} else {
							Ext.getCmp('search-eesets-chkbx').setValue(false);
						}
					}
				} else {
					return false;
				}

				return true;

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
																		if (e.getKey() === e.ENTER) {
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
										collapsed : true,
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
												}, {
													id : 'search-genesets-chkbx',
													name : "searchGeneSets",
													boxLabel : "Gene sets",
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
													id : 'search-eesets-chkbx',
													name : "searchEESets",
													boxLabel : "Experiment sets",
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

				var showAdvancedOptions = Ext.get("hasAdmin").getValue();

				if (showAdvancedOptions) {
					var advancedOptions = {
						width : 180,
						xtype : 'fieldset',
						defaultType : 'checkbox',
						collapsible : true,
						collapsed : true,
						autoHeight : true,
						title : 'Advanced options',
						items : [{
									id : 'search-database-chkbx',
									name : "searchDatabase",
									boxLabel : "Search database",
									hideLabel : true,
									checked : true
								}, {
									id : 'search-indices-chkbx',
									name : "searchIndices",
									boxLabel : "Seach indices",
									hideLabel : true,
									checked : true
								}, {
									id : 'search-characteristics-chkbx',
									name : "searchCharacteristics",
									boxLabel : "Search characteristics",
									hideLabel : true,
									checked : true
								}]
					};

					this.items.push(advancedOptions);
				}

				Gemma.SearchForm.superclass.initComponent.call(this);
				this.addEvents("search");

				this.restoreState();
			}

		});

Gemma.SearchGrid = Ext.extend(Ext.grid.GridPanel, {

	width : 800,
	height : 500,
	loadMask : true,
	stripeRows : true,
	collapsible : false,
	stateful : false,
	title : "Search results",
	selModel : new Ext.grid.RowSelectionModel({
				singleSelect : true
			}),
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
				name : "resultObject",
				sortType : this.sortInfo
			}, {
				name : "highlightedText",
				type : "string"
			}, {
				name : "indexSearchResult",
				type : "boolean"
			}]),

	toggleDetails : function(btn, pressed) {
		var view = this.getView();
		view.showPreview = pressed;
		view.refresh();
	},

	getSearchFun : function(text) {
		var value = new RegExp(Ext.escapeRe(text), 'i');
		return function(r, id) {
			var highlightedText = r.get("highlightedText");

			if (value.test(highlightedText)) {
				return true;
			}

			var clazz = r.get("resultClass");
			var obj = r.data.resultObject;
			if (clazz === "ExpressionExperimentValueObject") {
				return value.test(obj.shortName) || value.test(obj.name);
			} else if (clazz === "CompositeSequence") {
				return value.test(obj.name) || value.test(obj.description) || value.test(obj.arrayDesign.shortName);
			} else if (clazz === "ArrayDesignValueObject") {
				return value.test(obj.name) || value.test(obj.description);
			} else if (/^BioSequence.*/.exec(clazz)) { // because we get
				// proxies.
				return value.test(obj.name) || value.test(obj.description) || value.test(obj.taxon.commonName);
			} else if (clazz === "Gene" || clazz === "PredictedGene" || clazz === "ProbeAlignedRegion") {
				return value.test(obj.officialSymbol) || value.test(obj.officialName)
						|| value.test(obj.taxon.commonName);
			} else {
				return false;
			}
		};
	},

	searchForText : function(button, keyev) {
		var text = Ext.getCmp('search-in-grid').getValue();
		if (text.length < 2) {
			this.getStore().clearFilter();
			return;
		}
		this.getStore().filterBy(this.getSearchFun(text), this, 0);
	},

	initComponent : function() {
		var proxy = new Ext.data.DWRProxy(SearchService.search);

		proxy.on("loadexception", this.handleLoadError.createDelegate(this));

		Ext.apply(this, {
					tbar : new Ext.Toolbar({
								items : [{
											pressed : true,
											enableToggle : true,
											text : 'Toggle details',
											tooltip : "Click to show/hide details for results",
											cls : 'x-btn-text-icon details',
											toggleHandler : this.toggleDetails.createDelegate(this)
										}, ' ', ' ', {
											xtype : 'textfield',
											id : 'search-in-grid',
											tabIndex : 1,
											enableKeyEvents : true,
											emptyText : 'Find in results',
											listeners : {
												"keyup" : {
													fn : this.searchForText.createDelegate(this),
													scope : this,
													options : {
														delay : 100
													}
												}
											}
										}]
							}),
					view : new Ext.grid.GroupingView({
								enableRowBody : true,
								showPreview : true,
								getRowClass : function(record, index, p, store) {
									if (this.showPreview) {
										p.body = "<p class='search-result-body' >" + record.get("highlightedText")
												+ "</p>"; // typo.css
									}
									return '';
								},
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
								header : "Matched via:",
								width : 180,
								hidden : true,
								dataIndex : "highlightedText",
								tooltip : "The text or part of the result that matched the search",
								sortable : true
							}],
					store : new Ext.data.GroupingStore({
								proxy : proxy,
								reader : new Ext.data.JsonReader({
											id : "id",
											root : "records",
											totalProperty : "totalRecords"
										}, this.record),
								remoteSort : false,
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
		var i = 1;
		for (i; i < this.getStore().getCount(); i++) {
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
		if (clazz === "ExpressionExperimentValueObject") {
			return "Expression dataset";
		} else if (clazz === "CompositeSequence") {
			return "Probe";
		} else if (clazz === "ArrayDesignValueObject") {
			return "Array";
		} else if (/^BioSequence.*/.exec(clazz)) { // because we get proxies.
			return "Sequence";
		} else if (clazz === "GeneValueObject") {
			return "Gene";
		} else if (clazz === "GeneSetValueObject") {
			return "Gene set";
		} else if (clazz === "ExpressionExperimentSetValueObject") {
			return "Experiment set";
		} else {
			return clazz;
		}
	},

	sortInfo : function(record) {
		var clazz = record.resultsClass;
		if (clazz === "ExpressionExperimentValueObject") {
			return record.shortName;
		} else if (clazz === "CompositeSequence") {
			return record.name;
		} else if (clazz === "ArrayDesignValueObject") {
			return record.shortName;
		} else if (/^BioSequence.*/.exec(clazz)) { // because we get proxies.
			return record.name;
		} else if (clazz === "GeneValueObject" || clazz === 'GeneSetValueObject'
				|| clazz === 'ExpressionExperimentSetValueObject') {
			return record.name;
		} else {
			return clazz;
		}
	},

	renderEntity : function(data, metadata, record, row, column, store) {
		var dh = Ext.DomHelper;
		var clazz = record.get("resultClass");
		if (clazz === "ExpressionExperimentValueObject") {
			return "<a href=\"/Gemma/expressionExperiment/showExpressionExperiment.html?id="
					+ (data.sourceExperiment ? data.sourceExperiment : data.id) + "\">" + data.shortName + "</a> - "
					+ data.name;
		} else if (clazz === "CompositeSequence") {
			return "<a href=\"/Gemma/compositeSequence/show.html?id=" + data.id + "\">" + data.name + "</a> - "
					+ (data.description ? data.description : "") + "; Array: " + data.arrayDesign.shortName;
		} else if (clazz === "ArrayDesignValueObject") {
			return "<a href=\"/Gemma/arrays/showArrayDesign.html?id=" + data.id + "\">" + data.shortName + "</a>  "
					+ data.name;
		} else if (/^BioSequence.*/.exec(clazz)) {
			return "<a href=\"/Gemma/genome/bioSequence/showBioSequence.html?id=" + data.id + "\">" + data.name
					+ "</a> - " + data.taxon.commonName + " " + (data.description ? data.description : "");
		} else if (clazz === "GeneValueObject" || clazz === "PredictedGene" || clazz === "ProbeAlignedRegion") {
			return "<a href=\"/Gemma/gene/showGene.html?id=" + data.id + "\">" + data.officialSymbol
					+ "</a>  - Species: " + data.taxonCommonName + " Desc: " + data.officialName;
		} else if (clazz === "Bibliographicreference") {
			return "<a href=\"/Gemma/gene/showGene.html?id=" + data.id + "\">" + data.title + "</a> [" + data.pubmedId
					+ "]";
		} else if (clazz === "ExpressionExperimentSetValueObject") {

			/*
			 * TODO add links.
			 */
			return data.name;
		} else if (clazz === "GeneSetValueObject") {

			/*
			 * TODO add links
			 */
			return data.name;
		}
	}

});
