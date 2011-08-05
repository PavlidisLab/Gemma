
/**
 * A small panel which can show a DatasetGroupCombo an edit button which goes to a DatasetGroupEditor
 * 
 * @class Gemma.DatasetGroupComboPanel
 * @extends Ext.Panel
 * @see DatasetGroupCombo and DatasetGroupStore.
 */
Gemma.DatasetGroupComboPanel = Ext.extend(Ext.Panel, {
			layout : 'table',
			name : 'eesetpanel',
			layoutConfig : {
				columns : 2
			},
			defaults : {
				bodyStyle : 'padding:3px'
			},
			border : false,
			autoWidth : true,
			stateful : false,

			getSelected : function() {
				return this.store.getSelected();
			},

			filterByTaxon : function(taxon) {
				if (taxon) {
					this.combo.filterByTaxon(taxon.id);
				}
			},

			initComponent : function() {

				if (!this.store) {
					this.store = new Gemma.DatasetGroupStore();
				}

				this.combo = new Gemma.DatasetGroupCombo({
							width : 175,
							store : this.store
						});

				Ext.apply(this, {
							items : [this.combo, new Ext.Button({
												text : "Edit",
												anchor : '',
												tooltip : "View dataset group interface to modify or create sets",
												handler : function() {
													if (!this.dcp) {
														this.initDatasetGroupEditor();
													}
													this.dcp.show();
												},
												scope : this
											})]
						});

				Gemma.DatasetGroupComboPanel.superclass.initComponent.call(this);

				/*
				 * Just pass these events through
				 */
				this.relayEvents(this.combo, ['ready', 'select']);

			},

			/**
			 * Lazy initialization of the DatasetGroup editor.
			 */
			initDatasetGroupEditor : function() {
				this.dcp = new Gemma.DatasetGroupEditor({
							modal : true,
							datasetGroupStore : this.store
						});
				/*
				 * Handlers for when we're done editing.
				 */
				this.dcp.on("select", function(sel) {
							if (sel) {
								this.combo.setValue(sel.get("name"));
							}
						}.createDelegate(this));

				this.dcp.on("delete-set", function(rec) {
							if (this.store.getPreviousSelection()) { // fixme
								this.combo.setValue(this.store.getPreviousSelection().get("name"));
							} else {
								this.combo.setValue("");
							}
						}.createDelegate(this));

				this.dcp.on("commit", function(sel) {
							if (sel) {
								this.combo.setValue(sel.get("name"));
							}
						}.createDelegate(this));

				this.relayEvents(this.dcp, ['commit', 'select']);
			},

			selectByName : function(name) {
				this.combo.selectByName(name);
			}

		});