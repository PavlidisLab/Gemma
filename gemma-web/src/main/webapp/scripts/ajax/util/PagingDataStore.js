Ext.namespace('Gemma');

/**
 * 
 * @class Gemma.PagingDataStore
 * @extends Ext.data.Store
 * @version $Id$
 * @author Luke, Paul
 */

Gemma.PagingDataStore = function(config) {
	Ext.apply(this, config);

	Gemma.PagingDataStore.superclass.constructor.call(this);

	// this.on("add", this.checkStartIndex);
	// this.on("clear", this.checkStartIndex);
	// this.on("remove", this.checkStartIndex);
	// this.on("load", this.checkStartIndex);
	// this.on("dataChange", this.checkStartIndex);

	  this.on("loadexception", this.copeWithError.createDelegate(this));
}

Ext
		.extend(
				Gemma.PagingDataStore,
				Ext.data.Store,
				{

					pageSize : 10,
					currentStartIndex : 0,

					getAt : function(index) {
						return Gemma.PagingDataStore.superclass.getAt.call(
								this, this.currentStartIndex + index);
					},

					getCount : function() {
						return this.getVisibleRecords().length;
					},

					getRange : function(start, end) {

						// needed incase start or end is null
						var windowStart = start ? this.currentStartIndex
								+ start : this.currentStartIndex;
						var windowEnd = end ? this.currentStartIndex + end
								: this.getTotalCount();

						return Gemma.PagingDataStore.superclass.getRange.call(
								this, windowStart, windowEnd);
					},

					indexOf : function(record) {
						var i = this.data.indexOf(record);
						return i - this.currentStartIndex;
					},

					indexOfId : function(id) {
						var i = this.data.indexOfKey(id);
						return i - this.currentStartIndex;
					},

					add : function(records) {
						// check for duplicates -- though this shouldn't really
						// be necessary.
						for ( var i = 0, len = records.length; i < len; i++) {
							if (!this.getById(records[i].id)) {
								Gemma.PagingDataStore.superclass.add.call(this,
										[ records[i] ]);
							}
						}
					},

					insert : function(index, records) {
						// check for duplicates
						for ( var i = 0, len = records.length; i < len; i++) {
							if (!this.getById(records[i].id)) {
								Gemma.PagingDataStore.superclass.insert.call(
										this, index, [ records[i] ]);
							}
						}
					},

					load : function(options) {
						options = options || {};
						if (options.params !== undefined
								&& (options.params.start !== undefined || options.params.limit !== undefined)) {
							// fires when paging as opposed to a 'real' load.

							if (this.fireEvent("beforeload", this, options) !== false) {

								if (options.params.start !== undefined) {
									this.currentStartIndex = options.params.start;
								}
								if (options.params.limit !== undefined) {
									this.pageSize = options.params.limit;
								}
								var records = this.getVisibleRecords();
								this.fireEvent("datachanged", this);
								this.fireEvent("load", this, records, options);
							}
						} else {
							/*
							 * Unfortunately, if we don't do this, the
							 * gridview's doRender method can get muddled. See
							 * bug 1686 and 1072
							 */
							// this.currentStartIndex = 0;
							Gemma.PagingDataStore.superclass.load.call(this,
									options);
						}
					},

					checkStartIndex : function() {
						var previousIndex = this.currentStartIndex;
						while (this.currentStartIndex >= this.totalLength) {
							this.currentStartIndex -= this.pageSize;
						}
						if (this.currentStartIndex < 0) {
							this.currentStartIndex = 0;
						}

						if (this.currentStartIndex != previousIndex) {
							// update the paging toolbar...
							this.fireEvent("load", this, records, {
						params : {
							start : this.currentStartIndex
						}
					});
				}
			},

			copeWithError : function() {
				this.currentStartIndex = 0;
				this.fireEvent("datachanged", this);
			},

			getVisibleRecords : function() {
				return this.getRange(0, this.pageSize - 1);
			}

				});