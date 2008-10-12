/**
 * Common tasks to run on experiments. To use this, construct it with a id defined in the config (e.g., {id :
 * 'eemanager}). Then you can use things like onClick=Ext.getCmp('eemanager').updateEEReport(id).
 * 
 * @class Gemma.EEManager
 * @extends Ext.Component
 */
Gemma.EEManager = Ext.extend(Ext.Component, {

	initComponent : function() {

		Gemma.EEManager.superclass.initComponent.call(this);

		this.addEvents('reportUpdated', 'differential', 'missingValue', 'link' , 'processedVector', 'deleted', 'tagsUpdated', 'updated', 'pubmedUpdated', 'pubmedRemove');

		/**
		 * Paramters are passed to ProgressWindow config; eventToFire is fired in the callback.
		 */
		this.handleWait = function(taskId, eventToFire, showAllMessages) {
			try {
				var p = new Gemma.ProgressWindow({
							taskId : taskId,
							callback : function(data) {
								this.fireEvent(eventToFire, data);
							}.createDelegate(this),
							showAllMessages : showAllMessages
						});

				p.show();
			} catch (e) {
				Ext.Msg.alert("Error", e);
			}
		};

		this.updateEEReport = function(id) {
			var callParams = [];
			callParams.push(id);
			callParams.push({
						callback : function(data) {
							this.handleWait(data, 'reportUpdated', false);
						}.createDelegate(this)
					});

			ExpressionExperimentController.updateReport.apply(this, callParams);
		};

		this.deleteExperiment = function(id) {
			Ext.Msg.show({
						title : 'Really delete?',
						msg : 'Are you sure you want to delete the experiment? This cannot be undone.',
						buttons : Ext.Msg.YESNO,
						fn : function(btn, text) {
							if (btn == 'yes') {
								var callParams = []
								callParams.push(id);
								callParams.push({
											callback : function(data) {
												this.handleWait(data, 'deleted', true);
											}.createDelegate(this)
										});
								ExpressionExperimentController.deleteById.apply(this, callParams);
							}
						},
						animEl : 'elId',
						icon : Ext.MessageBox.WARNING
					});
		};

		this.save = function(id, fields) {
			/*
			 * TODO
			 */
		};

		this.tagger = function(id) {

			var annotator = new Ext.Panel({
						id : 'annotator-wrap',
						title : "Tags",
						collapsible : false,
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
		};

		this.doLinks = function(id) {
			Ext.Msg.show({
						title : 'Link analysis',
						msg : 'Please confirm. Previous analaysis results will be deleted.',
						buttons : Ext.Msg.YESNO,
						fn : function(btn, text) {
							if (btn == 'yes') {
								var callParams = []
								callParams.push(id);
								callParams.push({
											callback : function(data) {
												this.handleWait(data, 'link', true);
											}.createDelegate(this)
										});
								LinkAnalysisController.run.apply(this, callParams);
							}
						},
						animEl : 'elId',
						icon : Ext.MessageBox.WARNING
					});
		};

		this.doMissingValues = function(id) {
			Ext.Msg.show({
						title : 'Missing value analysis',
						msg : 'Please confirm. Previous analaysis results will be deleted.',
						buttons : Ext.Msg.YESNO,
						fn : function(btn, text) {
							if (btn == 'yes') {
								var callParams = []
								callParams.push(id);
								callParams.push({
											callback : function(data) {
												this.handleWait(data, 'missingValue', true);
											}.createDelegate(this)
										});
								ArrayDesignRepeatScanController.run.apply(this, callParams);
							}
						},
						animEl : 'elId',
						icon : Ext.MessageBox.WARNING
					});
		};

		this.doDifferential = function(id) {
			Ext.Msg.show({
						title : 'Differential expression analysis',
						msg : 'Please confirm. Previous analaysis results will be deleted.',
						buttons : Ext.Msg.YESNO,
						fn : function(btn, text) {
							if (btn == 'yes') {
								var callParams = []
								callParams.push(id);
								callParams.push({
											callback : function(data) {
												this.handleWait(data, 'differential', true);
											}.createDelegate(this)
										});
								DifferentialExpressionAnalysisController.run.apply(this, callParams);
							}
						},
						animEl : 'elId',
						icon : Ext.MessageBox.WARNING
					});
		};
		
		this.doProcessedVectors = function(id) {
		Ext.Msg.show({
					title : 'Processed vector analysis',
					msg : 'Please confirm. Previous analaysis results will be deleted.',
					buttons : Ext.Msg.YESNO,
					fn : function(btn, text) {
						if (btn == 'yes') {
							var callParams = []
							callParams.push(id);
							callParams.push({
										callback : function(data) {
												this.handleWait(data, 'processedVector', true);
											}.createDelegate(this)
									});
							ProcessedExpressionDataVectorCreateController.run.apply(this, callParams);
						}
					},
					animEl : 'elId',
					icon : Ext.MessageBox.WARNING
				});
		};

	}

});
