var dataId;
var overviewIds = "";
var peptideRef;
var useNoise1000;
var isGaussFilter;

function query(dataId, cutInfo) {
    var datas = null;
    $.ajax({
        type: "POST",
        url: "/analyse/view",
        data: {
            dataId: dataId,
            cutInfo: cutInfo
        },
        dataType: "json",
        async: false,
        success: function (result) {
            if (result.success) {
                datas = result.model;
            } else {
                chart.clear();
            }
        }
    });

    if (datas == null) {
        return;
    }
    var data_rt = datas.rt;
    var peptideRef = datas.peptideRef;
    var data_intensity = datas.intensity;
    var intensity = 0;
    for (var i = 0; i < data_intensity.length; i++) {
        intensity += data_intensity[i];
    }
    option = {
        title: {
            text: peptideRef + "-总强度为:" + intensity,
            left: 10
        },
        legend: {
            data: ['rt/intensity'],
            align: 'left'
        },
        toolbox: {
            // y: 'bottom',
            feature: {
                dataView: {},
                saveAsImage: {
                    pixelRatio: 2
                }

            }
        },
        dataZoom: [{
            type: 'inside',
            filterMode: 'empty'
        }, {
            type: 'slider'
        }],
        tooltip: {
            trigger: 'axis',
            axisPointer: {            // 坐标轴指示器，坐标轴触发有效
                type: 'shadow'        // 默认为直线，可选为：'line' | 'shadow'
            }
        },
        xAxis: {
            data: data_rt,
            silent: false,
            splitLine: {
                show: false
            }
        },
        yAxis: {},
        series: [{
            name: 'intensity',
            type: 'line',
            data: data_intensity
        }]
    };

    chart.setOption(option, true);
}
function queryGroup(dataId, isGaussFilter, useNoise1000) {
    if (dataId == null) {
        dataId = this.dataId;
    } else {
        this.dataId = dataId;
    }

    if (useNoise1000 == null) {
        useNoise1000 = this.useNoise1000;
    } else {
        this.useNoise1000 = useNoise1000;
    }

    if (isGaussFilter == null) {
        isGaussFilter = this.isGaussFilter;
    } else {
        this.isGaussFilter = isGaussFilter;
    }

    var datas = null;
    $.ajax({
        type: "POST",
        url: "/analyse/viewGroup",
        data: {
            dataId: dataId,
            isGaussFilter: isGaussFilter,
            useNoise1000: useNoise1000
        },

        dataType: "json",
        async: false,
        success: function (result) {
            if (result.success) {
                datas = result.model;
            } else {
                chartGroup.clear();
            }
        }

    });
    if (datas == null) {
        return;
    }
    var data_rt = datas.rt;
    var peptideRef = datas.peptideRef;
    var cutinfo = datas.cutInfoArray;
    var intensity_arrays = datas.intensityArrays;
    var bestRt = datas.bestRt;
    var leftRtList = datas.leftRtList;
    var rightRtList = datas.rightRtList;
    var boundaryData = [];
    for (var i =0; i<leftRtList.length; i++){
        if(bestRt < rightRtList[i] && bestRt > leftRtList[i]){
            boundaryData.push([
                {xAxis: leftRtList[i]+"",itemStyle:{color:'rgba(102,204,255,0.5)'}},
                {xAxis: rightRtList[i]+""}
            ]);
        }else {
            boundaryData.push([
                {xAxis: leftRtList[i]+""},
                {xAxis: rightRtList[i]+""}
            ]);
        }
    }


    var intensity_series = [];
    intensity_series.push({
        name: cutinfo[0],
        type: 'line',
        smooth: true,
        data: intensity_arrays[0],
        markArea: {
            silent: true,
            data: boundaryData
        }
    });
    for (var i = 1; i < intensity_arrays.length; i++) {
        intensity_series.push({
            name: cutinfo[i],
            type: 'line',
            smooth: true,
            data: intensity_arrays[i]
        });
    }
    var label = document.getElementById("peptideLabel");
    label.innerText = peptideRef;
    var textLabel = peptideRef + ":" + intensity_arrays.length + "个MS2碎片;";
    if (bestRt != null) {
        textLabel = textLabel + "最佳峰RT:" + bestRt;
    }
    option = {
        title: {
            text: textLabel,
            left: 10
        },
        legend: {
            data: ['rt/intensity'],
            align: 'left'
        },
        toolbox: {
            // y: 'bottom',
            feature: {
                dataView: {},
                saveAsImage: {
                    pixelRatio: 2
                }

            }
        },
        dataZoom: [{
            type: 'inside',
            filterMode: 'empty',
            start: (chartGroup.getModel() && chartGroup.getModel().getOption().dataZoom[0].start) ? chartGroup.getModel().getOption().dataZoom[0].start : 0,
            end: (chartGroup.getModel() && chartGroup.getModel().getOption().dataZoom[0].end) ? chartGroup.getModel().getOption().dataZoom[0].end : 100
        }, {
            type: 'slider'
        }],
        tooltip: {
            trigger: 'axis',
            axisPointer: {            // 坐标轴指示器，坐标轴触发有效
                type: 'shadow'        // 默认为直线，可选为：'line' | 'shadow'
            }
        },
        xAxis: {
            data: data_rt,
            silent: false,
            splitLine: {
                show: false
            }

        },
        yAxis: {},
        series: intensity_series
    };

    chartGroup.setOption(option, true);

}
function queryMultiGroup(peptideRef, isGaussFilter, useNoise1000) {

    if(peptideRef == null){
        peptideRef = this.peptideRef;
    }else{
        this.peptideRef = peptideRef;
    }

    if (useNoise1000 == null) {
        useNoise1000 = this.useNoise1000;
    } else {
        this.useNoise1000 = useNoise1000;
    }

    if (isGaussFilter == null) {
        isGaussFilter = this.isGaussFilter;
    } else {
        this.isGaussFilter = isGaussFilter;
    }
    var groups = null;
    $.ajax({
        type: "POST",
        url: "/analyse/viewMultiGroup",
        data: {
            peptideRef: peptideRef,
            overviewIds: overviewIds,
            isGaussFilter: isGaussFilter,
            useNoise1000: useNoise1000
        },

        dataType: "json",
        async: false,
        success: function (result) {
            // for(i in chartMap){
            //     chartMap[i].clear();
            // }
            if (result.success) {
                groups = result.model;
            }
        }

    });
    if (groups == null) {
        return;
    }
    var label = document.getElementById("peptideLabel");
    label.innerText = peptideRef;
    var count = 0;
    for(i in chartMap){
        var group = groups[count];
        if(group == null){
            continue;
        }
        count++;
        var element = chartMap[i];
        var data_rt = group.rt;
        var cutinfo = group.cutInfoArray;
        var intensity_arrays = group.intensityArrays;
        var bestRt = group.bestRt;
        var leftRtList = group.leftRtList;
        var rightRtList = group.rightRtList;
        var boundaryData = [];
        for (var i =0; i<leftRtList.length; i++){
            if(bestRt < rightRtList[i] && bestRt > leftRtList[i]){
                boundaryData.push([
                    {xAxis: leftRtList[i]+"",itemStyle:{color:'rgba(102,204,255,0.5)'}},
                    {xAxis: rightRtList[i]+""}
                ]);
            }else {
                boundaryData.push([
                    {xAxis: leftRtList[i]+""},
                    {xAxis: rightRtList[i]+""}
                ]);
            }
        }

        var intensity_series = [];
        for (var i = 0; i < intensity_arrays.length; i++) {
            intensity_series.push({
                name: cutinfo[i],
                type: 'line',
                smooth: true,
                data: intensity_arrays[i],
                markArea: {
                    silent: true,
                    data: boundaryData
                }
            });
        }

        var textLabel = element.getDom().getAttribute("data");
        if (bestRt != null) {
            textLabel = textLabel + "最佳RT:" + bestRt;
        }
        option = {
            title: {
                text: textLabel,
                left: 10
            },
            legend: {
                data: ['rt/intensity'],
                align: 'left'
            },
            dataZoom: [{
                type: 'inside',
                filterMode: 'empty',
                start: (element.getModel() && element.getModel().getOption().dataZoom[0].start) ? element.getModel().getOption().dataZoom[0].start : 0,
                end: (element.getModel() && element.getModel().getOption().dataZoom[0].end) ? element.getModel().getOption().dataZoom[0].end : 100
            }, {
                type: 'slider'
            }],
            tooltip: {
                trigger: 'axis',
                axisPointer: {            // 坐标轴指示器，坐标轴触发有效
                    type: 'shadow'        // 默认为直线，可选为：'line' | 'shadow'
                }
            },
            xAxis: {
                data: data_rt,
                silent: false,
                splitLine: {
                    show: false
                }
            },
            yAxis: {},
            series: intensity_series
        };

        element.setOption(option, true);
    }
}
function allFragmentConv(dataId, isGaussFilter, useNoise1000){

    if (dataId == null) {
        dataId = this.dataId;
    } else {
        this.dataId = dataId;
    }

    if (useNoise1000 == null) {
        useNoise1000 = this.useNoise1000;
    } else {
        this.useNoise1000 = useNoise1000;
    }

    if (isGaussFilter == null) {
        isGaussFilter = this.isGaussFilter;
    } else {
        this.isGaussFilter = isGaussFilter;
    }
    var datas = null;

    $.ajax({
        type: "POST",
        url: "/analyse/allFragmentConv",
        data: {
            dataId: dataId,
            isGaussFilter: isGaussFilter,
            useNoise1000: useNoise1000
        },
        dataType: "json",
        async: false,
        success: function (result) {
            if (result.success) {
                datas = result.model;
            } else {
                chartGroup.clear();
            }
        }
    });

    if (datas == null) {
        return;
    }
    var data_rt = datas.rt;
    var peptideRef = datas.peptideRef;
    var cutinfo = datas.cutInfoArray;
    var intensity_arrays = datas.intensityArrays;
    var bestRt = datas.bestRt;

    var intensity_series = [];
    for (var i = 0; i < intensity_arrays.length; i++) {
        intensity_series.push({
            name: cutinfo[i],
            type: 'line',
            smooth: true,
            data: intensity_arrays[i],
        });
    }

    var textLabel = peptideRef + ":" + intensity_arrays.length + "个MS2碎片;";
    if (bestRt != null) {
        textLabel = textLabel + "最佳峰RT:" + bestRt;
    }
    option = {
        title: {
            text: textLabel,
            left: 10
        },
        legend: {
            data: ['rt/intensity'],
            align: 'left'
        },
        toolbox: {
            // y: 'bottom',
            feature: {
                dataView: {},
                saveAsImage: {
                    pixelRatio: 2
                }

            }
        },
        dataZoom: [{
            type: 'inside',
            filterMode: 'empty',
            start: (chartGroup.getModel() && chartGroup.getModel().getOption().dataZoom[0].start) ? chartGroup.getModel().getOption().dataZoom[0].start : 0,
            end: (chartGroup.getModel() && chartGroup.getModel().getOption().dataZoom[0].end) ? chartGroup.getModel().getOption().dataZoom[0].end : 100
        }, {
            type: 'slider'
        }],
        tooltip: {
            trigger: 'axis',
            axisPointer: {
                type: 'shadow'
            }
        },
        xAxis: {
            data: data_rt,
            silent: false,
            splitLine: {
                show: false
            }
        },
        yAxis: {},
        series: intensity_series
    };

    chartGroup.setOption(option, true);
}