/**
 * 
 * @author thea
 * @version $Id$
 */
Ext.namespace('Gemma');

/**
 * 
 * Displays a small number of elements from the set with links to the set's page and to an editor
 * @class Gemma.ExperimentSetPreview
 * @xtype Gemma.ExperimentSetPreview
 */
Gemma.ExperimentSetPreview = Ext.extend(Gemma.SetPreview, {

	/**
	 * public
	 * update the contents of the experiment preview box
	 *
	 * @param {Number[]} ids an array of experimentIds to use to populate preview
	 */
	loadExperimentPreviewFromIds: function(ids){
	
		this.entityIds = ids;
		this.totalCount = ids.length;
		// load some experiments to display
		var limit = (ids.size() < this.preview_size) ? ids.size() : this.preview_size;
		var previewIds = ids.slice(0, limit);
		
		ExpressionExperimentController.loadExpressionExperiments(previewIds, function(ees) {
			this.loadPreview(ees, ids.length);
		}.createDelegate(this));
		
	},
	
	
	/**
	 * public
	 * update the contents of the experiment preview box
	 *
	 * @param {ExperimentValueSetObject[]} experimentSet populate preview with members
	 */
	loadExperimentPreviewFromExperimentSet: function(eeSet){
	
		var ids = eeSet.expressionExperimentIds;
		this.entityIds = ids;
		// load some ees to display
		this.loadExperimentPreviewFromIds(ids);
		this.setSelectedSetValueObject(eeSet);
		
	},
	
	/**
	 * public
	 * update the contents of the experiment preview box
	 *
	 * @param {ExperimentValueObject[]} ees an array of ees to use to populate preview
	 */
	loadExperimentPreviewFromExperiments: function(experiments){
	
		this.entityIds = [];
		Ext.each(experiments, function(item, index, allitems){
			this.entityIds.push(item.id);
		}, this);
		
		this.totalCount = experiments.length;
		// load some experiments to display
		var limit = (experiments.size() < this.preview_size) ? experiments.size() : this.preview_size;
		var previewExperiments = experiments.slice(0, limit);
		this.loadPreview(previewExperiments, experiments.length);
		
	},
	
	/**
	 * public
	 * don't use params if you want to update name based on this.selectedEntityOrGroup.resultValueObject
	 * @param {Object} name
	 * @param {Object} size
	 */
	updateTitle: function(name, size){
	
		// if an experiment group page exists for this set, make title a link 
		if (!name && this.selectedSetValueObject instanceof ExpressionExperimentSetValueObject) {
			size = this.selectedSetValueObject.expressionExperimentIds.size();
			
			if (! (this.selectedSetValueObject instanceof SessionBoundExpressionExperimentSetValueObject) ){
			
				name = '<a target="_blank" href="/Gemma/expressionExperimentSet/showExpressionExperimentSet.html?id=' +
				this.selectedSetValueObject.id +
				'">' +
				this.selectedSetValueObject.name +
				'</a>';
				
			} else {
				name = this.selectedSetValueObject.name;
			}
		} else if (!name) {
			name = "Experiment Selection Preview";
		}
		this.previewContent.setTitle('<span style="font-size:1.2em">' + name +
				'</span> &nbsp;&nbsp;<span style="font-weight:normal">(' +
				this.totalCount +
				((this.totalCount > 1) ? " experiments)" : " experiment)"));
	},
	
	initComponent: function(){
	
		var withinSetExperimentCombo = new Gemma.ExperimentAndExperimentGroupCombo({
			width: 300,
			style:'margin:10px',
			hideTrigger: true,
			emptyText: 'Add experiments to your group'
		});
		withinSetExperimentCombo.setTaxonId(this.taxonId);
		withinSetExperimentCombo.on('select', function(combo, record, index){
		
			var allIds = this.entityIds;
			var newIds = record.get('memberIds');
			var i;
			// don't add duplicates
			for (i = 0; i < newIds.length; i++) {
				if (allIds.indexOf(newIds[i]) < 0) {
					allIds.push(newIds[i]);
				}
			}
			var currentTime = new Date();
			var hours = currentTime.getHours();
			var minutes = currentTime.getMinutes();
			if (minutes < 10) {
				minutes = "0" + minutes;
			}
			var time = '(' + hours + ':' + minutes + ') ';
			
			var editedGroup;
			editedGroup = new SessionBoundExpressionExperimentSetValueObject();
			editedGroup.id = null;
			editedGroup.name = time+" Custom Experiment Group";
			editedGroup.description = "Temporary experiment group created " + currentTime.toString();
			editedGroup.expressionExperimentIds = allIds;
			editedGroup.taxonId = record.get('taxonId');
			editedGroup.taxonName = record.get('taxonName');
			editedGroup.numExperiments = editedGroup.expressionExperimentIds.length;
			editedGroup.modified = true;
			editedGroup.publik = false;
			
			
			ExpressionExperimentSetController.addSessionGroups([editedGroup], true, // returns datasets added
 				function(newValueObjects){
				// should be at least one datasetSet
				if (newValueObjects === null || newValueObjects.length === 0) {
					// TODO error message
					return;
				} else {
					
					withinSetExperimentCombo.reset();
					this.focus(); // want combo to lose focus
					this.loadExperimentPreviewFromIds(newValueObjects[0].expressionExperimentIds);
					this.setSelectedSetValueObject(newValueObjects[0]);
					this.updateTitle();
					this.fireEvent('experimentListModified', newValueObjects);
					this.fireEvent('doneModification');
				}
			}.createDelegate(this));
			
		},this);

		Ext.apply(this, {
			selectionEditor: new Gemma.ExpressionExperimentMembersGrid({
					name : 'selectionEditor',
					hideHeaders : true,
					frame : false,
					//queryText : this.withinSetExperimentCombo.getValue(),
					width : 500,
					height : 500
				}),
			defaultTpl: new Ext.XTemplate('<tpl for="."><div style="padding-bottom:7px;">'+
				'<a target="_blank" href="/Gemma/expressionExperiment/showExpressionExperiment.html?id=',
				'{id}"', ' ext:qtip="{shortName}">{shortName}</a>&nbsp; {name} <span style="color:grey">({taxon})</span></div></tpl>'),
			
			defaultPreviewTitle: "Experiment Selection Preview",
			
			addingCombo: withinSetExperimentCombo
		
		});
		Gemma.ExperimentSetPreview.superclass.initComponent.call(this);
		
		this.selectionEditor.on('experimentListModified', function(newSets){
			var i;
			for (i = 0; i < newSets.length; i++) { // should only be one
				if (typeof newSets[i].expressionExperimentIds !== 'undefined' && typeof newSets[i].name !== 'undefined') {
					this.loadExperimentPreviewFromIds(newSets[i].expressionExperimentIds);
					//?? this.loadExperimentPreviewFromExperimentSet(newSets[i]);
					this.setSelectedSetValueObject(newSets[i]);
					this.updateTitle();
				}
			}
			this.listModified = true;
			
			this.fireEvent('experimentListModified', newSets);
		}, this);
		
	}
	
});

Ext.reg('Gemma.ExperimentSetPreview', Gemma.ExperimentSetPreview);