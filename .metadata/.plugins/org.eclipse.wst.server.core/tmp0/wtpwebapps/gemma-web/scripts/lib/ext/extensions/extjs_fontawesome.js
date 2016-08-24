// based on https: // github.com/aswinramakrish/ext-js_font-awesome/blob/master/extjs_fa_override.js
// note: uses jquery

Ext.createSequence( Ext.tree.TreeNode.prototype, 'render', function( treenode ) {
   $( '.x-tree-node-icon' ).removeClass( 'x-tree-node-icon' ).replaceWith( function() {
      if ( typeof ($( this ).attr( 'class' )) != 'undefined' )
         return '<i class="' + $( this ).attr( 'class' ) + '"/>';
      else
         $( this ).addClass( '.x-tree-node-icon' );
   } );
} );

Ext.createInterceptor( Ext.tree.TreeNode.prototype, 'setIconCls', function( cls ) {
   $( $( this )[0].ui.elNode ).find( 'i' ).removeClass().addClass( cls );
} );

// /// PP added

Ext.createSequence( Ext.Button.prototype, 'render', function( button ) {
   $( '.x-btn-icon' ).removeClass( 'x-btn-icon' ).replaceWith( function() {
      if ( typeof ($( this ).attr( 'class' )) != 'undefined' )
         return '<i class="' + $( this ).attr( 'class' ) + '"/>';
      else
         $( this ).addClass( '.x-btn-icon' );
   } );
} );

Ext.createInterceptor( Ext.Button.prototype, 'setIconCls', function( cls ) {
   $( $( this )[0].ui.elNode ).find( 'i' ).removeClass().addClass( cls );
} );
