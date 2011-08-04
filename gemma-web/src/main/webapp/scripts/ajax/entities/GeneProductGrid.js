/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * 
 */
Ext.namespace('Gemma');

/**
 * 
 */
Gemma.GeneProductGrid = Ext.extend(Gemma.GemmaGridPanel, {

			record : Ext.data.Record.create([{
						name : "id"
					}, {
						name : "name"
					}, {
						name : "description"
					}, {
						name : "accessions"
					}, {
						name : "type",
						convert : function(d) {
							return d.value;
						}.createDelegate()
					}]),

			initComponent : function() {
				Ext.apply(this, {
							columns : [{
										header : "Name",
										dataIndex : "name"
									}, {
										header : "Type",
										dataIndex : "type"
									}, {
										header : "Description",
										dataIndex : "description"
									}, {
						dataIndex : "accessions",
						header : "accessions"
					}],

							store : new Ext.data.Store({
										proxy : new Ext.data.DWRProxy(GeneController.getProducts),
										reader : new Ext.data.ListRangeReader({
													id : "id"
												}, this.record),
										remoteSort : false
									})
						});

				Gemma.GeneProductGrid.superclass.initComponent.call(this);

				this.getStore().setDefaultSort('type', 'name');

				this.getStore().load({
							params : [this.geneid]
						});
			}

		});
Ext.reg('geneproductgrid',Gemma.GeneProductGrid);
