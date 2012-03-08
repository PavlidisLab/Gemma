/**
 * 
 * @author thea
 * @version $Id$
 */
Ext.namespace('Gemma');

/**
 * 
 * Displays a small number of elements from the set with links to the set's page and a selection editor
 * 
 * This class should generally not be used, use one of the subclasses:
 * GeneSetPreview and ExperimentSetPreview
 * 
 * @class Gemma.SetPreview
 * @xtype Gemma.SetPreview
 */
Gemma.SetPreview = Ext.extend(Ext.Panel, {

	/**
	 * @cfg {Number} preview_size how many entities to display in the preview
	 */
	preview_size: 5,
	/**
	 * keeps track of whether the set being previewed has been modified
	 * readonly
	 * (with the set member grid popup)
	 */
	listModified: false,
		
	border: true,
	hidden: true,
	forceLayout: true,
	hideBorders: true,
	bodyStyle: 'border-color:#B5B8C8; background-color:ghostwhite',
	width: 322,
	
	/**
	 * used for creating title and possibly enabling editing
	 * @param {GeneSetValueObject} gsvo
	 */
	setSelectedSetValueObject: function(gsvo){
		this.selectedSetValueObject = gsvo;
		this.isSet = true;
		this.isSingleEntity = false;
	},
	/**
	 * clear the contents of the preview
	 */
	resetPreview: function(){
		Ext.DomHelper.overwrite(this.previewContent.body, {
			cn: ''
		});
		// this.genePreviewExpandBtn.disable().hide();
		// this.moreIndicator.disable().hide();
	},
	/**
	 * insert a message into the preview
	 * @param {String} msg
	 */
	insertMessage: function(msg){
	
		Ext.DomHelper.append(this.previewContent.body, {
			cn: msg
		});
	},
	/**
	 * Set the taxon id for the preview and selection editor
	 * @param {Number} taxonId
	 */
	setTaxonId: function(taxonId){
		this.taxonId = taxonId;
		this.selectionEditor.setTaxonId(taxonId);
		if (this.addingCombo) {
			this.addingCombo.setTaxonId(taxonId);
		}
	},
	/**
	 * get the taxon id for the preview
	 */
	getTaxonId: function(){
		return this.taxonId;
	},
	
	/**
	 * @private 
	 * Use public methods from subclasses instead
	 * ex: loadGenePreviewFromIds, loadGenePreviewFromGenes, loadGenePreviewFromGeneSet (& analogs for experiment)
	 *
	 * updates the contents of the preview box with the entities passed in
	 *
	 * @param {GeneValueObject[]/ExpressionExperimentValueObject[]} entities an array of geneValueObjects or ExpressionExperimentValueObjects to use to populate preview
	 * @param {Number} total number of total entities (not just those being previewed)
	 */
	loadPreview: function(entities, total){
	
		this.totalCount = (total && total > 0) ? total : this.totalCount;
		// reset the preview panel content
		this.resetPreview();
		for (var i = 0; i < entities.size(); i++) {
			this.previewContent.update(entities[i]);
		}
		this.updateTitle();
		
		if (entities.length >= total) {
			this.moreIndicator.setText('');
			this.moreIndicator.disable().hide();
		} else {
			this.moreIndicator.enable().show();
			this.moreIndicator.setText('['+(total - entities.size()) + ' more...]');
		}
		this.previewContent.expand();
	},

	/**
	 * public
	 * don't use params if you want to update name based on this.selectedEntityOrGroup.resultValueObject
	 * @param {String} name [optional]
	 * @param {Number} size [optional]
	 */
	updateTitle: function(name, size){
		if (!name) {
			name = "Selection Preview";
		}
		this.previewContent.setTitle('<span style="font-size:1.2em">' + name +
		'</span> &nbsp;&nbsp;<span style="font-weight:normal">(' +
		this.totalCount +
		((this.totalCount > 1) ? " entities)" : " entity)"));
	},
	/**
	 * show the preview and expand its contents
	 */
	showPreview: function(){
		this.show();
		if (this.previewContent) {
			this.previewContent.show();
			this.previewContent.expand();
		}
	},
	/**
	 * collapse the preview text
	 */
	collapsePreview: function(){
		this.moreIndicator.hide();
		if (typeof this.previewContent !== 'undefined') {
			this.previewContent.collapse(true);
		}
	},
	/**
	 * expand the preview text
	 */
	expandPreview: function(){
		this.moreIndicator.show();
		if (typeof this.previewContent !== 'undefined') {
			this.previewContent.expand(true);
		}
	},
	/**
	 * @private
	 * loads data into the selection editor and shows the window
	 */
	launchSelectionEditor: function(){
	
		// let owner component control masking, body masking can interact badly with flash components in Firefox
		//Ext.getBody().mask();
		this.fireEvent('maskParentContainer');
	
		if (!(this.selectedEntityOrGroup && this.selectedEntityOrGroup.resultValueObject) &&
		!(this.entityIds || this.entityIds !== null || this.entityIds.length > 0) &&
		!this.selectedSetValueObject) {
			return;
		}
		
		this.selectionEditorWindow.show();
		
		this.selectionEditor.loadMask = new Ext.LoadMask(this.selectionEditor.getEl(), {
			msg: Gemma.StatusText.Loading.generic
		});
		this.selectionEditor.loadMask.show();
		Ext.apply(this.selectionEditor, {
			taxonId: this.getTaxonId()
		});
		
		if (this.selectedSetValueObject) {
			this.selectionEditor.loadSetValueObject(this.selectedSetValueObject, function(){
				this.selectionEditor.loadMask.hide();
			}.createDelegate(this, [], false));
		} else {
			this.selectionEditor.loadEntities(this.entityIds, function(){
				this.selectionEditor.loadMask.hide();
			}.createDelegate(this, [], false));
		}
		
	},
	
	// placeholder for subclasses
	selectionEditor: new Ext.grid.GridPanel({
		name:'placeholder'
	}),
		
	initComponent: function(){
		
		this.selectionEditor.on('doneModification', function(){
			this.selectionEditorWindow.hide();
			this.fireEvent('doneModification');
			this.fireEvent('unmaskParentContainer');
		}, this);
		
		this.moreIndicator = new Ext.Button({
			handler: this.launchSelectionEditor,
			scope: this,
			//style: 'float:right;text-align:right; padding-right:10px',
			style: 'margin-left:10px; padding-bottom:5px;',
			tooltip: "Edit your selection",
			//hidden: true,
			//disabled : true, // enabling later is buggy in IE
			ctCls: 'transparent-btn transparent-btn-link'
		});
		
		this.selectionEditorWindow = new Ext.Window({
			closable: false,
			layout: 'fit',
			width: 500,
			height: 500,
			items: this.selectionEditor,
			title: 'Edit Your Selection'
		});
		this.selectionEditor.on('titlechange', function(panel, newTitle){
			this.selectionEditorWindow.setTitle(newTitle);
		}, this);
		
		this.addEvents(		/**
		 * @event
		 * Fires when the preview's "x"/close button has been pressed
		 */
		'removeMe',  /**
		 * @event
		 * Fires when a set editor window was closed
		 */
		'doneModification');
		
		var itemsForCmp = [{
				ref: 'previewContent',
				title: this.defaultPreviewTitle,
				collapsible: true,
				forceLayout: true,
				cls: 'unstyledTitle',
				bodyStyle: 'padding:10px;padding-bottom:0px; background-color:transparent',
				hidden: false,
				style:'padding-right: 4px;',
				
				// use this to append to content when
				// calling update instead of replacing
				tplWriteMode: 'append',
				tpl: this.defaultTpl,
				
				tools: [{
					id: 'saveEdit',
					handler: this.launchSelectionEditor,
					scope: this,
					qtip: 'Edit or save your set'
				}],
				listeners: {
					collapse: function(){
						this.moreIndicator.hide();
					},
					expand: function(){
						this.moreIndicator.show();
					},
					scope: this
				}
			}];	
		if (this.moreIndicator) {
			itemsForCmp.push(this.moreIndicator);
		}
		if(this.addingCombo){
			itemsForCmp.push(this.addingCombo);
		}
		
		Ext.apply(this,{
			items: itemsForCmp
		});
		
		Gemma.SetPreview.superclass.initComponent.call(this);
		
	}
	
});

Ext.reg('Gemma.SetPreview', Gemma.SetPreview);