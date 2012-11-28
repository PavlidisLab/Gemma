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
	loadMask: true,
    viewConfig: {
        forceFit: true,
		deferEmptyText: false,
		emptyText: 'No meta-analysis to display'
    },
	removeMetaAnalysis: function(id) {
		Ext.MessageBox.confirm('Confirm',
			'Are you sure you want to remove this meta-analysis?',
			function(button) {
				if (button === 'yes') {
					DiffExMetaAnalyzerController.removeMetaAnalysis(id, function(baseValueObject) {
						if (baseValueObject.errorFound) {						
							Gemma.alertUserToError(baseValueObject, [ 'remove meta-analysis', 'meta-analysis' ]);
						} else {
							this.store.reload();
						}
					}.createDelegate(this));
				}
			},
			this);
	},
	viewAnalysis: function(id, analysisName, numGenesAnalyzed) {
		var recordData = this.store.getById(id).data;

		if (!this.loadMask) {
			this.loadMask = new Ext.LoadMask(this.getEl(), {
				msg: "Loading ..."
			});
		}
		this.loadMask.show();

		DiffExMetaAnalyzerController.findDetailMetaAnalysisById(recordData.id, function(baseValueObject) {
			this.loadMask.hide();			
			
			if (baseValueObject.errorFound) {						
				Gemma.alertUserToError(baseValueObject, [ 'view meta-analysis detail', 'meta-analysis' ]);
			} else {
				var analysis = baseValueObject.valueObject;
				
				analysis.numGenesAnalyzed = numGenesAnalyzed;
	
				var viewMetaAnalysisWindow = new Gemma.MetaAnalysisWindow({
					title: 'View Meta-analysis for ' + analysisName,
					metaAnalysis: analysis
					
				});  
				viewMetaAnalysisWindow.show();
			}			
		}.createDelegate(this));
	},
    initComponent: function() {
    	var metaAnalysisWindow;
    	
		var generateLink = function(methodWithArguments, imageSrc, description, width, height) {
			return '<span class="link" onClick="return Ext.getCmp(\'' + this.getId() + '\').' + methodWithArguments +
						'"><img src="' + imageSrc + '" alt="' + description + '" ext:qtip="' + description + '" ' +
						((width && height) ?
							'width="' + width + '" height="' + height + '" ' :
							'') +
						'/></span>';
			
		}.createDelegate(this);
    	
    	var numberColumnRenderer = function(value, metaData, record, rowIndex, colIndex, store) {
        	metaData.attr = 'style="padding-right: 15px;"';
			return value;
    	};
		
		Ext.apply(this, {
			store: new Ext.data.JsonStore({
				autoLoad: true,
				proxy: new Ext.data.DWRProxy(DiffExMetaAnalyzerController.findMyMetaAnalyses),
				fields: [ 'id',
					{ name: 'name', sortType: Ext.data.SortTypes.asUCString }, // case-insensitively
					'description', 'numGenesAnalyzed', 'numResults', 'numResultSetsIncluded',
					'public', 'shared', 'ownedByCurrentUser' ],
				idProperty: 'id',
				sortInfo: { field: 'name', direction: 'ASC'	}
			}),
			columns:[{
					header: 'Name',
					dataIndex: 'name',
					width: 0.4,
		            renderer: function(value, metadata, record, rowIndex, colIndex, store) {
						return value + ' ' + generateLink('viewAnalysis(' + record.data.id + ', \'' + record.data.name + '\', ' + record.data.numGenesAnalyzed + ');',   
							'/Gemma/images/icons/magnifier.png', 'View included result sets and results', 10, 10);		            	
		            }
				}, {
					header: 'Description',
					dataIndex: 'description',
					width: 0.75
				}, {
					header: 'Genes analyzed',
					align: "right",
					dataIndex: 'numGenesAnalyzed',
					width: 0.2,
		            renderer: numberColumnRenderer
				}, {
					header: 'Genes with q-value < 0.1',
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
					width: 0.4,
		            renderer: function(value, metadata, record, rowIndex, colIndex, store) {
		            	var adminLinks = '';
		            	
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
