"use strict";
Addon.new("com.ponysdk.sample.client.page.addon.LoggerAddOn",
    {
        init: function () {
            console.log("LabelPAddOn created");
        },

        log: function () {
            console.log("LabelPAddOn");
        },

        logWithText: function (value) {
            console.log("LabelPAddOn : " + value);
        },

    });

Addon.new("com.ponysdk.sample.client.page.addon.PElementAddOn",
    {
        init: function () {
            console.log("PElementAddOn created")
        },

        text: function (value) {
            this.element.innerHTML = value
        },

    });

Addon.new("com.ponysdk.sample.client.page.addon.SelectizeAddon",
    {
        init: function () {
        },


        tag: function (value) {
            console.log(value.id);
            console.log(value.oldTag);
            console.log(value.newTag);

            //item.setAttribute('data-value',value.tag)

            this.jqelement[0].selectize.removeItem(value.oldTag)
            this.jqelement[0].selectize.createItem(value.newTag)
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
                    var itemID = $item[0].getAttribute("id");

                    if (itemID == null) {
                        itemID = 'item' + Date.now();
                        $item[0].setAttribute("id",itemID);
                    }

                    pony.sendDataToServer(that.id, {
                        id: itemID,
                        tag: value
                    });
                }
            });
        },

    });


Addon.new("com.ponysdk.sample.client.page.addon.HighChartsStackedColumnAddOn",
    {
        init: function () {
            console.log("HighChartsStackedColumnAddOn created")
        },

        series: function (value) {
            this.jqelement.highcharts({
                chart: {
                    type: 'column'
                },
                title: {
                    text: 'Stacked column chart'
                },
                xAxis: {
                    categories: ['Apples', 'Oranges', 'Pears', 'Grapes', 'Bananas']
                },
                yAxis: {
                    min: 0,
                    title: {
                        text: 'Total fruit consumption'
                    },
                    stackLabels: {
                        enabled: true,
                        style: {
                            fontWeight: 'bold',
                            color: (Highcharts.theme && Highcharts.theme.textColor) || 'gray'
                        }
                    }
                },
                legend: {
                    align: 'right',
                    x: -30,
                    verticalAlign: 'top',
                    y: 25,
                    floating: true,
                    backgroundColor: (Highcharts.theme && Highcharts.theme.background2) || 'white',
                    borderColor: '#CCC',
                    borderWidth: 1,
                    shadow: false
                },
                tooltip: {
                    headerFormat: '<b>{point.x}</b><br/>',
                    pointFormat: '{series.name}: {point.y}<br/>Total: {point.stackTotal}'
                },
                plotOptions: {
                    column: {
                        stacking: 'normal',
                        dataLabels: {
                            enabled: true,
                            color: (Highcharts.theme && Highcharts.theme.dataLabelsColor) || 'white',
                            style: {
                                textShadow: '0 0 3px black'
                            }
                        }
                    }
                },
                series: [{
                    name: 'John',
                    data: [5, 3, 4, 7, 2]
                }, {
                    name: 'Jane',
                    data: [2, 2, 3, 2, 1]
                }, {
                    name: 'Joe',
                    data: [3, 4, 4, 2, 5]
                }]
            });
        },

    });



