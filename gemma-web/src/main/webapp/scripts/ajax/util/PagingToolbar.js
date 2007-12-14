Ext.namespace('Ext.Gemma');

/* Ext.Gemma.PagingToolbar is an extension of Ext.PagingToolbar that compensates for a
 * bug in how the current active page is calculated.
 *
 * An alternative to using this class would be to patch the Ext code thusly:
 *
 * --- gemma-web/src/main/webapp/scripts/ext/ext-all-debug.js
 * +++ gemma-web/src/main/webapp/scripts/ext/ext-all-debug.js (FIXED)
 * @@ -15764,7 +15764,7 @@
 *  
 *          onLoad : function(ds, r, o){
 * -       this.cursor = o.params ? o.params.start : 0;
 * +       this.cursor = o.params ? ( o.params.start !=== undefined ? 0.params.start : 0 ) : 0;
 *         var d = this.getPageData(), ap = d.activePage, ps = d.pages;
 *
 */
Ext.Gemma.PagingToolbar = function ( el, ds, config ) {

	Ext.Gemma.PagingToolbar.superclass.constructor.call( this, el, ds, config );

}

Ext.extend( Ext.Gemma.PagingToolbar, Ext.PagingToolbar, {

	onLoad : function( ds, r, o ) {
		/* temporarily set options.params.start to 0 if it's undefined as it
		 * won't be caught by the test in Ext.PagingToolbar.onLoad...
		 */
		var definedStartParameter = false;
		if ( o.params && o.params.start === undefined ) {
			o.params.start = 0;
			definedStartParameter = true;
		}
		
		Ext.Gemma.PagingToolbar.superclass.onLoad.call( this, ds, r, o );
		
		/* if we defined options.parm.start above, undefine it so we don't
		 * change behaviour elsewhere...
		 */
		if ( definedStartParameter )
			delete o.params.start;
	}

} );