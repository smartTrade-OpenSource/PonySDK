// UTF8 : Manage UTF-8 strings in ArrayBuffers
if(typeof module !== 'undefined' && typeof module.require === 'function') {
  require('string.fromcodepoint');
  require('string.prototype.codepointat');
}

var _UTF8={
  // non UTF8 encoding detection (cf README file for details)
  'isNotUTF8': function(bytes, byteOffset, byteLength) {
    try {
      UTF8.getStringFromBytes(bytes, byteOffset, byteLength, true);
    } catch(e) {
      return true;
    }
    return false;
  },
  // UTF8 decoding functions
  'getCharLength': function(theByte) {
    // 4 bytes encoded char (mask 11110000)
    if(0xF0 == (theByte&0xF0)) {
      return 4;
    // 3 bytes encoded char (mask 11100000)
    } else if(0xE0 == (theByte&0xE0)) {
      return 3;
    // 2 bytes encoded char (mask 11000000)
    } else if(0xC0 == (theByte&0xC0)) {
      return 2;
    // 1 bytes encoded char
    } else if(theByte == (theByte&0x7F)) {
      return 1;
    }
    return 0;
  },
  'getCharCode': function(bytes, byteOffset, charLength) {
    var charCode = 0, mask = '';
    byteOffset = byteOffset || 0;
    // Retrieve charLength if not given
    charLength = charLength || UTF8.getCharLength(bytes[byteOffset]);
    if(charLength == 0) {
      throw new Error(bytes[byteOffset].toString(2)+' is not a significative' +
        ' byte (offset:'+byteOffset+').');
    }
    // Return byte value if charlength is 1
    if(1 === charLength) {
      return bytes[byteOffset];
    }
    // Test UTF8 integrity
    mask = '00000000'.slice(0, charLength) + 1 + '00000000'.slice(charLength + 1);
    if(bytes[byteOffset]&(parseInt(mask, 2))) {
      throw Error('Index ' + byteOffset + ': A ' + charLength + ' bytes' +
        ' encoded char' +' cannot encode the '+(charLength+1)+'th rank bit to 1.');
    }
    // Reading the first byte
    mask='0000'.slice(0,charLength+1)+'11111111'.slice(charLength+1);
    charCode+=(bytes[byteOffset]&parseInt(mask,2))<<((--charLength)*6);
    // Reading the next bytes
    while(charLength) {
      if(0x80!==(bytes[byteOffset+1]&0x80)
        ||0x40===(bytes[byteOffset+1]&0x40)) {
        throw Error('Index '+(byteOffset+1)+': Next bytes of encoded char'
          +' must begin with a "10" bit sequence.');
      }
      charCode += ((bytes[++byteOffset]&0x3F) << ((--charLength) * 6));
    }
    return charCode;
  },
  'getStringFromBytes': function(bytes, byteOffset, byteLength, strict) {
    var charLength, chars = [];
    byteOffset = byteOffset|0;
    byteLength=('number' === typeof byteLength ?
      byteLength :
      bytes.byteLength || bytes.length
    );
    for(; byteOffset < byteLength; byteOffset++) {
      charLength = UTF8.getCharLength(bytes[byteOffset]);
      if(byteOffset + charLength > byteLength) {
        if(strict) {
          throw Error('Index ' + byteOffset + ': Found a ' + charLength +
            ' bytes encoded char declaration but only ' +
            (byteLength - byteOffset) +' bytes are available.');
        }
      } else {
        chars.push(String.fromCodePoint(
          UTF8.getCharCode(bytes, byteOffset, charLength, strict)
        ));
      }
      byteOffset += charLength - 1;
    }
    return chars.join('');
  },
  // UTF8 encoding functions
  'getBytesForCharCode': function(charCode) {
    if(charCode < 128) {
      return 1;
    } else if(charCode < 2048) {
      return 2;
    } else if(charCode < 65536) {
      return 3;
    } else if(charCode < 2097152) {
      return 4;
    }
    throw new Error('CharCode '+charCode+' cannot be encoded with UTF8.');
  },
  'setBytesFromCharCode': function(charCode, bytes, byteOffset, neededBytes) {
    charCode = charCode|0;
    bytes = bytes || [];
    byteOffset = byteOffset|0;
    neededBytes = neededBytes || UTF8.getBytesForCharCode(charCode);
    // Setting the charCode as it to bytes if the byte length is 1
    if(1 == neededBytes) {
      bytes[byteOffset] = charCode;
    } else {
      // Computing the first byte
      bytes[byteOffset++] =
        (parseInt('1111'.slice(0, neededBytes), 2) << 8 - neededBytes) +
        (charCode >>> ((--neededBytes) * 6));
      // Computing next bytes
      for(;neededBytes>0;) {
        bytes[byteOffset++] = ((charCode>>>((--neededBytes) * 6))&0x3F)|0x80;
      }
    }
    return bytes;
  },
  'setBytesFromString': function(string, bytes, byteOffset, byteLength, strict) {
    string = string || '';
    bytes = bytes || [];
    byteOffset = byteOffset|0;
    byteLength = ('number' === typeof byteLength ?
      byteLength :
      bytes.byteLength||Infinity
    );
    for(var i = 0, j = string.length; i < j; i++) {
      var neededBytes = UTF8.getBytesForCharCode(string[i].codePointAt(0));
      if(strict && byteOffset + neededBytes > byteLength) {
        throw new Error('Not enought bytes to encode the char "' + string[i] +
          '" at the offset "' + byteOffset + '".');
      }
      UTF8.setBytesFromCharCode(string[i].codePointAt(0),
        bytes, byteOffset, neededBytes, strict);
      byteOffset += neededBytes;
    }
    return bytes;
  }
};


if(typeof module !== 'undefined' && module.hasOwnProperty('exports')) {
  var UTF8 = _UTF8;
  module.exports = UTF8;
}else{
  UTF8 = _UTF8;
}
_UTF8 = undefined;
