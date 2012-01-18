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
	resetExperimentPreview : function() {
		this.preview.resetPreview();
	},
	showExperimentPreview : function() {
		this.preview.showPreview();
	},
	collapsePreview : function(){
		if(typeof this.preview !== 'undefined'){
			this.preview.collapsePreview();
		}
	},
	maskExperimentPreview : function() {
		if (!this.loadMask && this.getEl()) {
			this.loadMask = new Ext.LoadMask(this.getEl(), {
						msg : Gemma.StatusText.Loading.experiments
					});
		}
		if (this.loadMask) {
			this.loadMask.show();
		}
	},
	/**
	 * Show the selected eeset members
	 */
	loadExperimentOrGroup : function(record, query) {

		this.selectedExperimentOrGroup = record.data;
		
		if(this.selectedExperimentOrGroup.resultValueObject instanceof ExpressionExperimentSetValueObject){
			this.setSelectedExpressionExperimentSetValueObject(this.selectedExperimentOrGroup.resultValueObject);
			this.isExperiment = false;
			this.isExperimentSet = true;
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
		if (this.isExperimentSet) {
			var eeIds = this.getSelectedExpressionExperimentSetValueObject().expressionExperimentIds;
			this.experimentGroupId = id;
			if (!eeIds || eeIds === null || eeIds.length === 0) {
				return;
			}

			this.preview.setTaxonId(taxonId);
			this.preview.loadExperimentPreviewFromExperimentSet(this.selectedExperimentOrGroup.resultValueObject);

		}
		// load single experiment if experiment was selected
		else {
			this.experimentIds = [id];
			this.selectedExperimentOrGroup.memberIds = [id];
			// reset the experiment preview panel content
			this.resetExperimentPreview();

			this.preview.setTaxonId(taxonId);
			this.preview.loadExperimentPreviewFromExperiments([this.selectedExperimentOrGroup.resultValueObject]);
			
		}
	},

	/**
	 * update the contents of the experiment preview box and the this.experimentIds value
	 * using a list of experiment Ids
	 * 
	 * @param {Number[]} ids an array of experimentIds to use
	 */
	loadExperiments : function(ids) {

		// store selected ids for searching
		this.searchForm.experimentIds.push(ids);
		this.experimentIds = ids;

		this.preview.loadExperimentPreviewFromIds(ids);

	},
	initComponent : function() {

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
					
					this.preview.setTaxonId(record.get("taxonId"));

					// store the eeid(s) selected and load some EE into the
					// previewer
					// store the taxon associated with selection
					var query = combo.store.baseParams.query;
					this.loadExperimentOrGroup(record, query);
					this.preview.showPreview();

					// if this was the first time a selection was made using
					// this box
					if (combo.startValue === '' && this.newBoxTriggered === false) {
						this.fireEvent('madeFirstSelection');
						this.newBoxTriggered = true;
						this.helpBtn.hide();
						// this.relayEvents(this.experimentCombo, ['select']);
					}
					combo.disable().hide();
					//this.selectionTitle.update(combo.getValue());
					this.helpBtn.hide();
					this.doLayout();

				}, this);



		this.preview = new Gemma.ExperimentSetPreview();
							
		this.preview.on('experimentListModified', function(newSets) {
			var i;
			for (i = 0; i < newSets.length; i++) { // should only be one
				if (typeof newSets[i].expressionExperimentIds !== 'undefined' && typeof newSets[i].name !== 'undefined') {
					// update record
					this.selectedExperimentOrGroup.resultValueObject = newSets[i];
					this.setSelectedExpressionExperimentSetValueObject(newSets[i]);
				}
			}
		}, this);

		this.preview.on('doneModification', function() {
			this.searchForm.getEl().unmask();
		}, this);
		
		this.preview.on('removeMe', function() {
			this.fireEvent('removeExperiment');
		}, this);
		

		this.helpBtn = new Gemma.InlineHelpIcon({
			tooltipText: Gemma.HelpText.WidgetDefaults.ExperimentSearchAndPreview.widgetHelpTT
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
			}, this.preview]
		});
		Gemma.ExperimentSearchAndPreview.superclass.initComponent.call(this);
	}
});

Ext.reg('experimentSearchAndPreview', Gemma.ExperimentSearchAndPreview);
