function sprintf() {
	// http://kevin.vanzonneveld.net
	// + original by: Ash Searle (http://hexmen.com/blog/)
	// + namespaced by: Michael White (http://crestidg.com)
	// * example 1: sprintf("%01.2f", 123.1);
	// * returns 1: 123.10

	var regex = /%%|%(\d+\$)?([-+#0 ]*)(\*\d+\$|\*|\d+)?(\.(\*\d+\$|\*|\d+))?([scboxXuidfegEG])/g;
	var a = arguments, i = 0, format = a[i++];

	// pad()
	var pad = function(str, len, chr, leftJustify) {
		var padding = (str.length >= len)
				? ''
				: Array(1 + len - str.length >>> 0).join(chr);
		return leftJustify ? str + padding : padding + str;
	};

	// justify()
	var justify = function(value, prefix, leftJustify, minWidth, zeroPad) {
		var diff = minWidth - value.length;
		if (diff > 0) {
			if (leftJustify || !zeroPad) {
				value = pad(value, minWidth, ' ', leftJustify);
			} else {
				value = value.slice(0, prefix.length)
						+ pad('', diff, '0', true) + value.slice(prefix.length);
			}
		}
		return value;
	};

	// formatBaseX()
	var formatBaseX = function(value, base, prefix, leftJustify, minWidth,
			precision, zeroPad) {
		// Note: casts negative numbers to positive ones
		var number = value >>> 0;
		prefix = prefix && number && {
			'2' : '0b',
			'8' : '0',
			'16' : '0x'
		}[base] || '';
		value = prefix + pad(number.toString(base), precision || 0, '0', false);
		return justify(value, prefix, leftJustify, minWidth, zeroPad);
	};

	// formatString()
	var formatString = function(value, leftJustify, minWidth, precision,
			zeroPad) {
		if (precision != null) {
			value = value.slice(0, precision);
		}
		return justify(value, '', leftJustify, minWidth, zeroPad);
	};

	// finalFormat()
	var doFormat = function(substring, valueIndex, flags, minWidth, _,
			precision, type) {
		if (substring == '%%')
			return '%';

		// parse flags
		var leftJustify = false, positivePrefix = '', zeroPad = false, prefixBaseX = false;
		for (var j = 0; flags && j < flags.length; j++)
			switch (flags.charAt(j)) {
				case ' ' :
					positivePrefix = ' ';
					break;
				case '+' :
					positivePrefix = '+';
					break;
				case '-' :
					leftJustify = true;
					break;
				case '0' :
					zeroPad = true;
					break;
				case '#' :
					prefixBaseX = true;
					break;
			}

		// parameters may be null, undefined, empty-string or real valued
		// we want to ignore null, undefined and empty-string values
		if (!minWidth) {
			minWidth = 0;
		} else if (minWidth == '*') {
			minWidth = +a[i++];
		} else if (minWidth.charAt(0) == '*') {
			minWidth = +a[minWidth.slice(1, -1)];
		} else {
			minWidth = +minWidth;
		}

		// Note: undocumented perl feature:
		if (minWidth < 0) {
			minWidth = -minWidth;
			leftJustify = true;
		}

		if (!isFinite(minWidth)) {
			throw new Error('sprintf: (minimum-)width must be finite');
		}

		if (!precision) {
			precision = 'fFeE'.indexOf(type) > -1 ? 6 : (type == 'd')
					? 0
					: void(0);
		} else if (precision == '*') {
			precision = +a[i++];
		} else if (precision.charAt(0) == '*') {
			precision = +a[precision.slice(1, -1)];
		} else {
			precision = +precision;
		}

		// grab value using valueIndex if required?
		var value = valueIndex ? a[valueIndex.slice(0, -1)] : a[i++];

		switch (type) {
			case 's' :
				return formatString(String(value), leftJustify, minWidth,
						precision, zeroPad);
			case 'c' :
				return formatString(String.fromCharCode(+value), leftJustify,
						minWidth, precision, zeroPad);
			case 'b' :
				return formatBaseX(value, 2, prefixBaseX, leftJustify,
						minWidth, precision, zeroPad);
			case 'o' :
				return formatBaseX(value, 8, prefixBaseX, leftJustify,
						minWidth, precision, zeroPad);
			case 'x' :
				return formatBaseX(value, 16, prefixBaseX, leftJustify,
						minWidth, precision, zeroPad);
			case 'X' :
				return formatBaseX(value, 16, prefixBaseX, leftJustify,
						minWidth, precision, zeroPad).toUpperCase();
			case 'u' :
				return formatBaseX(value, 10, prefixBaseX, leftJustify,
						minWidth, precision, zeroPad);
			case 'i' :
			case 'd' : {
				var number = parseInt(+value);
				var prefix = number < 0 ? '-' : positivePrefix;
				value = prefix
						+ pad(String(Math.abs(number)), precision, '0', false);
				return justify(value, prefix, leftJustify, minWidth, zeroPad);
			}
			case 'e' :
			case 'E' :
			case 'f' :
			case 'F' :
			case 'g' :
			case 'G' : {
				var number = +value;
				var prefix = number < 0 ? '-' : positivePrefix;
				var method = ['toExponential', 'toFixed', 'toPrecision']['efg'
						.indexOf(type.toLowerCase())];
				var textTransform = ['toString', 'toUpperCase']['eEfFgG'
						.indexOf(type)
						% 2];
				value = prefix + Math.abs(number)[method](precision);
				return justify(value, prefix, leftJustify, minWidth, zeroPad)[textTransform]();
			}
			default :
				return substring;
		}
	};

	return format.replace(regex, doFormat);
}

window.sprintf = sprintf;