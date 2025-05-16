/*
 **    Copyright (C) 2003-2008 Institute for Systems Biology
 **                            Seattle, Washington, USA.
 **
 **    This library is free software; you can redistribute it and/or
 **    modify it under the terms of the GNU Lesser General Public
 **    License as published by the Free Software Foundation; either
 **    version 2.1 of the License, or (at your option) any later version.
 **
 **    This library is distributed in the hope that it will be useful,
 **    but WITHOUT ANY WARRANTY; without even the implied warranty of
 **    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 **    Lesser General Public License for more details.
 **
 **    You should have received a copy of the GNU Lesser General Public
 **    License along with this library; if not, write to the Free Software
 **    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA
 */

// setup namespace if not already defined
if ( typeof org === 'undefined' )
   org = {};
if ( !org.systemsbiology )
   org.systemsbiology = {};
if ( !org.systemsbiology.visualization )
   org.systemsbiology.visualization = {};

// ---------------------------------------------------------------------------------------------------------------------
// - ColorRange Class
// -
// - Description: Defines a range of colors from start to end, depending on the options.
// - Author: dburdick
// -
// ---------------------------------------------------------------------------------------------------------------------

// PP fixed this so it doesn't use Prototype.js
org.systemsbiology.visualization.DiscreteColorRange = function() {
};

