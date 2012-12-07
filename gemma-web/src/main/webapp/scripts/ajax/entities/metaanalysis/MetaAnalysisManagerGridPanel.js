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

		var showSaveAsEvidenceWindow = function(id, metaAnalysis, numGenesAnalyzed) {
			metaAnalysis.numGenesAnalyzed = numGenesAnalyzed;

			var saveAsEvidenceWindow = new Gemma.MetaAnalysisEvidenceWindow({
				metaAnalysisId: id,
				metaAnalysis: metaAnalysis,
				defaultQvalueThreshold: DEFAULT_THRESHOLD
			});
			saveAsEvidenceWindow.show();
		};

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
			saveAsEvidence: function(id, numGenesAnalyzed) {
				var metaAnalysisFound = metaAnalysisCache[id];
				
				if (metaAnalysisFound) {
					showSaveAsEvidenceWindow(id, metaAnalysisFound, numGenesAnalyzed);
				} else {
					showLoadMask();
						DiffExMetaAnalyzerController.findDetailMetaAnalysisById(id, function(baseValueObject) {
							hideLoadMask();
							
							if (baseValueObject.errorFound) {						
								Gemma.alertUserToError(baseValueObject,
									Gemma.HelpText.WidgetDefaults.MetaAnalysisManagerGridPanel.ErrorTitle.saveMetaAnalysisAsEvidence);
							} else {
								metaAnalysisFound = baseValueObject.valueObject;
				
								metaAnalysisCache[id] = metaAnalysisFound;
								showSaveAsEvidenceWindow(id, metaAnalysisFound, numGenesAnalyzed);
							}			
						}.createDelegate(this));
				}
			},
			removeMetaAnalysis: function(id) {
				Ext.MessageBox.confirm('Confirm',
					'Are you sure you want to remove this meta-analysis?',
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
			},
			viewMetaAnalysis: function(id, analysisName, numGenesAnalyzed) {
				var metaAnalysisFound = metaAnalysisCache[id];
				if (metaAnalysisFound) {
					showMetaAnalysisWindow(metaAnalysisFound, analysisName, numGenesAnalyzed);
				} else {
					showLoadMask();
					DiffExMetaAnalyzerController.findDetailMetaAnalysisById(id, function(baseValueObject) {
						hideLoadMask();				
						
						if (baseValueObject.errorFound) {						
							Gemma.alertUserToError(baseValueObject, 
								Gemma.HelpText.WidgetDefaults.MetaAnalysisManagerGridPanel.ErrorTitle.viewMetaAnalysisDetail);
						} else {
							metaAnalysisFound = baseValueObject.valueObject;
				
							metaAnalysisCache[id] = metaAnalysisFound;
							showMetaAnalysisWindow(metaAnalysisFound, analysisName, numGenesAnalyzed);
						}			
					}.createDelegate(this));
				}
			},
			columns:[{
					header: 'Name',
					dataIndex: 'name',
					width: 0.3,
		            renderer: function(value, metadata, record, rowIndex, colIndex, store) {
						return value + ' ' + generateLink('viewMetaAnalysis(' + record.data.id + ', \'' + record.data.name + '\', ' + record.data.numGenesAnalyzed + ');',   
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
		            	
						adminLinks += generateLink('saveAsEvidence(' + record.data.id + ', ' +
							record.data.numGenesAnalyzed + ');', '/Gemma/images/logo/neurocarta-icon.png', 'Save as Neurocarta evidence') + ' ';
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
