Ext.namespace('Gemma');

/**
 * ComboBox to show DatasetGroups (aka ExpressionExperimentSets). Configure this with a DatsetGroupStore.
 * 
 * @class Gemma.DatasetGroupCombo
 * @extends Ext.form.ComboBox
 * @version $Id$
 * @author paul
 * @see DatasetGroupComboPanel for a convenient way to display this with an edit button.
 */
Gemma.DatasetGroupCombo = Ext.extend(Gemma.StatefulRemoteCombo, {

	displayField : 'name',
	valueField : 'id',
	editable : false,
	loadingText : Gemma.StatusText.Loading.generic,
	listWidth : 250,
	triggerAction : 'all',

	forceSelection : true,

	lastQuery : '', // is this needed.

	emptyText : 'Select scope',

	suppressFiltering : false,

	stateId : 'dataset-group-combo-state',

	/**
	 * Hide all entries except that indicated by the given taxonId.
	 * 
	 * @param {}
	 *            taxonId
	 */
	filterByTaxon : function(taxonId) {
		if (this.suppressFiltering) {
			return;
		}

		// all taxa is taxonId=-1
		if(taxonId===-1){
			this.store.clearFilter();
			return;
		}
		this.doQueryBy(function(record, id) {
					if (!record.get("taxonId")) {
						return true; // in case there is none.
					} else if (taxonId === record.get("taxonId")) {
						return true;
					} else {
						return false;
					}
				});

		// clear the field if the current selection is for a record that was filtered out.
		if (this.store.getSelected() && this.store.getSelected().get("taxonId") !== taxonId) {
			this.setValue("");
		}
	}, 

	/**
	 * 
	 * @param {}
	 *            fn
	 */
	doQueryBy : function(fn) {
		this.store.clearFilter();
		this.store.filterBy(fn, this);
		this.onLoad(); // needed?
	},

	/**
	 * 
	 * @param {}
	 *            name
	 */
	selectByName : function(name) {

		if (name === undefined || !name) {
			return null;
		}

		this.store.clearFilter(false);

		this.suppressFiltering = true;
		var index = this.store.findBy(function(record, i) {
					return record.get("name").toLowerCase() === name.toLowerCase();
				});

		if (index >= 0) {
			var rec = this.store.getAt(index);
			this.setValue(rec.get("id"));
			this.store.setSelected(rec);

			this.suppressFiltering = false;
			this.filterByTaxon(rec.get('taxonId'));
			this.fireEvent("select", this, rec, index);
			return rec;
		} else {
			return null;
		}
	},

	/**
	 * 
	 */
	initComponent : function() {
		if (!this.store) {
			this.store = new Gemma.DatasetGroupStore();
		}

		this.tpl = new Ext.XTemplate('<tpl for="."><div ext:qtip="{description} ({numExperiments} members)" class="x-combo-list-item">{name}{[ values.taxon ? " (" + values.taxon.scientificName + ")" : "" ]}</div></tpl>');
		this.tpl.compile();

		Gemma.DatasetGroupCombo.superclass.initComponent.call(this);

		if(this.allExperiments){
			this.store.load({
							params : [],
							add : false,
							callback: function (){
									// add an option so user can select all experiments
									var allExperimentsRecord = new this.recordType({
										id: '-1',
										name: 'All Experiments',
										description: 'All expression experiments in the database.',
										numExperiments: -10,
										currentUserHasWritePermission: false
									},0);
									this.insert(0, allExperimentsRecord);
							}
						});
		}else{
			this.store.load({
							params : [],
							add : false
						});	
		}
		
		
		this.on("select", function(cb, rec, index) {
					this.store.setSelected(rec);
				});
	}

});

