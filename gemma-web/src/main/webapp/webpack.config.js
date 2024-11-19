const path = require( 'path' );
const MiniCssExtractPlugin = require( 'mini-css-extract-plugin' );
const webpack = require( 'webpack' );

module.exports = {
   plugins : [
      new MiniCssExtractPlugin( {
         filename : 'bundles/gemma-all.css'
      } ),
      // some modules don't properly import jQuery, so this plugin will "provide" it
      new webpack.ProvidePlugin( {
         $ : 'jquery',
         jQuery : 'jquery',
         // this is mostly for flotr2 which improperly imports bean and underscore
         bean : 'bean',
         _ : 'underscore'
      } )
   ],
   entry : {
      style : {
         import : './styles/index.js',
         filename : 'bundles/gemma-style.js'
      },
      lib : {
         import : './scripts/lib/index.js',
         filename : 'bundles/include.js'
      },
      api : {
         import : './scripts/api/index.js',
         filename : 'bundles/gemma-lib.js'
      }
   },
   output : {
      path : path.resolve( __dirname ),
      assetModuleFilename : 'bundles/[hash][ext][query]'
   },
   module : {
      rules : [
         {
            test : /\.css$/i,
            exclude : /node_modules/,
            use : [ MiniCssExtractPlugin.loader, "css-loader" ],
         }
      ]
   },
   devServer : {
      static : {
         directory : path.resolve( __dirname )
      },
      port : 8082,
      headers : {
         'Access-Control-Allow-Origin' : 'http://localhost:8080',
         'Access-Control-Allow-Methods' : 'GET'
      }
   }
}
