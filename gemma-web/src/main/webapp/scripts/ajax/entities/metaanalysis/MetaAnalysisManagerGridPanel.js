/**
 * Meta-analysis manager displays all the available meta-analyses for the current user.
 * 
 * @author frances
 * @version $Id$
 */
Ext.namespace('Gemma');

Gemma.MetaAnalysisManagerGridPanel = Ext.extend(Ext.grid.GridPanel, {
	title: "Meta-analysis Manager",
    autoScroll: true,
    stripeRows: true,
    viewConfig: {
        forceFit: true,
		deferEmptyText: false,
		emptyText: 'No meta-analysis to display'
    },
    initComponent: function() {
    	var DEFAULT_THRESHOLD = 0.1;
    	
		var metaAnalysisCache = [];

    	var metaAnalysisWindow;
    	
		var generateLink = function(methodWithArguments, imageSrc, description, width, height) {
			return '<span class="link" onClick="return Ext.getCmp(\'' + this.getId() + '\').' + methodWithArguments +
						'"><img src="' + imageSrc + '" alt="' + description + '" ext:qtip="' + description + '" ' +
						((width && height) ?
							'width="' + width + '" height="' + height + '" ' :
							'') +
						'/></span>';
			
		}.createDelegate(this);
    	
		var showLoadMask = function(msg) {
			if (!this.loadMask) {
				this.loadMask = new Ext.LoadMask(this.getEl());
			}
			this.loadMask.msg = msg 
				? msg 
				: "Loading ...";
				
			this.loadMask.show();
		}.createDelegate(this);
		
		var hideLoadMask = function() {
			this.loadMask.hide();
		}.createDelegate(this);

    	var numberColumnRenderer = function(value, metaData, record, rowIndex, colIndex, store) {
        	metaData.attr = 'style="padding-right: 15px;"';
			return value;
    	};
    	
		var showMetaAnalysisWindow = function(metaAnalysis, analysisName, numGenesAnalyzed) {
			metaAnalysis.numGenesAnalyzed = numGenesAnalyzed;

			var viewMetaAnalysisWindow = new Gemma.MetaAnalysisWindow({
				title: 'View Meta-analysis for ' + analysisName,
				metaAnalysis: metaAnalysis,
				defaultQvalueThreshold: DEFAULT_THRESHOLD
			});  
			viewMetaAnalysisWindow.show();
		};

		var showSaveAsEvidenceWindow = function(metaAnalysis, id, analysisName, numGenesAnalyzed) {
			metaAnalysis.numGenesAnalyzed = numGenesAnalyzed;

			var saveAsEvidenceWindow = new Gemma.MetaAnalysisEvidenceWindow({
				metaAnalysisId: id,
				metaAnalysis: metaAnalysis,
				defaultQvalueThreshold: DEFAULT_THRESHOLD,
				title: 'Save ' + analysisName + ' as Neurocarta evidence',
				listeners: {
					evidenceSaved: function() {
						this.store.reload();
					},
					scope: this
				}
			});
			saveAsEvidenceWindow.show();
		};

		var showViewEvidenceWindow = function(metaAnalysis, id, analysisName, numGenesAnalyzed) {
			var record = this.getStore().getById(id);
			if (record != null) {
				metaAnalysis.numGenesAnalyzed = numGenesAnalyzed;
	
				var viewEvidenceWindow = new Gemma.MetaAnalysisEvidenceWindow({
					metaAnalysisId: id,
					metaAnalysis: metaAnalysis,
					defaultQvalueThreshold: DEFAULT_THRESHOLD,
					title: 'View Neurocarta evidence for ' + analysisName,
					diffExpressionEvidence: record.data.diffExpressionEvidence,
					modal: false,
					listeners: {
						evidenceRemoved: function() {
							this.store.reload();
						},
						scope: this
					}
				});
				viewEvidenceWindow.show();
			}
		};

		var processMetaAnalysis = function(id, errorDialogTitle, callback, args) {
			var metaAnalysisFound = metaAnalysisCache[id];
			if (metaAnalysisFound) {
				// Put metaAnalysisFound at the beginning of args.
				args.splice(0, 0, metaAnalysisFound);

				callback.apply(this, args);
			} else {
				showLoadMask();
				DiffExMetaAnalyzerController.findDetailMetaAnalysisById(id, function(baseValueObject) {
					hideLoadMask();				
					
					if (baseValueObject.errorFound) {						
						Gemma.alertUserToError(baseValueObject,	errorDialogTitle);
					} else {
						metaAnalysisCache[id] = baseValueObject.valueObject;

						// Put metaAnalysisFound at the beginning of args.
						args.splice(0, 0, metaAnalysisCache[id]); 
						
						callback.apply(this, args);
					}			
				}.createDelegate(this));
			}
		}.createDelegate(this);
		
		Ext.apply(this, {
			store: new Ext.data.JsonStore({
				autoLoad: true,
				proxy: new Ext.data.DWRProxy(DiffExMetaAnalyzerController.findMyMetaAnalyses),
				fields: [ 'id',
					{ name: 'name', sortType: Ext.data.SortTypes.asUCString }, // case-insensitively
					'description', 'numGenesAnalyzed', 'numResults', 'numResultSetsIncluded',
					'public', 'shared', 'ownedByCurrentUser', 'diffExpressionEvidence' ],
				idProperty: 'id',
				sortInfo: { field: 'name', direction: 'ASC'	}
			}),
			eval: function(request) {
				eval(request);
			},			
			removeMetaAnalysis: function(id) {
				var record = this.getStore().getById(id);
				if (record != null) {
					if (record.data.diffExpressionEvidence) {
						Ext.MessageBox.alert(Gemma.HelpText.WidgetDefaults.MetaAnalysisManagerGridPanel.ErrorTitle.removeMetaAnalysis,
							Gemma.HelpText.WidgetDefaults.MetaAnalysisManagerGridPanel.ErrorMessage.evidenceExist);
					} else {
						Ext.MessageBox.confirm('Confirm',
							'Are you sure you want to remove meta-analysis "' + record.data.name + '"?',
							function(button) {
								if (button === 'yes') {
									showLoadMask("Removing analysis ...");
				
									DiffExMetaAnalyzerController.removeMetaAnalysis(id, function(baseValueObject) {
										hideLoadMask();
				
										if (baseValueObject.errorFound) {						
											Gemma.alertUserToError(baseValueObject,
												Gemma.HelpText.WidgetDefaults.MetaAnalysisManagerGridPanel.ErrorTitle.removeMetaAnalysis);
										} else {
											this.store.reload();
										}
									}.createDelegate(this));
								}
							},
							this);
					}
				}
			},
			columns:[{
					header: 'Name',
					dataIndex: 'name',
					width: 0.3,
		            renderer: function(value, metadata, record, rowIndex, colIndex, store) {
						return value + ' ' + generateLink('eval(\'processMetaAnalysis(' +
								record.data.id + ', ' +
								'\\\'Cannot view meta-analysis\\\', ' +
								'showMetaAnalysisWindow, ' +
								'[ \\\'' + record.data.name + '\\\', ' + record.data.numGenesAnalyzed + ' ])\');',   
							'/Gemma/images/icons/magnifier.png', 'View included result sets and results', 10, 10);		            	
		            }
				}, {
					header: 'Description',
					dataIndex: 'description',
					width: 0.4
				}, {
					header: 'Genes analyzed',
					align: "right",
					dataIndex: 'numGenesAnalyzed',
					width: 0.2,
		            renderer: numberColumnRenderer
				}, {
					header: 'Genes with q-value < ' + DEFAULT_THRESHOLD,
					align: "right",
					dataIndex: 'numResults',
					width: 0.25,
		            renderer: numberColumnRenderer
				}, {
					header: 'Result sets included',
					align: "right",
					dataIndex: 'numResultSetsIncluded',
					width: 0.2,
		            renderer: numberColumnRenderer
				}, {
					header: 'Admin',
					id: 'id',
					width: 0.15,
		            renderer: function(value, metadata, record, rowIndex, colIndex, store) {
		            	var adminLinks = '';

						if (record.data.diffExpressionEvidence == null) {
							adminLinks += generateLink('eval(\'processMetaAnalysis(' +
									record.data.id + ', ' +
									'\\\'Cannot save meta-analysis as Neurocarta evidence\\\', ' +
									'showSaveAsEvidenceWindow, ' +
									'[ ' + record.data.id + ', \\\'' + record.data.name + '\\\', ' + record.data.numGenesAnalyzed + ' ])\');',   
								'/Gemma/images/icons/neurocarta-add.png', 'Save as Neurocarta evidence') + ' ';		            	
						} else {
							adminLinks += generateLink('eval(\'processMetaAnalysis(' +
									record.data.id + ', ' +
									'\\\'Cannot view meta-analysis\\\', ' +
									'showViewEvidenceWindow, ' +
									'[ ' + record.data.id + ', \\\'' + record.data.name + '\\\', ' + record.data.numGenesAnalyzed + ' ])\');',   
								'/Gemma/images/icons/neurocarta-check.png', 'View Neurocarta evidence') + ' ';		            	
						}

	            		adminLinks += generateLink('removeMetaAnalysis(' + record.data.id + ');', '/Gemma/images/icons/cross.png', 'Remove meta-analysis') + ' ';
	            		
	            		adminLinks += Gemma.SecurityManager.getSecurityLink(
							'ubic.gemma.model.analysis.expression.diff.GeneDifferentialExpressionMetaAnalysisImpl',
							record.data.id,
							record.data.public,
							record.data.shared,
							record.data.ownedByCurrentUser, // Can current user edit?
							null,
							null,
							null, // It is title in Security dialog. Specify null to use the object name.
							record.data.ownedByCurrentUser); // Is current user owner? 

						return adminLinks;
		            },
					sortable: false
				}],
			tbar: [{
					handler: function() {
						if (!metaAnalysisWindow || metaAnalysisWindow.hidden) {
							metaAnalysisWindow = new Gemma.MetaAnalysisWindow({
								title: 'Add New Meta-analysis',
								defaultQvalueThreshold: DEFAULT_THRESHOLD,
								listeners: {
									resultSaved: function() {
										metaAnalysisWindow.close();
										this.store.reload();
									},
									scope: this
								}
							});  
						}

						metaAnalysisWindow.show();
					},
					scope: this,
					icon: "/Gemma/images/icons/add.png",
					tooltip: "Add new meta-analysis"
				}]
		});

		Gemma.MetaAnalysisManagerGridPanel.superclass.initComponent.call(this);
    }
});
