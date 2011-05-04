/**
 * @author thea
 * @version $Id$
 */
Ext.namespace('Gemma');

Gemma.ExperimentSearchAndPreview = Ext.extend(Ext.Panel, {
	/**
	 * Show the selected eeset members 
	 */
	loadExperimentOrGroup : function(record, query) {
				
		this.selectedExperimentOrGroupRecord = record.data;
				
		var id = record.data.reference.id;
		var isGroup = record.get("isGroup");
		var type = record.get("type");
		var reference = record.get("reference");
		var name = record.get("name");
		
		var taxonId = record.get("taxonId");
		this.searchForm.setTaxonId(taxonId);
		var taxonName = record.get("taxonName");
		this.searchForm.setTaxonName(taxonName);
		this.searchForm.setTaxonId(taxonId);
						
		// load preview of group if group was selected
		if (isGroup) {
			eeIds = record.get('memberIds');
			if(!eeIds || eeIds === null || eeIds.length === 0){
				return;
			}
			this.loadExperiments(eeIds);
			
		}
		//load single experiment if experiment was selected
		else {
			this.experimentIds = [id];
			this.searchForm.experimentIds = [id];
			// reset the experiment preview panel content
			this.resetExperimentPreview();
			
			// update the gene preview panel content
			this.experimentPreviewContent.update({
				shortName: record.get("name"),
				name: record.get("description"),
				id: record.get("id"),
				taxon: record.get("taxonName")
			});
			this.experimentPreviewContent.setTitle("Experiment Selection Preview (1)");
			this.experimentSelectionEditorBtn.setText('0 more');
			this.experimentSelectionEditorBtn.disable();
			this.experimentSelectionEditorBtn.show();
		}
	},
				
	/**
	 * update the contents of the gene preview box and the this.geneIds value using a list of gene Ids
	 * @param geneIds an array of geneIds to use
	 */
	loadExperiments : function(ids) {
				
		//store selected ids for searching
		this.searchForm.experimentIds.push(ids);
		this.experimentIds = ids;
						
		this.loadExperimentPreview();
						
	},
							
	/**
	 * update the contents of the epxeriment preview box using the this.experimentIds value
	 * @param geneIds an array of geneIds to use
	 */
	loadExperimentPreview : function() {
		
		this.maskExperimentPreview();
				
		//store selected ids for searching
		ids = this.experimentIds;
		
		if(!ids || ids === null || ids.length === 0){
			this.resetExperimentPreview();
			return;
		}
						
		// reset the experiment preview panel content
		this.resetExperimentPreview();
						
		// load some experiments for previewing
		var limit = (ids.size() < this.searchForm.PREVIEW_SIZE) ? ids.size() : this.searchForm.PREVIEW_SIZE;
		var idsToPreview = [];
		for (var i = 0; i < limit; i++) {
			idsToPreview[i]=ids[i];
		}
		ExpressionExperimentController.loadExpressionExperiments(idsToPreview,function(ees){
							
			for (var j = 0; j < ees.size(); j++) {
				this.experimentPreviewContent.update(ees[j]);
			}
			this.experimentPreviewContent.setTitle("Experiment Selection Preview ("+ids.size()+")");
			this.experimentSelectionEditorBtn.setText('<a>'+(ids.size() - limit) + ' more - Edit</a>');
			this.showExperimentPreview();
			
			if (ids.size() === 1) {
				this.experimentSelectionEditorBtn.setText('0 more');
				this.experimentSelectionEditorBtn.disable();
				this.experimentSelectionEditorBtn.show();
			}
					
		}.createDelegate(this));
						
	},
	launchExperimentSelectionEditor: function(){
				
		this.searchForm.getEl().mask();

		this.experimentSelectionEditorWindow.show();

		this.experimentSelectionEditor.loadMask = new Ext.LoadMask(this.experimentSelectionEditor.getEl(), {
							msg: "Loading experiments ..."
						});
		this.experimentSelectionEditor.loadMask.show();
		Ext.apply(this.experimentSelectionEditor, {
			experimentGroupId: this.experimentGroupId,
			selectedExperimentGroup: this.experimentCombo.getExpressionExperimentGroup(), 
			groupName: this.experimentCombo.getExpressionExperimentGroup().name
		});
		this.experimentSelectionEditor.loadExperiments(this.experimentIds, 
				function(){
					this.experimentSelectionEditor.loadMask.hide();
					//Ext.getCmp('experimentSelectionEditor').loadMask.hide();
				}.createDelegate(this, [], false));
	},

		
	maskExperimentPreview: function(){
		if (!this.loadMask) {
			this.loadMask = new Ext.LoadMask(this.getEl(), {
				msg: "Loading Experiments ..."
			});
		}
		this.loadMask.show();
	},
	
	showExperimentPreview: function(){
		this.loadMask.hide();
		this.experimentSelectionEditorBtn.enable();
		this.experimentSelectionEditorBtn.show();
		this.experimentPreviewContent.show();
		this.experimentPreviewContent.expand();
	},	
	resetExperimentPreview: function(){
		Ext.DomHelper.overwrite(this.experimentPreviewContent.body, {cn: ''});
		//this.experimentPreviewExpandBtn.disable().hide();
		//this.experimentSelectionEditorBtn.disable().hide();
		//this.experimentPreviewContent.collapse();
	},
	/**
	 * Check if the taxon needs to be changed, and if so, update the geneAndGroupCombo and reset the gene preivew
	 * @param {} taxonId
	 */
	taxonChanged : function(taxonId, taxonName) {

		// if the 'new' taxon is the same as the 'old' taxon for the experiment combo, don't do anything
		if (taxonId && this.searchForm.getTaxonId() && (this.searchForm.getTaxonId() === taxonId) ) {
			return;
		}
		// if the 'new' and 'old' taxa are different, reset the gene preview and filter the geneCombo
		else if(taxonId){
			//this.searchForm.geneChooser.resetGenePreview();
			//this.searchForm.geneChooser.geneCombo.setTaxonId(taxonId);
			this.searchForm.setTaxonId(taxonId);
			this.searchForm.setTaxonName(taxonName);
			//this.searchForm.geneChooser.geneCombo.reset();
		}

		this.searchForm.fireEvent("taxonchanged", taxonId);
	},
	initComponent: function(){
		
		/****** EE COMBO ****************************************************************************/
		
		// Shows the combo box for EE groups 
		this.newBoxTriggered = false;
		this.experimentCombo = new Gemma.ExperimentAndExperimentGroupCombo({
							typeAhead: false,
							width : 300
						});
		this.experimentCombo.on('select', function(combo, record, index) {
																				
										// if the EE has changed taxon, reset the gene combo
										this.taxonChanged(record.get("taxonId"), record.get("taxonName"));
										
										// store the eeid(s) selected and load some EE into the previewer
										// store the taxon associated with selection
										var query = combo.store.baseParams.query;
										this.loadExperimentOrGroup(record, query);
										this.experimentPreviewContent.show();
										
										// once an experiment has been selected, cue the user that the gene select is now active
										Ext.get(this.searchForm.geneChoosers.items.items[0].geneCombo.id).setStyle('background','white');
										//Ext.apply(this.searchForm.geneChoosers.items.items[0].items.items[1].geneCombo, {style:'background:white;'}); // doesn't work
										//this.eeComboSelectedRecord=record;
										//this.searchForm.eeComboSelectedRecord=record; // needed?
										
										// if this was the first time a selection was made using this box
										if(combo.startValue==='' && this.newBoxTriggered === false){
											this.fireEvent('madeFirstSelection');
											this.newBoxTriggered = true;
											this.helpBtn.hide();
											this.removeBtn.show();
											//this.relayEvents(this.experimentCombo, ['select']);
										}
										
									},this);
		
		
		
		/******* EXPERIMENT SELECTION EDITOR ******************************************************************/		
				
		this.experimentSelectionEditor = new Gemma.ExpressionExperimentMembersGrid( {
			id: 'experimentSelectionEditor',
			height : 200,
			//hidden: 'true',
			hideHeaders:true,
			frame:false
		});
		
		this.experimentSelectionEditor.on('experimentListModified', function(newExperimentIds, groupName){
			if(newExperimentIds){
				this.loadExperiments(newExperimentIds);
			} if(groupName){
				this.experimentCombo.setRawValue(groupName);
			}
			this.experimentCombo.getStore().reload();
		},this);
		
		this.experimentSelectionEditor.on('doneModification', function(){
			this.searchForm.getEl().unmask();
			this.experimentSelectionEditorWindow.hide();
			},this);

		this.experimentSelectionEditorBtn = new Ext.LinkButton({
							handler: this.launchExperimentSelectionEditor,//.createDelegate(this, [], false),
							scope : this,
							style: 'float:right;text-align:right; ',
							width: '200px',
							tooltip : "Edit your selection",
							hidden : true,
							disabled: true,
							ctCls: 'right-align-btn transparent-btn'
		});
		
		this.experimentSelectionEditorWindow = new Ext.Window({
				id:'experimentSelectionEditorWindow',
				//closeAction: 'hide',
				closable: false,
				layout: 'fit',
				items: this.experimentSelectionEditor,
				title: 'Edit Your Experiment Selection'
			});
		
		 
		/****** EE PREVIEW **************************************************************************/

		this.experimentPreviewContent = new Ext.Panel({
				width:290,
				//id:'experimentPreview',
				//html:'<div style="padding: 7px 0 ;font-weight:bold;">Experiment Selection Preview</div>',
				tpl: new Ext.XTemplate(
				'<tpl for="."><div style="padding-bottom:7px;"><a target="_blank" href="/Gemma/expressionExperiment/showExpressionExperiment.html?id=',
				'{id}"',
				' ext:qtip="{shortName}">{shortName}</a>&nbsp; {name} <span style="color:grey">({taxon})</span></div></tpl>'),
				tplWriteMode: 'append',
				style:'padding-top:7px;',
				title: 'Experiment Selection Preview',
				collapsible: true,
				cls: 'unstyledTitle',
				hidden:true,
				listeners:{
					collapse: function(){
						this.experimentSelectionEditorBtn.hide();
					}.createDelegate(this, [], true),
					expand: function(){
						this.experimentSelectionEditorBtn.show();
					}.createDelegate(this, [], true)
				}
		});	
		this.experimentPreviewExpandBtn = new Ext.Button({
							handler:function(){
								//this.experimentPreviewExpandBtn.disable().hide();
								//this.loadExperimentPreview();
								if(this.experimentPreviewContent.collapsed){
									this.experimentPreviewContent.expand();
								}else{
									this.experimentPreviewContent.collapse();
								}
								
							}.createDelegate(this, [], true),
							scope : this,
							//style: 'float:right;',
							tooltip : "View your selection",
							hidden : true,
							disabled: true,
							icon : "/Gemma/images/minus.gif",
							cls : "x-btn-icon"
		});
		this.removeBtn = new Ext.Button({
			icon: "/Gemma/images/icons/cross.png",
			cls: "x-btn-icon",
			tooltip: 'Remove this experiment or group from your search',
			hidden: true,
			handler: function(){
				this.searchForm.removeExperimentChooser(this.id);
				//this.fireEvent('removeExperiment');
			}.createDelegate(this, [], true)
		});
		this.helpBtn = new Ext.Button({
			icon: "/Gemma/images/icons/questionMark16x16.png",
			cls: "x-btn-icon",
			tooltip: 'Select a general group of experiments or try searching for experiments by name or keywords such as: schizophrenia, hippocampus, GPL96 etc.',
			hidden: false,
			handler: function(){
				Ext.Msg.alert('Experiment Selection Help', 'Select a general group of experiments or try searching for experiments by name or keywords such as: schizophrenia, hippocampus, GPL96 etc.');
			}
		});
		Ext.apply(this, {
				frame:true,
				width: 330,
				items:[{layout:'hbox',items:[this.experimentCombo,this.removeBtn,this.helpBtn]},this.experimentPreviewExpandBtn,this.experimentPreviewContent,this.experimentSelectionEditorBtn]
		});
		Gemma.ExperimentSearchAndPreview.superclass.initComponent.call(this);
}
	});

Ext.reg('experimentSearchAndPreview', Gemma.ExperimentSearchAndPreview);
