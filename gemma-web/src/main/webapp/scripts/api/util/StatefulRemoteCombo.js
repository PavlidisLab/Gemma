/*
 * Gemma project
 */
Ext.namespace( 'Gemma' );

/**
 * Abstract utility class. Subclass this to make a stateful combo that loads its contents from the remote store.
 * Subclasses should set the stateId and provide the store.
 * <p>
 * This address the problem of having to 1) read the cookie and 2) load the combo contents and then 3) restore state
 * from the cookie in the right order.
 * <p>
 * The store is assumed to have records that have an 'id' parameter (true for all Gemma entities). Fires the 'ready'
 * event when the combo is ready to go.
 * 
 * @class Gemma.StatefulRemoteCombo
 * @extends Ext.form.ComboBox
 * @version $Id$
 * @author paul
 * @see DatasetGroupCombo, ArrayDesignCombo, TaxonCombo for concrete implementations.
 */
Gemma.StatefulRemoteCombo = Ext.extend( Ext.form.ComboBox, {

   mode : 'local', // we control it manually.
   lazyInit : false, // important! Initialize immediately.
   isReady : false,

   /**
    * We are stateful: we remember the last selected item. We also save state when 'ready', because otherwise the cookie
    * gets wiped (is this a browser thing?) and there will be no state unless the user touches the combo.
    */
   stateEvents : [ 'select' ],

   /**
    * Private. Custom cookie config for restoring state. This method will be called by the state manager. Note that we
    * don't actually immediately 'select' the value, because the combo might not yet be ready. Instead we wait for the
    * 'load' event.
    * 
    * @param {}
    *           state
    */
   applyState : function( state ) {
      if ( state && state.id ) {
         this.setState( state.id );
      }
   },

   /**
    * Private. Called by state manager when serializing the state.
    * 
    * @return {} state
    * @private
    */
   getState : function() {
      // we can't get to the selection in the store by 'getSelected': race.
      r = this.getSelected();

      if ( r ) {
         return ({
            id : r.get( 'id' )
         });
      }
   },

   /**
    * 
    * @return {}
    * @memberOf Gemma.StatefulRemoteCombo
    */
   getSelected : function() {
      if ( this.getStore() && this.view ) {
         var index = this.view.getSelectedIndexes()[0];
         return this.getStore().getAt( index );
      }
   },

   /*
    * Private. Called only after the underlying store is ready. At this point applyState would already have been called.
    */
   restoreState : function() {
      if ( this.storedState ) {
         this.selectById( this.storedState, true );
         delete this.storedState;
      }

      if ( this.getSelected() ) {
         this.fireEvent( 'ready', this.getSelected().data );
      } else {
         this.fireEvent( 'ready' );
      }

      this.isReady = true;
   },

   /**
    * Private.
    * 
    * @param {}
    *           state
    * @private
    */
   setState : function( state ) {
      if ( this.isReady ) {
         this.selectById( state, true );
      } else {
         this.storedState = state;
      }
   },

   /**
    * Note: Fires the select event
    * 
    * @param {}
    *           id
    */
   selectById : function( id, suppressEvent ) {
      if ( this.store ) {
         var index = this.store.findExact( "id", id );
         if ( index >= 0 ) {
            var rec = this.store.getAt( index );
            if ( typeof this.store.setSelected == 'function' ) {
               this.store.setSelected( rec );
            }
            this.setValue( rec.get( this.displayField ) );
            // We might not want to fire select event. See bug 1872
            if ( suppressEvent !== undefined && !suppressEvent ) {
               this.fireEvent( "select", this, rec, index );
            }
         }
      }
   },

   /**
    */
   initComponent : function() {
      Gemma.StatefulRemoteCombo.superclass.initComponent.call( this );
      this.addEvents( 'ready' );
      this.store.on( 'beforeload', function( field, query ) {
         if ( this.loadMask ) {
            this.loadMask.show();
         }
         this.addClass( "x-loading" );
         this.disable();
      }, this );

      this.on( 'ready', function( field, results ) {
         if ( this.loadMask ) {
            this.loadMask.hide();
         }
         this.enable();
         this.removeClass( "x-loading" );
      } );

      this.store.on( "load", this.restoreState, this, {
         delay : 100,
         single : true
      } );
   }

} );