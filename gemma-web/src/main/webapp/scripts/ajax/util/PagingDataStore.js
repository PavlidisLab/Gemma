Ext.namespace('Gemma');

/**
 * 
 * @class Gemma.PagingDataStore
 * @extends Ext.data.Store
 */
Gemma.PagingDataStore = Ext.extend(Ext.data.Store, {

	initComponent : function() {

		Gemma.PagingDataStore.superclass.initComponent.call(this);

		this.on("add", this.checkStartIndex);
		this.on("clear", this.checkStartIndex);
		this.on("remove", this.checkStartIndex);
		this.on("load", this.checkStartIndex);
		this.on("dataChange", this.checkStartIndex);
	},

	pageSize : 10,
	currentStartIndex : 0,

	getAt : function(index) {
		return Gemma.PagingDataStore.superclass.getAt.call(this, this.currentStartIndex + index);
	},

	getCount : function() {
		return this.getVisibleRecords().length;
	},

	getRange : function(start, end) {

		// needed incase start or end is null
		var windowStart = start ? this.currentStartIndex + start : this.currentStartIndex;
		var windowEnd = end ? this.currentStartIndex + end : this.getTotalCount();

		// if (windowEnd > this.currentStartIndex + this.pageSize - 1) {
		// windowEnd = this.currentStartIndex + this.pageSize - 1;
		// }
		return Gemma.PagingDataStore.superclass.getRange.call(this, windowStart, windowEnd);
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
		// check for duplicates -- though this shouldn't really be necessary.
		for (var i = 0, len = records.length; i < len; i++) {
			if (!this.getById(records[i].id)) {
				Gemma.PagingDataStore.superclass.add.call(this, [records[i]]);
			}
		}
	},

	insert : function(index, records) {
		// check for duplicates
		for (var i = 0, len = records.length; i < len; i++) {
			if (!this.getById(records[i].id)) {
				Gemma.PagingDataStore.superclass.insert.call(this, index, [records[i]]);
			}
		}
	},

	load : function(options) {
		options = options || {};
		if (options.params !== undefined && (options.params.start !== undefined || options.params.limit !== undefined)) {
			if (this.fireEvent("beforeload", this, options) !== false) {
				if (options.params.start !== undefined) {
					this.currentStartIndex = options.params.start;
				}
				if (options.params.limit !== undefined) {
					this.pageSize = options.params.limit;
				}
				var total = this.getTotalCount();
				var records = this.getVisibleRecords();
				this.fireEvent("datachanged", this);
				this.fireEvent("load", this, records, options);
			}
		} else {
			// not resetting to the first page by default as per bug 1072
			if (options.resetPage) {
				this.currentStartIndex = 0;
			}
			Gemma.PagingDataStore.superclass.load.call(this, options);
		}
	},

	loadRecords : function(o, options, success) {
		Gemma.PagingDataStore.superclass.loadRecords.call(this, o, options, success);

	},

	remove : function(record) {
		Gemma.PagingDataStore.superclass.remove.call(this, record);
	},

	removeAll : function() {
		Gemma.PagingDataStore.superclass.removeAll.call(this);
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
			this.fireEvent("load", this, [], {
						params : {
							start : this.currentStartIndex
						}
					});
		}
	},

	getVisibleRecords : function() {
		return this.getRange(0, this.pageSize - 1);
	}

});