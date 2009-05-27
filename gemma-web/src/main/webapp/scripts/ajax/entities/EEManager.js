/**
 * Common tasks to run on experiments. To use this, construct it with a id defined in the config (e.g., {id :
 * 'eemanager}). Then you can use things like onClick=Ext.getCmp('eemanager').updateEEReport(id).
 * 
 * @class Gemma.EEManager
 * @extends Ext.Component
 */
Gemma.EEManager = Ext.extend(Ext.Component, {

	record : Ext.data.Record.create([{
				name : "id",
				type : "int"
			}, {
				name : "shortName"
			}, {
				name : "name"
			}, {
				name : "arrayDesignCount",
				type : "int"
			}, {
				name : "technologyType"
			}, {
				name : "hasBothIntensities",
				type : 'bool'
			}, {
				name : "hasEitherIntensity",
				type : 'bool'
			}, {
				name : "bioAssayCount",
				type : "int"
			}, {
				name : "processedExpressionVectorCount",
				type : "int"
			}, {
				name : "externalUri"
			}, {
				name : "description"
			}, {
				name : "taxon"
			}, {
				name : "numAnnotations"
			}, {
				name : "numPopulatedFactors"
			}, {
				name : "isPublic"
			}, {
				name : "sourceExperiment"
			}, {
				name : "coexpressionLinkCount"
			}, {
				name : "diffExpressedProbes"	
			}, {
				name : "validatedFlag"
			}, {
				name : "troubleFlag"
			}, {
				name : "missingValueAnalysisEventType"
			}, {
				name : "processedDataVectorComputationEventType"
			}, {
				name : "dateCreated",
				type : 'date'
			}, {
				name : "dateProcessedDataVectorComputation",
				type : 'date'
			}, {
				name : "dateMissingValueAnalysis",
				type : 'date'
			}, {
				name : "dateDifferentialAnalysis",
				type : 'date'
			}, {
				name : "dateLastUpdated",
				type : 'date'
			}, {
				name : "dateLinkAnalysis",
				type : 'date'
			}, {
				name : "linkAnalysisEventType"
			}, {
				name : "processedDataVectorComputationEventType"
			}, {
				name : "missingValueAnalysisEventType"
			}, {
				name : "differentialAnalysisEventType"
			}]),

	updateEEReport : function(id) {
		var callParams = [];
		callParams.push(id);
		callParams.push({
					callback : function(data) {
						var k = new Gemma.WaitHandler();
						this.relayEvents(k, ['done']);
						k.handleWait(data, false);
						k.on('done', function(payload) {
									this.fireEvent('reportUpdated', payload)
								});
					}.createDelegate(this)
				});

		ExpressionExperimentController.updateReport.apply(this, callParams);
	},

	updateAllEEReports : function() {
		var callParams = [];
		callParams.push({
					callback : function(data) {
						var k = new Gemma.WaitHandler();
						k.handleWait(data, true);
						this.relayEvents(k, ['done']);
						k.on('done', function(payload) {
									this.fireEvent('reportUpdated', payload)
								});
					}.createDelegate(this)
				});

		ExpressionExperimentController.updateAllReports.apply(this, callParams);
	},

	tagger : function(id) {

		var annotator = new Ext.Panel({
					id : 'annotator-wrap',
					title : "Tags",
					collapsible : false,
					stateful : false,
					bodyBorder : false,
					width : 600,
					height : 200,
					layout : 'fit',
					items : [new Gemma.AnnotationGrid({
								id : 'annotator-grid',
								readMethod : ExpressionExperimentController.getAnnotation,
								writeMethod : OntologyService.saveExpressionExperimentStatement,
								removeMethod : OntologyService.removeExpressionExperimentStatement,
								readParams : [{
											id : id
										}],
								editable : this.editable,
								showParent : false,
								mgedTermKey : "experiment",
								entId : id
							})]
				});
		this.change = false;
		Ext.getCmp('annotator-grid').on('refresh', function() {
					this.change = true;
				}.createDelegate(this));
		var w = new Ext.Window({
					modal : true,
					stateful : false,
					layout : 'fit',
					items : [annotator],
					buttons : [{
						text : 'Help',
						handler : function() {
							Ext.Msg
									.alert(
											"Help with tagging",
											"Select a 'category' for the term; then enter a term, "
													+ "choosing from existing terms if possible. "
													+ "Click 'create' to save it. You can also edit existing terms;"
													+ " click 'save' to make the change stick, or 'delete' to remove a selected tag.");
						}
					}, {
						text : 'Done',
						handler : function() {
							if (this.change) {
								/* Update the display of the tags. */
								this.fireEvent('tagsUpdated');
							}
							w.hide();
						},
						scope : this
					}]

				});

		w.show();
	},

	deleteExperiment : function(id) {
		Ext.Msg.show({
					title : 'Really delete?',
					msg : 'Are you sure you want to delete the experiment? This cannot be undone.',
					buttons : Ext.Msg.YESNO,
					fn : function(btn, text) {
						if (btn == 'yes') {
							var callParams = []
							callParams.push(id);
							Ext.getBody().mask();
							callParams.push({
										callback : function(data) {
											var k = new Gemma.WaitHandler();
											k.handleWait(data, true);
											this.relayEvents(k, ['done']);
											Ext.getBody().unmask();
											k.on('done', function(payload) {
														this.fireEvent('deleted', payload)
													});
										}.createDelegate(this),
										errorHandler : function(error) {
											Ext.Msg.alert("Deletion failed", error);
											Ext.getBody().unmask();
										}.createDelegate(this)
									});
							ExpressionExperimentController.deleteById.apply(this, callParams);
						}
					},
					scope : this,
					animEl : 'elId',
					icon : Ext.MessageBox.WARNING
				});
	},

	doLinks : function(id) {
		Ext.Msg.show({
					title : 'Link analysis',
					msg : 'Please confirm. Previous analysis results will be deleted.',
					buttons : Ext.Msg.YESNO,
					fn : function(btn, text) {
						if (btn == 'yes') {
							var callParams = []
							callParams.push(id);
							Ext.getBody().mask();
							callParams.push({
										callback : function(data) {
											var k = new Gemma.WaitHandler();
											k.handleWait(data, true);
											this.relayEvents(k, ['done']);
											Ext.getBody().unmask();
											k.on('done', function(payload) {
														this.fireEvent('link', payload)
													});
										}.createDelegate(this),
										errorHandler : function(error) {
											Ext.Msg.alert("Link analysis failed", error);
											Ext.getBody().unmask();
										}.createDelegate(this)
									});
							LinkAnalysisController.run.apply(this, callParams);
						}
					},
					scope : this,
					animEl : 'elId',
					icon : Ext.MessageBox.WARNING
				});
	},

	doMissingValues : function(id) {
		Ext.Msg.show({
					title : 'Missing value analysis',
					msg : 'Please confirm. Previous analysis results will be deleted.',
					buttons : Ext.Msg.YESNO,
					fn : function(btn, text) {
						if (btn == 'yes') {
							var callParams = []
							callParams.push(id);
							Ext.getBody().mask();
							callParams.push({
										callback : function(data) {
											var k = new Gemma.WaitHandler();
											k.handleWait(data, true);
											this.relayEvents(k, ['done']);
											Ext.getBody().unmask();
											k.on('done', function(payload) {
														this.fireEvent('missingValue', payload)
													});
										}.createDelegate(this),
										errorHandler : function(error) {
											Ext.Msg.alert("Missing value analysis failed", error);
											Ext.getBody().unmask();
										}.createDelegate(this)
									});
							TwoChannelMissingValueController.run.apply(this, callParams);
						}
					},
					scope : this,
					animEl : 'elId',
					icon : Ext.MessageBox.WARNING
				});
	},

	doDifferential : function(id) {

		/*
		 * Do an analysis interactively.
		 */
		var customize = function(analysisInfo) {

			var factors = analysisInfo.factors;
			var proposedAnalysis = analysisInfo.type;

			var canDoInteractions = (proposedAnalysis == 'TWIA') || factors.length > 2;

			/*
			 * DifferentialExpressionAnalysisSetupWindow - to be refactored.
			 */
			var deasw = new Ext.Window({
						modal : true,
						stateful : false,
						resizable : false,
						autoHeight : true,
						plain : true,
						title : "Differential analysis settings",
						items : [{
									xtype : 'form',
									autoHeight : true,
									items : [{
												xtype : 'fieldset',
												title : "Select the factor(s) to use",
												autoHeight : true,
												labelWidth : 200,
												id : 'diff-ex-analysis-customize-factors'
											}, {
												xtype : 'fieldset',
												labelWidth : 200,
												autoHeight : true,
												hidden : !canDoInteractions,
												items : [{
															xtype : 'checkbox',
															id : 'diff-ex-analysis-customize-include-interactions-checkbox',
															fieldLabel : 'Include interactions'
														}]
											}]
								}],
						buttons : [{
							text : 'Proceed',
							id : 'diff-ex-customize-proceed-button',
							disabled : false,
							scope : this,
							handler : function(btn, text) {

								var includeInteractions = Ext
										.getCmp('diff-ex-analysis-customize-include-interactions-checkbox').getValue();

								/*
								 * Get the factors the user checked. See checkbox creation code below.
								 */
								var factorsToUseIds = [];
								if (factors) {
									for (var i = 0; i < factors.length; i++) {
										var f = factors[i];
										if (!f.name) {
											continue;
										}
										var checked = Ext.getCmp(f.id + '-factor-checkbox').getValue();
										if (checked) {
											factorsToUseIds.push(f.id);
										}
									}
								}

								/*
								 * Pass back the factors to be used, and the choice of whether interactions are to be
								 * used.
								 */
								var callParams = [];
								callParams.push(id);
								callParams.push(factorsToUseIds);
								callParams.push(includeInteractions);
								Ext.getBody().mask();
								callParams.push({
											callback : function(data) {
												var k = new Gemma.WaitHandler();
												k.handleWait(data, true);
												deasw.relayEvents(k, ['done']);
												Ext.getBody().unmask();
												k.on('done', function(payload) {
															this.fireEvent('differential', payload)
														});
											}.createDelegate(this),
											errorHandler : function(error) {
												Ext.Msg.alert("Diff. Analysis failed", error);
												Ext.getBody().unmask();
											}.createDelegate(this)
										});

								DifferentialExpressionAnalysisController.runCustom.apply(this, callParams);
								deasw.close();
							}
						}, {
							text : 'Cancel',
							handler : function() {
								deasw.close();
							}
						}]
					});

			deasw.doLayout();

			/*
			 * Create the checkboxes for user choice of factors.
			 */
			if (factors) {
				for (var i = 0; i < factors.length; i++) {
					var f = factors[i];
					if (!f.name) {
						continue;
					}
					Ext.getCmp('diff-ex-analysis-customize-factors').add(new Ext.form.Checkbox({
								fieldLabel : f.name,
								labelWidth : 180,
								id : f.id + '-factor-checkbox',
								tooltip : f.name
							}));
				}
			}
			deasw.doLayout();
			deasw.show();

		};

		/*
		 * Callback for analysis type determination. This gets the type of analysis, if it can be determined. If the
		 * type is non-null, then just ask the user for confirmation. If they say no, or the type is null, show them the
		 * DifferentialExpressionAnalysisSetupWindow.
		 */
		var cb = function(analysisInfo) {
			if (analysisInfo.type) {
				var customizable = false;
				var analysisType = '';
				if (analysisInfo.type === 'TWIA') {
					analysisType = 'Two-way ANOVA with interactions';
					customizable = true;
				} else if (analysisInfo.type === 'TWA') {
					analysisType = 'Two-way ANOVA without interactions';
					customizable = true;
				} else if (analysisInfo.type === 'TTEST') {
					analysisType = 'T-test';
				} else if (analysisInfo.type === 'OWA') {
					analysisType = 'One-way ANOVA';
				}

				// ask for confirmation.
				var w = new Ext.Window({
							autoCreate : true,
							resizable : false,
							constrain : true,
							constrainHeader : true,
							minimizable : false,
							maximizable : false,
							stateful : false,
							modal : true,
							shim : true,
							buttonAlign : "center",
							width : 400,
							height : 100,
							minHeight : 80,
							plain : true,
							footer : true,
							closable : true,
							title : 'Differential expression analysis',
							html : 'Please confirm. The analysis performed will be a ' + analysisType
									+ '. Previous analysis results for this experiment will be deleted.',
							buttons : [{
										text : 'Proceed',										
										handler : function(btn, text) {
											var callParams = []
											callParams.push(id);
											Ext.getBody().mask();
											callParams.push({
														callback : function(data) {
															var k = new Gemma.WaitHandler();
															k.handleWait(data, true);
															this.relayEvents(k, ['done']);
															Ext.getBody().unmask();
															k.on('done', function(payload) {
																		this.fireEvent('differential', payload)
																	});
														}.createDelegate(this),
														errorHandler : function(error) {
															Ext.Msg.alert("Diff. Analysis failed", error);
															Ext.getBody().unmask();
														}.createDelegate(this)
													});

											DifferentialExpressionAnalysisController.run.apply(this, callParams);
											w.close();
										}
									}, {
										text : 'Cancel',
										handler : function() {
											w.close();
										}
									}, {
										disabled : !customizable,
										hidden : !customizable,
										text : 'Customize',
										handler : function() {
											w.close();
											customize(analysisInfo);
										}
									}],
							iconCls : Ext.MessageBox.QUESTION
						});

				w.show();

			} else {
				/*
				 * System couldn't guess the analysis type, so force user to customize.
				 */
				customize(analysisInfo);
			}
		};

		/*
		 * Get the analysis type.
		 */
		var eh = function(error) {
			Ext.Msg.alert("There was an error", error);
		};
		DifferentialExpressionAnalysisController.determineAnalysisType(id, {
					callback : cb,
					errorhandler : eh
				});

	},

	doProcessedVectors : function(id) {
		Ext.Msg.show({
					title : 'Processed vector analysis',
					msg : 'Please confirm. Any existing processed vectors will be deleted.',
					buttons : Ext.Msg.YESNO,
					fn : function(btn, text) {
						if (btn == 'yes') {
							var callParams = []
							callParams.push(id);
							Ext.getBody().mask();
							callParams.push({
										callback : function(data) {
											var k = new Gemma.WaitHandler();
											k.handleWait(data, true);
											this.relayEvents(k, ['done']);
											Ext.getBody().unmask();
											k.on('done', function(payload) {
														this.fireEvent('processedVector', payload)
													});
										}.createDelegate(this)
									});
							ProcessedExpressionDataVectorCreateController.run.apply(this, callParams);
						}
					},
					scope : this,
					animEl : 'elId',
					icon : Ext.MessageBox.WARNING
				});
	},

	initComponent : function() {

		Gemma.EEManager.superclass.initComponent.call(this);

		this.addEvents('done', 'reportUpdated', 'differential', 'missingValue', 'link', 'processedVector', 'deleted',
				'tagsUpdated', 'updated', 'pubmedUpdated', 'pubmedRemove');

		this.save = function(id, fields) {
			/*
			 * TODO
			 */
		};

	}

});


