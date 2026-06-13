(function() {
  "use strict";

  AbstractAddon.defineAddon("com.ponysdk.sample.client.page.addon.LoggerAddOn", {

    init: function () {
      console.log("LabelPAddOn created");
    },

    log: function () {
      console.log("LabelPAddOn");
      this.sendDataToServer({
        "info": "Init done"
      });
      this.sendDataToServer({
        info: "Get Data",
        info2: "Get Data2"
      }, function(response) {
          console.log(JSON.parse(response));
      });
    },

    logWithText: function (value) {
      console.log("LabelPAddOn : " + value);
      this.sendDataToServer({
        "info": "Init done"
      });
      this.sendDataToServer({
        info: "Get Data",
        info2: value
      }, function(response) {
          console.log(JSON.parse(response));
      });
    },

  });

  AbstractAddon.defineAddon("com.ponysdk.sample.client.page.addon.PElementAddOn", {

    init: function () {
        console.log("PElementAddOn created")
    },

    text: function (value) {
        this.element.innerHTML = value
    }

  });

  AbstractAddon.defineAddon("com.ponysdk.sample.client.page.addon.SelectizeAddon", {

    init: function () {
    },

    addTag: function (value) {
        console.log(value.tag)
    },

    updateTag: function (value) {
        console.log(value.id)
        console.log(value.oldTag)
        console.log(value.newTag)

        if(value.found == 'no'){
            console.log('red item')
            this.jqelement[0].selectize.getItem(value.oldTag)[0].classList.add('unmatch')
        }else{
            this.jqelement[0].setAttribute('process','true')
            this.jqelement[0].setAttribute('type',value.type)

            console.log('remove and create')
            this.jqelement[0].selectize.removeItem(value.oldTag, true)
            this.jqelement[0].selectize.createItem(value.newTag + '{' + value.desc + '}' ,false)
        }
    },

    text: function (value) {
        var that = this;

        this.jqelement.selectize({
            plugins: ['remove_button'],
            delimiter: ',',
            persist: false,
            create: function (input) {
                return {
                    value: input,
                    text: input
                }
            },
            onItemAdd: function (value, $item) {
                console.log('on add item')
                var process = that.jqelement[0].getAttribute('process')
                var type = that.jqelement[0].getAttribute('type')
                
                if(process == 'true'){
                    that.jqelement[0].setAttribute('process','false')    
                    that.jqelement[0].setAttribute('type','-1')
                    
                    if(type == '1'){
                        $item[0].classList.add('qty-px')
                    }
                    
                    return;
                }
            
                var itemID = $item[0].getAttribute("id")

                    itemID = 'item' + Date.now();
                //    $item[0].setAttribute("id",itemID);
                //}

                pony.sendDataToServer(that.id, {
                    type: 'add',
                    id: itemID,
                    tag: value
                });
            },

            onItemRemove: function(value, $item) {
                console.log('on remove item')
                pony.sendDataToServer(that.id, {
                    type: 'remove',
                    tag: value
                });
            }
        });
    }

  });

  AbstractAddon.defineAddon("com.ponysdk.sample.client.page.addon.BinaryArgsAddOn", {

    init: function () {
      // Creation args arrive as a pure-binary typed JS array (no JSON)
      var c = this.options || [];
      window.__createLen = c.length;
      this._createHtml =
          '<div><b>Creation args (pure binary, no JSON):</b> ['
        + Array.prototype.map.call(c, function (x) { return JSON.stringify(x); }).join(', ') + ']</div>'
        + '<div><b>JS types received:</b> '
        + Array.prototype.map.call(c, function (x) { return typeof x; }).join(', ') + '</div>';
      this._render();
    },

    values: function () {
      var arr = Array.prototype.slice.call(arguments);
      var sum = 0;
      for (var i = 0; i < arr.length; i++) sum += arr[i];
      window.__bigLen = arr.length;
      window.__bigSum = sum;
      this._valuesHtml =
          '<div style="margin-top:12px"><b>Method call &mdash; typed binary array of '
        + arr.length + ' values</b> (the old protocol capped binary arrays at 255):</div>'
        + '<div>first=' + arr[0] + ', last=' + arr[arr.length - 1] + ', sum=' + sum
        + ', allNumbers=' + arr.every(function (x) { return typeof x === "number"; }) + '</div>';
      this._render();
    },

    _render: function () {
      if (this.element) {
        this.element.innerHTML =
            '<div style="font-family:ui-monospace,SFMono-Regular,Menlo,monospace;line-height:1.7;font-size:13px;color:#f0f4ff">'
          + (this._createHtml || '') + (this._valuesHtml || '') + '</div>';
      }
    }

  });
})();
