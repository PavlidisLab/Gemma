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

Ext.namespace( 'Gemma' );

/**
 * Serverside: SearchResultDisplayObject (generic), here wraps either a single experiment or eeSet (valueobject)
 */
Gemma.ExperimentAndExperimentGroupComboRecord = Ext.data.Record.create( [ {
   name : "name",
   type : "string"
}, {
   name : "description",
   type : "string"
}, {
   name : "isGroup",
   type : "boolean"
}, {
   name : "size",
   type : "int"
}, {
   name : "taxonId",
   type : "int",
   defaultValue : "-1"
}, {
   name : "taxonName",
   type : "string",
   defaultValue : ""
}
 , {
 name : "memberIds",
 defaultValue : []
 }
, {
   name : "resultValueObject"
}, {
   name : "userOwned",
   type : "boolean"
} ] );

Gemma.ExperimentAndExperimentGroupCombo = Ext
   .extend(
      Ext.form.ComboBox,
      {

         name : 'experimentAndExperimentGroupCombo',
         displayField : 'name',
         width : 160,
         listWidth : 450, // ridiculously large so IE displays it properly
         lazyInit : false, // true to not initialize the list for this combo until the field is focused (defaults to
         // true)
         triggerAction : 'all', // run the query specified by the allQuery config option when the trigger is clicked
         allQuery : '', // loading of auto gen and user's sets handled in Controller when query = ''
         enableKeyEvents : true,

         loadingText : 'Searching ...', // FIXME if field is blank use 'loading ...'
         emptyText : "Find experiments by keyword",
         listEmptyTextBlankQuery : 'Search by keyword or ID',
         listEmptyText : 'No results',
         minChars : 3,
         selectOnFocus : false,
         autoSelect : false,
         forceSelection : true,
         typeAhead : false,
         taxonId : null,

         lastQuery : null, // used for query queue fix

         mode : 'remote',
         queryDelay : 800, // default = 500
         listeners : {
            specialkey : function( formField, e ) { // needed for query queue fix
               // e.HOME, e.END, e.PAGE_UP, e.PAGE_DOWN,
               // e.TAB, e.ESC, arrow keys: e.LEFT, e.RIGHT, e.UP, e.DOWN
               if ( e.getKey() === e.TAB || e.getKey() === e.RIGHT || e.getKey() === e.DOWN ) {
                  this.expand();
               } else if ( e.getKey() === e.ENTER ) {
                  if ( this.getValue() && this.getValue() !== null ) {
                     this.doQuery( this.getValue(), true );
                  } else {
                     this.doQuery( '', true );
                  }
               } else if ( e.getKey() === e.ESC ) {
                  this.collapse();
               }
            },
            beforequery : function( qe ) {
               delete qe.combo.lastQuery;
            }
         },

         // overwrite ComboBox onLoad function to get rid of query text being selected after a search returns
         // (this was interfering with the query queue fix)
         // only change made is commented out line
         onLoad : function() {
            if ( !this.hasFocus ) {
               return;
            }
            if ( this.store.getCount() > 0 || this.listEmptyText ) {
               this.expand();
               this.restrictHeight();
               if ( this.lastQuery === this.allQuery ) {
                  if ( this.editable ) {
                     // this.el.dom.select(); // change from superclass
                  }

                  if ( this.autoSelect !== false && !this.selectByValue( this.value, true ) ) {
                     this.select( 0, true );
                  }
               } else {
                  if ( this.autoSelect !== false ) {
                     this.selectNext();
                  }
                  if ( this.typeAhead && this.lastKey !== Ext.EventObject.BACKSPACE
                     && this.lastKey !== Ext.EventObject.DELETE ) {
                     this.taTask.delay( this.typeAheadDelay );
                  }
               }
            } else {
               this.collapse();
            }

         }, // end onLoad overwrite

         /**
          * Filters the contents on the basis of whether the mode is 'coexpresssion' or differential expression'
          * 
          * @param {String}
          *           mode
          * @memberOf Gemma.ExperimentAndExperimentGroupCombo
          */
         setMode : function( mode ) {
            // console.log( "set mode = " + mode );
            /*
             * filter the results
             */
            if ( mode == 'diffex' ) {
               this.getStore().filterBy( function( rec, id ) {
                  var r = rec.get( 'resultValueObject' );
                  if ( r instanceof FreeTextExpressionExperimentResultsValueObject ) {
                     return r.numWithDifferentialExpressionAnalysis > 0;
                  } else if ( r instanceof SessionBoundExpressionExperimentSetValueObject ) {
                     return r.numWithDifferentialExpressionAnalysis > 0;
                  } else if ( r instanceof ExpressionExperimentSetValueObject ) {
                     return r.numWithDifferentialExpressionAnalysis > 0;
                  } else if ( r instanceof ExpressionExperimentValueObject ) {
                     return r.hasDifferentialExpressionAnalysis;
                  }
                  return true;

               } );
            } else if ( mode == 'coex' ) {
               this.getStore().filterBy( function( rec, id ) {
                  var r = rec.get( 'resultValueObject' );
                  if ( r instanceof FreeTextExpressionExperimentResultsValueObject ) {
                     return r.numWithCoexpressionAnalysis > 0;
                  } else if ( r instanceof SessionBoundExpressionExperimentSetValueObject ) {
                     return r.numWithCoexpressionAnalysis > 0;
                  } else if ( r instanceof ExpressionExperimentSetValueObject ) {
                     return r.numWithCoexpressionAnalysis > 0;
                  } else if ( r instanceof ExpressionExperimentValueObject ) {
                     return r.hasCoexpressionAnalysis;
                  }
                  return true;
               } );
            } else {
               console.log( "unknown mode" );
            }
         },

         /**
          * Parameters for AJAX call.
          * 
          * @private
          * @Override
          * @param {string}
          *           query
          * 
          * @return {}
          */
         getParams : function( query ) {
            // console.log( "ExperimentCombo getting parameters for search: " + query );
            return [ query, this.taxonId ];
         },

         /**
          * Was: getSelected but changed to match GeneAndGeneGroupCombo
          * 
          * @returns
          */
         getExperimentGroup : function() {
            if ( this.getRawValue() === '' ) {
               return null;
            }
            return this.selectedExpressionExperimentGroup;
         },

         /**
          * @memberOf Gemma.ExperimentAndExperimentGroupComboRecord
          */
         initComponent : function() {

            /*
             * Colors:
             * 
             * Dataset, not an EEset: #ECF4FF - light blue
             * 
             * Modified session-bound: #FFFFFF - white
             * 
             * Free-text: #FFFFE3 - almost white
             * 
             * user-owned: #FFECEC (pink)
             * 
             * Default, also for session-bound. Dataset, not an EEset #EBE3F6 ? - light purple
             */

            var eeTpl = new Ext.XTemplate(
               '<div style="font-size:11px;background-color:#ECF4FF" class="x-combo-list-item" '
                  + 'ext:qtip="{name}: {description} ({taxonName})"><b>{name}</b>: {description} <span style="color:grey">({taxonName}) <tpl if="resultValueObject.hasCoexpressionAnalysis">C</tpl>'
                  + '&nbsp;<tpl if="resultValueObject.hasDifferentialExpressionAnalysis">D</tpl></span></div>' );
            var modifiedSessionTpl = new Ext.XTemplate(
               '	<div style="font-size:11px;background-color:#FFFFFF" class="x-combo-list-item" ext:qtip="{name}:'
                  + ' {description} ({size}) ({taxonName})"><b>{name}</b>:  <span style="color:red">Unsaved</span> {description} ({size}) <span style="color:grey">({taxonName})</span></div>' );
            var freeTxtTpl = new Ext.XTemplate(
               '<div style="font-size:11px;background-color:#FFFFE3" class="x-combo-list-item" ext:qtip="{name}: {description} ({size}) ({taxonName})">'
                  + '<b>{name}</b>: {description} ({size}) <span style="color:grey">({taxonName})</span></div>' );
            var userOwnedDbSetTpl = new Ext.XTemplate(
               '<div style="font-size:11px;background-color:#FFECEC" class="x-combo-list-item" ext:qtip="{name}: {description} ({size}) ({taxonName})">'
                  + '<b>{name}</b>: {description} ({size}) <span style="color:grey">({taxonName})</span></div>' );
            var dbSetTpl = new Ext.XTemplate(
               '<div style="font-size:11px;background-color:#EBE3F6" class="x-combo-list-item" ext:qtip="{name}: {description} ({size}) ({taxonName})">'
                  + '<b>{name}</b>: {description} ({size}) <span style="color:grey">({taxonName})</span></div>' );
            var dbMasterSetTpl = new Ext.XTemplate(
               '<div style="font-size:11px;background-color:#E6B2FF" class="x-combo-list-item" ext:qtip="{name}: {description} ({size}) ({taxonName})">'
                  + '<b>{name}</b>: {description} ({size}) <span style="color:grey">({taxonName})</span></div>' );
            var sessionSetTpl = dbSetTpl;
            var defaultTpl = dbSetTpl;

            this.urlInitiatedQuery = false;
            Ext.apply( this, {
               // format fields to show in combo, only show size in brackets if the entry is a group
               tpl : new Ext.XTemplate( '<tpl for=".">' + '{[ this.renderItem(values) ]}' + '</tpl>', {
                  renderItem : function( values ) {
                     if ( values.resultValueObject instanceof FreeTextExpressionExperimentResultsValueObject ) {
                        return freeTxtTpl.apply( values );
                     } else if ( values.resultValueObject instanceof SessionBoundExpressionExperimentSetValueObject ) {
                        if ( values.resultValueObject.modified ) {
                           return modifiedSessionTpl.apply( values );
                        } else {
                           return sessionSetTpl.apply( values );
                        }
                     } else if ( values.resultValueObject instanceof ExpressionExperimentSetValueObject ) {
                        if ( values.userOwned ) {
                           return userOwnedDbSetTpl.apply( values );
                        } else if ( values.name.match( "Master set for" ) ) {
                           return dbMasterSetTpl.apply( values );
                        } else {
                           return dbSetTpl.apply( values );
                        }
                     } else if ( values.resultValueObject instanceof ExpressionExperimentValueObject ) {
                        return eeTpl.apply( values );
                     }
                     return defaultTpl.apply( values );
                  }
               } ),
               store : {
                  reader : new Ext.data.ListRangeReader( {}, Gemma.ExperimentAndExperimentGroupComboRecord ),
                  proxy : new Ext.data.DWRProxy( ExpressionExperimentController.searchExperimentsAndExperimentGroups ),
                  autoLoad : false
               }
            } );

            Gemma.ExperimentAndExperimentGroupCombo.superclass.initComponent.call( this );

            /** *** start of query queue fix **** */

            // enableKeyEvents config required
            this.on( 'keypress', function( textfield, eventObj ) {
               // this is set to true when query returns
               this.displayingComboValueToQueryMatch = false;
            } );

            this.getStore().on( 'beforeload', function( store, opts ) {
               // fires before the loader request so not very useful.
            }, this );

            this.getStore().on(
               /* fires after loading.. */
               'load',
               function( store, records, options ) {
                  var query = (options.params) ? options.params[0] : null;
                  // if the query for which the store is returning is not the same as the last query made with the combo
                  // clear these results and add the previous query's results

                  // if ( records.length > 0 && records[0].originalQuery
                  // && records[0].originalQuery !== this.getValue() ) {
                  // /*
                  // * Abort loading; but the load event is fired too late. This could cause problems.
                  // */
                  // console.log( 'aborting previous query for ' + records[0].originalQuery );
                  // store.remove( records );
                  // }
                  // console.log( 'valid results match the query=' + this.getValue() );

                  if ( this.urlInitiatedQuery ) {
                     this.fireEvent( "select", this, records[0] );
                     // } else if ( this.getValue() !== records[0].originalQuery ) {

                  } else if ( this.getValue() !== query ) {

                     // replace returned records with those of last matching query
                     store.removeAll();
                     if ( this.prevQuery === this.getValue() ) {
                        store.add( this.prevRecords );
                     }

                     // removing records works to prevent wrong/old results from popping up, but this also
                     // removes the loading text which should be shown if there's a newer query that's still working

                     // if a valid query has already returned, don't replace results with loading text
                     // if the valid query hasn't returned yet, show loading text
                     if ( !this.displayingComboValueToQueryMatch ) {
                        // --- from Combo.js to show loading text ---
                        this.innerList.update( this.loadingText ? '<div class="loading-indicator">' + this.loadingText
                           + '</div>' : '' );
                        this.restrictHeight();
                        this.selectedIndex = -1;
                        // --- end of code from Combo.js ---
                     }
                  } else {
                     this.displayingComboValueToQueryMatch = true;
                     this.prevRecords = this.store.getRange();
                     this.prevQuery = (options.params) ? options.params[0] : null;

                     // special case for empty prompted record set
                     if ( this.store.getCount() === 0 && (this.prevQuery === '' || this.prevQuery === null) ) {
                        this.innerList.update( this.listEmptyTextBlankQuery ? '' + this.listEmptyTextBlankQuery + ''
                           : '' );
                     }

                  }

               }, this );
            /** *** end of query queue fix **** */

            this.on( 'focus', function( field ) {
               // if the text field is blank, show any public automatically available and user's own groups
               setTimeout( function() {
                  if ( this.getValue() === '' ) {
                     this.doQuery( '', true );
                     this.lastQuery = null; // needed for query queue fix
                  }
               }.createDelegate( this ), 1250 );
            }, this );

            this.addEvents( "experimentGroupUrlSelectionComplete" );
         },

         /**
          * @override
          */
         reset : function() {
            Gemma.ExperimentAndExperimentGroupCombo.superclass.reset.call( this );
            this.lastQuery = null;

            if ( this.tooltip ) {
               this.tooltip.destroy();
            }
         },

         setTaxonId : function( id ) {
            this.taxonId = id;
         }
      } );