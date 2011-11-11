/**
 * @author thea
 * @version $Id: ExperimentSearchAndPreview.js,v 1.7 2011/05/06 01:05:42
 *          tvrossum Exp $
 */
Ext.namespace('Gemma');

Gemma.ExperimentSearchAndPreview = Ext.extend(Ext.Panel, {
	taxonId: null, // might be set by parent to control combo
	listModified: false,
	getSelectedExperimentOrExperimentSetValueObject:function(){
		return (this.selectedExperimentOrGroup)? this.selectedExperimentOrGroup.resultValueObject : null;
	},
	setSelectedExpressionExperimentSetValueObject: function(eesvo){
		this.selectedExpressionExperimentSetValueObject = eesvo;
		this.isExperimentSet = true;
		this.isExperiment = false;
	},
	getSelectedExpressionExperimentSetValueObject: function(){
		return this.selectedExpressionExperimentSetValueObject;
	},
	/**
	 * Show the selected eeset members
	 */
	loadExperimentOrGroup : function(record, query) {

		this.selectedExperimentOrGroup = record.data;
		
		if(this.selectedExperimentOrGroup.resultValueObject instanceof ExpressionExperimentSetValueObject){
			this.setSelectedExpressionExperimentSetValueObject(this.selectedExperimentOrGroup.resultValueObject);
		}else if (this.selectedExperimentOrGroup.resultValueObject instanceof ExpressionExperimentValueObject){
			delete this.selectedExpressionExperimentSetValueObject;
			this.isExperiment = true;
			this.isExperimentSet = false;
		}
		var id = record.get("resultValueObject").id;
		this.queryUsedToGetSessionGroup = (id === null || id === -1)? query : null;
		
		var isGroup = record.get("isGroup");
		var type = record.get("type");
		var resultValueObject = record.get("resultValueObject");
		var name = record.get("name");
		var taxonId = record.get("taxonId");
		var taxonName = record.get("taxonName");
		
		// for bookmarking diff ex viz
		if (id === null || id === -1) {
			var queryToGetSelected = name;
			if(resultValueObject instanceof FreeTextExpressionExperimentResultsValueObject && name.indexOf(query)!=-1){
				queryToGetSelected = "taxon:"+taxonId+"query:"+query;
			}
			this.queryUsedToGetSessionGroup = queryToGetSelected;
		}
		
		
		// load preview of group if group was selected
		if (isGroup) {
			eeIds = record.get('memberIds');
			this.experimentGroupId = id;
			if (!eeIds || eeIds === null || eeIds.length === 0) {
				return;
			}
			this.loadExperiments(eeIds);

		}
		// load single experiment if experiment was selected
		else {
			this.experimentIds = [id];
			// reset the experiment preview panel content
			this.resetExperimentPreview();

			// update the ee preview panel content
			this.previewPart.experimentPreviewContent.update({
						shortName : record.get("name"),
						name : record.get("description"),
						id : record.get("resultValueObject").id,
						taxon : record.get("taxonName")
					});
			this.updateTitle(this.experimentCombo.getRawValue(), 1);
			this.experimentSelectionEditorBtn.setText('0 more - Edit');
			this.experimentSelectionEditorBtn.show();
			this.previewPart.experimentPreviewContent.expand();
			this.previewPart.moreIndicator.update('');
		}
	},

	/**
	 * update the contents of the experiment preview box and the this.experimentIds value
	 * using an expressionExperimentSetValueObject
	 * 
	 * @param eesvo an ExpressionExperimentSetValueObject object
	 * @return success
	 */
	loadExperimentSet: function(eesvo){
		if(eesvo instanceof ExpressionExperimentSetValueObject){
			this.setSelectedExpressionExperimentSetValueObject(eesvo);
			this.loadExperiments(eesvo.expressionExperimentIds);
			return true;
		}
		return false;
	},
	/**
	 * update the contents of the experiment preview box and the this.experimentIds value
	 * using a list of experiment Ids
	 * 
	 * @param ids
	 *            an array of experimentIds to use
	 */
	loadExperiments : function(ids) {

		// store selected ids for searching
		this.searchForm.experimentIds.push(ids);
		this.experimentIds = ids;

		this.loadExperimentPreview();

	},

	/**
	 * update the contents of the experiment preview box using the
	 * this.experimentIds value
	 */
	loadExperimentPreview : function() {

		this.maskExperimentPreview();

		// store selected ids for searching
		ids = this.experimentIds;
		if(!this.experimentIds && this.selectedExpressionExperimentValueObject){
			ids = this.selectedExpressionExperimentValueObject.expressionExperimentIds;
		}

		if (!ids || ids === null || ids.length === 0) {
			this.resetExperimentPreview();
			return;
		}

		// reset the experiment preview panel content
		this.resetExperimentPreview();

		// load some experiments for previewing
		var limit = (ids.size() < this.searchForm.PREVIEW_SIZE) ? ids.size() : this.searchForm.PREVIEW_SIZE;
		var idsToPreview = [];
		for (var i = 0; i < limit; i++) {
			idsToPreview[i] = ids[i];
		}
		ExpressionExperimentController.loadExpressionExperiments(idsToPreview, function(ees) {

					for (var j = 0; j < ees.size(); j++) {
						this.previewPart.experimentPreviewContent.update(ees[j]);
					}
					
					this.updateTitle();
					
					this.showExperimentPreview();
					
					if (ids.size() <= this.searchForm.PREVIEW_SIZE) {
						this.previewPart.moreIndicator.update('');
					}
					else {
						this.previewPart.moreIndicator.update('[...]');
					}
					if (ids.size() === 1) {
						this.experimentSelectionEditorBtn.setText('0 more - Edit');
						this.experimentSelectionEditorBtn.show();
					}else{
						this.experimentSelectionEditorBtn.setText((ids.size() - limit) + ' more - Edit');
					}
					
				}.createDelegate(this));
	},

	/**
	 * 
	 */
	launchExperimentSelectionEditor : function() {
		this.searchForm.getEl().mask();

		this.experimentSelectionEditorWindow.show();

		this.experimentSelectionEditor.loadMask = new Ext.LoadMask(this.experimentSelectionEditor.getEl(), {
					msg : "Loading experiments ..."
				});
		this.experimentSelectionEditor.loadMask.show();
		Ext.apply(this.experimentSelectionEditor, {
					taxonId : this.searchForm.getTaxonId(),
					taxonName : this.searchForm.getTaxonName()
				});
				
				
		if(this.selectedExpressionExperimentSetValueObject){
			this.experimentSelectionEditor.loadExperimentSetValueObject(this.selectedExpressionExperimentSetValueObject, function() {
					this.experimentSelectionEditor.loadMask.hide();
				}.createDelegate(this, [], false));
		}else if(this.experimentIds){
			this.experimentSelectionEditor.loadExperiments(this.experimentIds, function() {
					this.experimentSelectionEditor.loadMask.hide();
				}.createDelegate(this, [], false));
		}
	},

	/**
	 * 
	 */
	maskExperimentPreview : function() {
		if (!this.loadMask) {
			this.loadMask = new Ext.LoadMask(this.getEl(), {
						msg : "Loading Experiments ..."
					});
		}
		this.loadMask.show();
	},
	showExperimentPreview : function() {
		this.loadMask.hide();
		this.experimentSelectionEditorBtn.show();
		this.previewPart.experimentPreviewContent.show();
		this.previewPart.show();
		this.previewPart.experimentPreviewContent.expand();
	},
	resetExperimentPreview : function() {
		Ext.DomHelper.overwrite(this.previewPart.experimentPreviewContent.body, {
					cn : ''
				});
		// this.experimentPreviewExpandBtn.disable().hide();
		// this.experimentSelectionEditorBtn.disable().hide();
		// this.previewPart.experimentPreviewContent.collapse();
	},

	initComponent : function() {

		/**
		 * **** EE COMBO
		 * ***************************************************************************
		 */

		// Shows the combo box for EE groups
		this.newBoxTriggered = false;
		this.experimentCombo = new Gemma.ExperimentAndExperimentGroupCombo({
					width : 310,
					taxonId: this.taxonId,
					hideTrigger: true
					
				});
		this.experimentCombo.on('select', function(combo, record, index) {

					// if the EE has changed taxon, reset the experiment combo
					this.searchForm.taxonChanged(record.get("taxonId"), record.get("taxonName"));
					
					this.experimentSelectionEditor.setTaxonId(record.get("taxonId"));

					// store the eeid(s) selected and load some EE into the
					// previewer
					// store the taxon associated with selection
					var query = combo.store.baseParams.query;
					this.loadExperimentOrGroup(record, query);
					this.previewPart.experimentPreviewContent.show();
					this.previewPart.show();

					// if this was the first time a selection was made using
					// this box
					if (combo.startValue === '' && this.newBoxTriggered === false) {
						this.fireEvent('madeFirstSelection');
						this.newBoxTriggered = true;
						this.helpBtn.hide();
						this.removeBtn.show();
						// this.relayEvents(this.experimentCombo, ['select']);
					}
					combo.disable().hide();
					//this.selectionTitle.update(combo.getValue());
					this.helpBtn.hide();
					this.removeBtn.show();
					this.removeBtn.setPosition(300,0);
					this.doLayout();

				}, this);

		/**
		 * ***** EXPERIMENT SELECTION EDITOR
		 * *****************************************************************
		 */

		this.experimentSelectionEditor = new Gemma.ExpressionExperimentMembersGrid({
					// id : 'experimentSelectionEditor',
					name : 'experimentSelectionEditor',
					// hidden: 'true',
					hideHeaders : true,
					frame : false,
					queryText : this.experimentCombo.getValue(),
					width : 500,
					height : 500
				});

		this.experimentSelectionEditor.on('experimentListModified', function(newValueObjects) {
					var i;
					for (i = 0; i < newValueObjects.length; i++) { // should only
						// be one
						if (newValueObjects[i]) {
							this.loadExperimentSet(newValueObjects[i]);
							// update selected record
							this.selectedExperimentOrGroup.resultValueObject = newValueObjects[i];
							this.setSelectedExpressionExperimentSetValueObject(newValueObjects[i]);
							this.updateTitle();
						}
					}
					this.listModified = true;
				}, this);

		this.experimentSelectionEditor.on('doneModification', function() {
					this.searchForm.getEl().unmask();
					this.experimentSelectionEditorWindow.hide();
				}, this);

		/**
		 * 
		 */
		this.experimentSelectionEditorBtn = new Ext.Button({
					handler : this.launchExperimentSelectionEditor,
					scope : this,
					style : 'float:right;text-align:right; margin-right:10px; margin-bottom:5px;',
					tooltip : "Edit your selection",
					hidden : true,
					//disabled : true, // enabling later is buggy in IE
					ctCls : 'right-align-btn transparent-btn transparent-btn-link'
				});

		this.experimentSelectionEditorWindow = new Ext.Window({
					// id : 'experimentSelectionEditorWindow',
					// closeAction: 'hide',
					closable : false,
					layout : 'fit',
					width : 500,
					height : 500,
					items : this.experimentSelectionEditor,
					title : 'Edit Your Experiment Selection'
				});
				
		this.experimentSelectionEditor.on('titlechange', function(panel, newTitle){
			this.experimentSelectionEditorWindow.setTitle(newTitle);
		}, this);

		/**
		 * **** EE PREVIEW
		 * *************************************************************************
		 */

		/**
		 * don't use params if you want to update name based on this.selectedExpressionExperimentValueObject
		 * @param {Object} name
		 * @param {Object} size
		 */
		this.updateTitle = function(name, size){
			
			// if an experiment set page exists for this set, make title a link 
			if( !name && this.selectedExpressionExperimentSetValueObject instanceof ExpressionExperimentSetValueObject){
				
				size = this.selectedExpressionExperimentSetValueObject.expressionExperimentIds.size();
			
				if( this.selectedExpressionExperimentSetValueObject instanceof DatabaseBackedExpressionExperimentSetValueObject){
					
					name = '<a target="_blank" href="/Gemma/expressionExperimentSet/showExpressionExperimentSet.html?id='+
							this.selectedExpressionExperimentSetValueObject.id+'">'+this.selectedExpressionExperimentSetValueObject.name+'</a>'
				}else{
					name = this.selectedExpressionExperimentSetValueObject.name;
				}
			}
			
			this.previewPart.experimentPreviewContent.setTitle(
				'<span style="font-size:1.2em">'+name+
				'</span> &nbsp;&nbsp;<span style="font-weight:normal">(' + size + ((size > 1)?" experiments)":" experiment)"));
			this.previewPart.experimentPreviewContent.doLayout();
		};
		this.previewPart = new Ext.Panel({
			border: true,
			hidden: true,
			forceLayout: true,
			hideBorders: true,
			bodyStyle: 'border-color:#B5B8C8; background-color:ghostwhite', 
			items: [{
				ref: 'experimentPreviewContent',
				width: 315,
				tpl: new Ext.XTemplate('<tpl for="."><div style="padding-bottom:7px;"><a target="_blank" href="/Gemma/expressionExperiment/showExpressionExperiment.html?id=',
						 '{id}"', ' ext:qtip="{shortName}">{shortName}</a>&nbsp; {name} <span style="color:grey">({taxon})</span></div></tpl>'),
				tplWriteMode: 'append',
				//style : 'padding-top:7px;',
				title: 'Experiment Selection Preview',
				collapsible: true,
				cls: 'unstyledTitle',
				bodyStyle: 'padding:10px;padding-bottom:0px; background-color:transparent', 
				hidden: true,
				tools: [{
					id: 'delete',
					handler: function(event, toolEl, panel, toolConfig){
						this.searchForm.removeExperimentChooser(this.id);
					// this.fireEvent('removeExperiment');
					}.createDelegate(this, [], true)					,
					qtip: 'Remove this experiment or group from your search'
				}],
				listeners: {
					collapse: function(){
						this.experimentSelectionEditorBtn.hide();
							this.previewPart.moreIndicator.hide();
					},
					expand: function(){
						this.experimentSelectionEditorBtn.show();
							this.previewPart.moreIndicator.show();
					},
					scope:this
				}
			},{
				xtype: 'box',
				ref: 'moreIndicator',
				html: '[...]',
				hidden: false,
				style: 'margin-left:10px; background-color:transparent',
			},this.experimentSelectionEditorBtn]
		});
		
		this.collapsePreview = function(){
			this.experimentSelectionEditorBtn.hide();
			if(typeof this.previewPart.experimentPreviewContent !== 'undefined'){
				this.previewPart.experimentPreviewContent.collapse();
			}
		};		
		
		this.removeBtn = new Ext.Button({
					icon : "/Gemma/images/icons/cross.png",
					cls : "x-btn-icon",
					tooltip : 'Remove this experiment or group from your search',
					hidden : true,
					handler : function() {
						this.searchForm.removeExperimentChooser(this.id);
						// this.fireEvent('removeExperiment');
					}.createDelegate(this, [], true)
				});
		this.helpBtn = new Gemma.InlineHelpIcon({
			tooltipText:'Select a group of experiments or try searching for experiments by name, '+
					' or keywords such as: schizophrenia, hippocampus, GPL96 etc.<br><br>'+
					'<b>Example: search for Alzheimer\'s and select all human experiments'
		});
		Ext.apply(this, {
			frame : false,
			border:false,
			hideBorders:true,
			width: 330,
			items: [
			{
				layout: 'hbox',
				hideBorders: true,
				items: [this.experimentCombo, this.helpBtn]
			}, this.previewPart]
		});
		Gemma.ExperimentSearchAndPreview.superclass.initComponent.call(this);
	}
});

Ext.reg('experimentSearchAndPreview', Gemma.ExperimentSearchAndPreview);