org.systemsbiology.visualization.DiscreteColorRange.prototype = {
   // --------------------------------
   // Constants
   // --------------------------------
   MINCOLORS : 2,
   PASS_THROUGH_BLACK_MINCOLORS : 2,
   NO_PASS_THROUGHBLACK_MINCOLORS : 2,
   MINRGB : 0,
   MAXRGB : 255,
   BLACK_RGBA : {
      r : 255,
      g : 255,
      b : 255,
      a : 1
   },

   // --------------------------------
   // Private Attributes
   // --------------------------------

   // Setable option defaults
   _maxColors : 64, // number of colors to divide space into
   _backgroundColor : {
      r : 0,
      g : 0,
      b : 0,
      a : 1
   },
   _maxColor : {
      r : 255,
      g : 0,
      b : 0,
      a : 1
   },
   _minColor : {
      r : 0,
      g : 255,
      b : 0,
      a : 1
   },
   _emptyDataColor : {
      r : 100,
      g : 100,
      b : 100,
      a : 1
   },
   _passThroughBlack : true,

   // Other
   _dataRange : {
      min : null,
      max : null
   },
   _debug : false,
   _colorRange : null,

   // Calculated
   _colorStep : {
      r : null,
      g : null,
      b : null,
      a : null
   },
   _dataStep : null,
   _maxDataSpace : null,

   // --------------------------------
   // Public Methods
   // --------------------------------

   // constructor
   initialize : function( maxColors, dataRange, options ) {
      // check required parameters
      if ( maxColors >= 1 && dataRange ) {
         this._maxColors = maxColors;
         this._dataRange = dataRange;
      } else {
         throw ('Error in org.systemsbiology.visualization.DiscreteColorRange instantiation. required parameters not provided');
      }

      // set optional parameters
      if ( options ) {
         if ( options.maxColor )
            this._maxColor = this.niceRGBAColor( options.maxColor );
         if ( options.minColor )
            this._minColor = this.niceRGBAColor( options.minColor );
         if ( options.emptyDataColor )
            this._emptyDataColor = this.niceRGBAColor( options.emptyDataColor );
         if ( options.passThroughBlack != null && options.passThroughBlack == false ) {
            this._passThroughBlack = false;
         }
      }
      // setup color space
      this._colorRange = new Array();
      this._setupColorRange();
   },

   // when given an RBGA object it returns a canvas-formatted string for that color
   // if the RGBA is empty or ill-defined it returns a string for the empty data color
   getCellColorString : function( dataValue ) {
      var colorValue = this.getCellColorRGBA( dataValue );
      var colorString;
      if ( colorValue.r >= 0 && colorValue.g >= 0 && colorValue.b >= 0 && colorValue.a >= 0 ) {
         colorString = this.getRgbaColorString( colorValue );
      } else {
         colorString = this.getRgbaColorString( this._emptyDataColor );
      }

      // this._log("Value="+dataValue+", colorString="+colorString);
      return colorString;
   },

   // returns an RBGA object with the color for the given dataValue
   getCellColorRGBA : function( dataValue ) {
      if ( dataValue == null ) {
         return this._emptyDataColor;
      }

      var dataBin = dataValue / this._dataStep;
      var binOffset = this._dataRange.min / this._dataStep;
      var newDataBin = (dataBin - binOffset);
      // round
      if ( newDataBin < 0 )
         newDataBin = Math.ceil( newDataBin );
      else
         newDataBin = Math.floor( newDataBin );

      this._log( 'value: ' + dataValue + ' bin: ' + dataBin + ' new bin: ' + newDataBin );

      // assure bounds
      if ( newDataBin < 0 )
         newDataBin = 0;
      if ( newDataBin >= this._colorRange.length )
         newDataBin = (this._colorRange.length) - 1;
      return this._colorRange[newDataBin];
   },

   // returns the Hex color for the given dataValue
   getCellColorHex : function( dataValue ) {
      var rgba = this.getCellColorRGBA( dataValue );
      return "#" + this._RGBtoHex( rgba.r, rgba.g, rgba.b );
   },

   getRgbaColorString : function( rgba ) {
      if ( rgba.r >= 0 && rgba.g >= 0 && rgba.b >= 0 && rgba.a >= 0 ) {
         return "rgba(" + rgba.r + "," + rgba.g + "," + rgba.b + "," + rgba.a + ")";
      }
   },

   // makes sure each value of the RGBA is in a reasonable range
   niceRGBAColor : function( rgbaColor ) {
      var newRgbaColor = {
         r : null,
         g : null,
         b : null,
         a : null
      };
      newRgbaColor.r = this.niceIndividualColor( rgbaColor.r );
      newRgbaColor.g = this.niceIndividualColor( rgbaColor.g );
      newRgbaColor.b = this.niceIndividualColor( rgbaColor.b );
      if ( rgbaColor.a < 0 )
         newRgbaColor.a = 0;
      else if ( rgbaColor.a > 1 )
         newRgbaColor.a = 1;
      else
         newRgbaColor.a = rgbaColor.a;
      return newRgbaColor;
   },

   // keeps a value between MINRGB and MAXRGB
   niceIndividualColor : function( individualColor ) {
      if ( individualColor < this.MINRGB )
         return this.MINRGB;
      if ( individualColor > this.MAXRGB )
         return this.MAXRGB;
      return Math.floor( individualColor );
   },

   // --------------------------------
   // Private Methods
   // --------------------------------

   // maps data ranges to colors
   _setupColorRange : function() {
      var dataRange = this._dataRange;
      var maxColors = this._maxColors;
      var centerColor = this.BLACK_RGBA;
      var colorStep;

      if ( maxColors > 256 )
         maxColors = 256;
      if ( maxColors < 1 ) {
         maxColors = 1;
      }
      this._maxDataSpace = Math.abs( dataRange.min ) + Math.abs( dataRange.max );
      this._dataStep = this._maxDataSpace / maxColors;

      if ( this._passThroughBlack ) {
         // determine the color step for each attribute of the color
         colorStep = {
            r : 2 * this._calcColorStep( this._minColor.r, centerColor.r, maxColors ),
            g : 2 * this._calcColorStep( this._minColor.g, centerColor.g, maxColors ),
            b : 2 * this._calcColorStep( this._minColor.b, centerColor.b, maxColors ),
            a : 2 * this._calcColorStep( this._minColor.a, centerColor.a, maxColors )
         };
         this._addColorsToRange( this._minColor, colorStep, maxColors / 2 );

         colorStep = {
            r : 2 * this._calcColorStep( centerColor.r, this._maxColor.r, maxColors ),
            g : 2 * this._calcColorStep( centerColor.g, this._maxColor.g, maxColors ),
            b : 2 * this._calcColorStep( centerColor.b, this._maxColor.b, maxColors ),
            a : 2 * this._calcColorStep( centerColor.a, this._maxColor.a, maxColors )
         };
         this._addColorsToRange( centerColor, colorStep, (maxColors / 2) + 1 );

      } else {
         // single continue range
         colorStep = {
            r : this._calcColorStep( this._minColor.r, this._maxColor.r, maxColors ),
            g : this._calcColorStep( this._minColor.g, this._maxColor.g, maxColors ),
            b : this._calcColorStep( this._minColor.b, this._maxColor.b, maxColors ),
            a : this._calcColorStep( this._minColor.a, this._maxColor.a, maxColors )
         };
         this._addColorsToRange( this._minColor, colorStep, maxColors );
      }

      // calc data step
      this._maxDataSpace = Math.abs( dataRange.min ) + Math.abs( dataRange.max );
      this._dataStep = this._maxDataSpace / maxColors;

      this._log( 'dataStep: ' + this._dataStep );

   },

   _calcColorStep : function( minColor, maxColor, numberColors ) {
      if ( numberColors <= 0 )
         return;
      var numColors = numberColors == 1 ? 1 : numberColors - 1;
      return ((maxColor - minColor) / numColors);
   },

   // append colors to the end of the color Range, splitting the number of colors up evenly
   _addColorsToRange : function( startColor, colorStep, numberColors ) {
      var currentColor = this.niceRGBAColor( startColor );
      for (var i = 0; i < numberColors; i++) {
         this._colorRange[this._colorRange.length] = currentColor;
         currentColor = this.niceRGBAColor( {
            r : currentColor.r + colorStep.r,
            g : currentColor.g + colorStep.g,
            b : currentColor.b + colorStep.b,
            a : currentColor.a + colorStep.a
         } );

      }
   },

   _log : function( message ) {
      if ( this._debug ) {
         console.log( message );
      }
   },

   _RGBtoHex : function( R, G, B ) {
      return this._toHex( R ) + this._toHex( G ) + this._toHex( B );
   },

   _toHex : function( N ) {
      if ( N == null )
         return "00";
      N = parseInt( N );
      if ( N == 0 || isNaN( N ) )
         return "00";
      N = Math.max( 0, N );
      N = Math.min( N, 255 );
      N = Math.round( N );
      return "0123456789ABCDEF".charAt( (N - N % 16) / 16 ) + "0123456789ABCDEF".charAt( N % 16 );
   }

};
