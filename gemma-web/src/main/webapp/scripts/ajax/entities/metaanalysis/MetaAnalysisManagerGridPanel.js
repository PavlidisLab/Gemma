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
					DiffExMetaAnalyzerController.removeMetaAnalysis(id, function() {
						this.store.reload();
					}.createDelegate(this));
// TODO: the following code was copied from phenotype section. Should do something similar.					
//							DiffExMetaAnalyzerController.removeMetaAnalysis(id, function(validateEvidenceValueObject) {
//								if (validateEvidenceValueObject == null) {
//									this.fireEvent('phenotypeAssociationChanged');
//								} else {
//									if (validateEvidenceValueObject.evidenceNotFound) {
//										// We still need to fire event to let listeners know that it has been removed.
//										this.fireEvent('phenotypeAssociationChanged');
//										Ext.Msg.alert('Evidence already removed', 'This evidence has already been removed by someone else.');
//									} else {
//										Ext.Msg.alert('Cannot remove evidence', Gemma.convertToEvidenceError(validateEvidenceValueObject).errorMessage,
//											function() {
//												if (validateEvidenceValueObject.userNotLoggedIn) {
//													Gemma.AjaxLogin.showLoginWindowFn();
//												}
//											}
//										);
//									}
//								}
//							}.createDelegate(this));
				}
			},
			this);
	},
	viewAnalysis: function(id) {
		var recordData = this.store.getById(id).data;

		if (!this.loadMask) {
			this.loadMask = new Ext.LoadMask(this.getEl(), {
				msg: "Loading ..."
			});
		}
		this.loadMask.show();

		DiffExMetaAnalyzerController.getMetaAnalysis(recordData.id, function(analysis) {
			this.loadMask.hide();			
			
			var viewMetaAnalysisWindow = new Gemma.MetaAnalysisWindow({
				title: 'View Meta-analysis for ' + analysis.name,
				metaAnalysis: analysis
				
			});  
			viewMetaAnalysisWindow.show();
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
    	
    	
		Ext.apply(this, {
			store: new Ext.data.JsonStore({
				autoLoad: true,
				proxy: new Ext.data.DWRProxy(DiffExMetaAnalyzerController.getMyMetaAnalyses),
				fields: [ 'id',
					{ name: 'name', sortType: Ext.data.SortTypes.asUCString }, // case-insensitively
					'description', 'numGenesAnalyzed', 'numResults', 'numResultSetsIncluded' ],
				idProperty: 'id',
				sortInfo: { field: 'name', direction: 'ASC'	}
			}),
			columns:[{
					header: 'Name',
					dataIndex: 'name',
					width: 0.4,
		            renderer: function(value, metadata, record, rowIndex, colIndex, store) {
						return value + ' ' + generateLink('viewAnalysis(' + record.data.id + ');',
							'/Gemma/images/icons/magnifier.png', 'View included result sets and results', 10, 10);		            	
		            }
				}, {
					header: 'Description',
					dataIndex: 'description',
					width: 0.75
				}, {
					header: 'Genes analyzed',
					dataIndex: 'numGenesAnalyzed',
					width: 0.2
				}, {
					header: 'Genes with q-value < 0.1',
					dataIndex: 'numResults',
					width: 0.25
				}, {
					header: 'Result sets included',
					dataIndex: 'numResultSetsIncluded',
					width: 0.2
				}, {
					header: 'Admin',
					id: 'id',
					width: 0.4,
		            renderer: function(value, metadata, record, rowIndex, colIndex, store) {
		            	var adminLinks = '';
		            	
	            		adminLinks += generateLink('removeMetaAnalysis(' + record.data.id + ');', '/Gemma/images/icons/cross.png', 'Remove meta-analysis');
		            	
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
