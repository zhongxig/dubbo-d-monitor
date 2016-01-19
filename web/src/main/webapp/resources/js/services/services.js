var headerUrl = 'http://' + window.location.host;
//全局缓存
var storage = window.sessionStorage;//localStorage;

// echarts的ec对象
var echartsEc;
// 所有的app相关map，将返回的对象存储起来
var allAppResultMap = undefined;
// 测试环境url
var testUrlSet = undefined;

$(function () {
    initData();

    initClick();

    initeCharts();


});


function initData() {
    $.get(headerUrl + "/monitor/services/getAllService", function (resultVO) {
        if (resultVO.success) {
            var resutMap = resultVO.data;
            allAppResultMap = resutMap.allServicesMap;
            testUrlSet = resutMap.testUrlSet;
            // 展示风险项
            var wrongAppList = resutMap.wrongAppList;
            var wrongMethodsList = resutMap.wrongMethodsList;
            var wrongHostServiceList = resutMap.wrongHostServiceList;

            if (wrongAppList.length == 0 && wrongMethodsList.length == 0 && wrongHostServiceList.length == 0) {
                $("#warning_tab_div").addClass("hidden");
            } else {
                $("#warning_tab_div").removeClass("hidden");
            }

            if (wrongAppList.length > 0) {
                var wrongAppList_html = Mustache.render($('#string_list_template').html(), {'list': wrongAppList});
                $("#tab_more_app").html(wrongAppList_html);
            }
            if (wrongMethodsList.length > 0) {
                var wrongMethodsList_html = Mustache.render($('#string_list_template').html(), {'list': wrongMethodsList});
                $("#tab_more_method").html(wrongMethodsList_html);
            }
            if (wrongHostServiceList.length > 0) {
                var wrongHostServiceList_html = Mustache.render($('#string_list_template').html(), {'list': wrongHostServiceList});
                $("#wrong_host_content").html(wrongHostServiceList_html);
            }

            // 异常的service点击
            $(".warning_service").click(function () {
                $("#search_value").val($(this).html());
                $("#search_btn").trigger('click');
            })
        } else {
            var html = Mustache.render($('#alert_danger_template').html(), {'msg': "加载失败~！原因：" + resultVO.msg});
            $("body").prepend(html);
            $("#search_section").addClass("hidden");
        }
    });
}


function initClick() {
    // 搜索框自动去空格
    //$('#search_value').keyup(function(){
    //    var value = $(this).val().replace(" ","");
    //    $(this).val(value);
    //});
    //搜索框回车键
    $('#search_value').bind('keypress', function (event) {
        if (event.keyCode == "13") {
            $('#search_btn').trigger('click');
        }
    });
    //点击搜索
    $("#search_btn").click(function () {
        var search_value = $("#search_value").val().trim().replace(" ", "");
        $("#main_service_section").addClass("hidden");

        serviceTable(search_value);
    });

    //点击另外一个标签
    $(".wrong_tab").click(function(){
        var content_id = $(this).find('a').attr("href");

        $('.wrong_tab').removeClass("active");
        $(this).addClass("active");
        $('.wrong_content').removeClass("active");
        $(content_id).addClass("active");

        Amm.changeiframeParentHeight();
        return false;
    });

}


function initeCharts(functionName, param) {

    require.config({
        paths: {
            echarts: headerUrl + '/resources/echarts/dist'
        }
    });
    require(
        [
            'echarts',
            'echarts/chart/line',   // 按需加载所需图表，如需动态类型切换功能，别忘了同时加载相应图表
            'echarts/chart/bar',
            'echarts/chart/chord',
            'echarts/chart/force'
        ],
        function (ec) {
            echartsEc = ec;
            if (param != undefined) {
                functionName(param);
            } else if (functionName != undefined) {
                functionName();
            }
        }
    )


}

// =========================二级方法============

// 生成筛选的service列表 并定位到此处
function serviceTable(search_value) {
    var serviceBO_list = [];

    // 晒出符合的 ，若searchValue为空，则全部
    search_value = search_value.toUpperCase();
    $.each(allAppResultMap, function (key, value) {
        if ("" == search_value) {
            serviceBO_list.push(value);
        } else {
            var key_name = key.toUpperCase();
            if (key_name.indexOf(search_value) != -1) {
                serviceBO_list.push(value);
            }
        }
    });

    $("#appSumNumber").html(serviceBO_list.length);
    // 生成table
    var warningHtml = '<span class="badge badge-danger">WARNING</span>';
    var successHtml = '<span class="badge badge-success">SUCCESS</span>';
    var map = {
        list: serviceBO_list,
        serviceNameFunc: function () {
            var serviceName = this.serviceName;
            if ("" == search_value) {
                return serviceName
            }
            var index = serviceName.toUpperCase().indexOf(search_value);
            var next_index = index + search_value.length;
            var html = serviceName.substring(0, index);
            html += '<span class="hlight">' + serviceName.substring(index, next_index) + '</span>';
            html += serviceName.substring(next_index);
            return html
        },
        usedAppSum: function () {
            var usedApp = this.usedApp;
            if (usedApp == undefined) {
                return warningHtml.replace("WARNING", "0");
            }
            return successHtml.replace("SUCCESS", usedApp.length);
        },
        statusFunction: function () {
            var html = '';
            var ownerSet = this.ownerApp;
            var methodsSet = this.methods;
            var isHostWrong = this.isHostWrong;
            if (ownerSet != undefined && methodsSet != undefined && ownerSet.length == 1 && methodsSet.length == 1 && !isHostWrong) {
                return successHtml.replace("SUCCESS", "正常")
            }
            if(isHostWrong){
                html += warningHtml.replace("WARNING", "非法启动")
            }
            if (methodsSet == undefined) {
                html += warningHtml.replace("WARNING", "无方法")
            }
            else if (methodsSet.length > 1) {
                html += warningHtml.replace("WARNING", "多方法")
            }

            if (ownerSet == undefined) {
                html += warningHtml.replace("WARNING", "无提供者")
            }
            else if (ownerSet.length > 1) {
                html += warningHtml.replace("WARNING", "多应用提供")
            }

            return html
        },
        statusOK:function () {
            var ownerSet = this.ownerApp;
            var methodsSet = this.methods;
            if (ownerSet != undefined && methodsSet != undefined && ownerSet.length == 1 && methodsSet.length == 1) {
                return "true";
            }
            return "false";
        },
        usedAppLength:function(){
            var usedApp = this.usedApp;
            if (usedApp == undefined) {
                return "0";
            }
            return usedApp.length;
        }
    };

    var html = Mustache.render($('#search_result_template').html(), map);
    $("#main_service_tbody").html(html);
    $("#search_result_section").removeClass("hidden");

    Amm.changeiframeParentHeight();
    tableClick();
    // 滚动到指定位置
    var scroll_offset;
    if ("" == search_value) {
        var html = Mustache.render($('#alert_danger_template').html(), {'msg': "若无输入搜索词，将展示所有service！！"});
        $("#search_result_section").prepend(html);
        scroll_offset = $("#search_result_section").offset();
    } else {
        scroll_offset = $("#main_service_tbody").offset();
    }
    Amm.animate(scroll_offset);
}


// 生成table 的 后续
function tableClick() {
    $("#main_service_tbody > tr").unbind("click").click(function () {
        var value = $(this).data("value");
        var serviceBO = allAppResultMap[value];
        var methodsHost = serviceBO.methodsHost;
        var methodsSet = serviceBO.methods;
        if (methodsSet != undefined) {
            $("#serviceMethods").html("");

            var list = [];
            $.each(methodsHost, function (method, hostList) {
                var map = {
                    method: method,
                    hostList: hostList
                };
                list.push(map);
            });
            var map = {
                'list': list,
                methodFunction: function () {
                    var method = this.method;
                    var method_array = method.split(",");
                    // 每4个为一组
                    var html = '';
                    $.each(method_array, function (index, method_value) {
                        html += method_value + ",";
                        if (index % 3 == 0 && index != 0) {
                            html += "<br>";
                        }
                    });
                    var html_length = html.length;
                    if (html.charAt(html_length - 1) == ",") html = html.substring(0, html.length - 1);
                    return html;
                },
                hostFunction:function(){
                    var html = ""
                    var hostList = this.hostList;
                    $.each(hostList,function(i,hostBO){
                        var host = hostBO.host;
                        if($.inArray(host,testUrlSet) > -1){
                            html += '<span class="badge badge-success">测试环境：'+hostBO.hostString+ "</span>";
                        }else{
                            html += hostBO.hostString + " "
                        }
                    });
                    return html;
                }
            };
            var html = Mustache.render($('#method_template').html(), map);
            $("#method").html(html);
        } else {
            $("#method").html("");
            $("#serviceMethods").html("无 Methods");
        }
        $('#serviceName').html(serviceBO.serviceName);
        $("#main_service_section").removeClass("hidden");
        main_service_section(serviceBO);

        Amm.changeiframeParentHeight();

        var scroll_offset = $("#main_service_section").offset();
        // 滚动到指定位置
        Amm.animate(scroll_offset);
        return false;
    });

    //筛选 异常service
    $("#select_wrong_service_btn").unbind("click").click(function (){
        $(this).toggleClass("actived");

        changeTableShow();
        return false;
    });

    //筛选 0 用户
    $("#select_no_used_btn").unbind("click").click(function (){
        $(this).toggleClass("actived");
        changeTableShow();
        return false;
    });
}

//表格内部数据的展示和隐藏
function changeTableShow(){
    var wrong_service_status = $("#select_wrong_service_btn").hasClass("actived");
    var no_used_status = $("#select_no_used_btn").hasClass("actived");

    var tr_array = $("#main_service_tbody > tr");
    tr_array.removeClass("hidden");
    $.each(tr_array,function(i,tr_object){
        if(wrong_service_status){
            // 筛选出错误的
            var wrong_td = $(tr_object).find(".status_false").html();
            if(wrong_td == undefined){
                $(this).addClass("hidden");
            }
        }
        if(no_used_status){
            // 筛选出无用户的
            var no_used= $(tr_object).find(".used_0").html();
            if(no_used == undefined){
                $(this).addClass("hidden");
            }
        }
    });
    Amm.changeiframeParentHeight();

}

// section：main_service_section 的动态内容
function main_service_section(serviceBO) {
    var methodsSet = serviceBO.methods;

    var usedApp = serviceBO.usedApp;
    if (usedApp == undefined) {
        $("#echarts_div").html("无消费的应用！");
        return false;
    }

    var html = Mustache.render($('#echarts_section_template').html(), {});
    $("#echarts_div").html(html);
    Amm.changeiframeParentHeight();


    if (echartsEc == undefined) {
        initeCharts(servicesRelationForceChart, usedApp)
    } else {
        servicesRelationForceChart(usedApp);
    }

    if (methodsSet == undefined) {
        var html = Mustache.render($('#alert_danger_template').html(), {'msg': "不存在方法，解决后方可查看数据图表"});
        $("#echarts_div").prepend(html);
        $("#tab_service_data_btn").addClass("hidden");
        return false;
    }else if (methodsSet.length > 1) {
        var html = Mustache.render($('#alert_danger_template').html(), {'msg': "存在多个不同方法，解决后方可查看数据图表"});
        $("#echarts_div").prepend(html);
        $("#tab_service_data_btn").addClass("hidden");
        return false;
    } else {
        $("#tab_service_data_btn").removeClass("hidden");
    }

    $("#tab_service_data_btn").unbind("click").click(function () {

        $('#tab_service_relation_btn').parent().removeClass("active");
        $(this).parent().addClass("active");


        $('#tab_service_relation').removeClass("active");
        $('#tab_service_data').addClass("active");

        if ($("#bar_body").html().trim() == "") {

            var loadingEL = $('#tabbable-custom');
            Metronic.blockUI(loadingEL);
            serviceArtChart(serviceBO);
            Metronic.unblockUI(loadingEL);
        }
        Amm.changeiframeParentHeight();

        $("#search_method_value").unbind("keypress").bind('keypress', function (event) {
            if (event.keyCode == "13") {

                var method_value = $(this).val().trim().toUpperCase();
                var bar_div_array = $("#bar_body > div");
                $.each(bar_div_array,function(i,obj){
                    var value = $(obj).data("value").toUpperCase();
                    if(value.indexOf(method_value) != -1){
                        var scroll_offset = $(obj).offset();
                        // 滚动到指定位置
                        Amm.animate(scroll_offset);
                        //跳出循环
                        return false;
                    }
                });
            }
        });
        return false;
    });


    $(".relation_bar_options").unbind("click").click(function () {

        var loadingEL = $('#tabbable-custom');
        Metronic.blockUI(loadingEL);

        var type = $(this).find('input').data("value");
        serviceArtChart(serviceBO, type);

        Metronic.unblockUI(loadingEL);

    });


}


// ====echarts===
function servicesRelationForceChart(usedApp) {
    var ec = echartsEc;
    var charts_id = 'service_relation_force_echarts';
    var myChart = ec.init(document.getElementById(charts_id));
    myChart.showLoading({
        text: 'Loading...',
        effect: 'bubble',
        textStyle: {
            fontSize: 20
        }
    });

    var nodesList = [];
    nodesList.push({
        category: 0, name: "Service", value: 10, draggable: true
    });
    var linkList = [];
    $.each(usedApp, function (i, used_name) {
        var nodesMap = {
            category: 1, name: used_name, value: 10, draggable: true
        };
        nodesList.push(nodesMap);

        var linkMap = {
            source: "Service",
            target: used_name,
            weight: 1,
            name: "提供服务给" + used_name,
            itemStyle: {normal: {color: 'red'}}
        };
        linkList.push(linkMap);
    });
    var option = {
        tooltip: {
            trigger: 'item',
            formatter: ' {b}'
        },
        toolbox: {
            show: true,
            feature: {
                restore: {show: true},
                magicType: {show: true, type: ['force', 'chord']},
                saveAsImage: {show: true}
            }
        },
        legend: {
            data: ['applicationName']
        },
        series: [
            {
                type: 'force',
                name: "依赖关系",
                ribbonType: false,
                categories: [
                    {
                        name: '自身service'
                    },
                    {
                        name: 'applicationName'
                    }
                ],
                itemStyle: {
                    normal: {
                        label: {
                            show: true,
                            textStyle: {
                                color: '#333'
                            }
                        },
                        nodeStyle: {
                            brushType: 'both',
                            borderColor: 'yellow',
                            borderWidth: 1
                        }
                    },
                    emphasis: {
                        label: {
                            show: false
                        }
                    }
                },
                minRadius: 15,
                maxRadius: 55,
                gravity: 1.1,
                scaling: 1.2,
                draggable: false,
                linkSymbol: 'arrow',
                steps: 10,
                coolDown: 0.9,
                //preventOverlap: true,
                nodes: nodesList,
                links: linkList
            }
        ]
    };

    myChart.setOption(option);
    myChart.hideLoading();

}


// 生成全局art——tps图和每个方法的图
function serviceArtChart(serviceBO, type) {
    console.log('start');

    if (type == undefined) {
        type = $("#relation_bar_day").data("value");
    }


    var serviceName = serviceBO.serviceName;


    var methods = serviceBO.methods[0];
    var methodsSet = methods.split(",");

    // 初始化
    var ecChart_map = [];// 存每个方法的echart对象
    var methodChartMap = {
        'main': {
            'provider_tps_list': [],
            'provider_art_list': [],
            'consumer_tps_list': [],
            'consumer_art_list': []
        }
    }; // 存每个方法的每个列表数据
    var div_list = ['main'];    // 存每个方法名称

    $.each(methodsSet, function (i, method) {


        div_list.push(method);

        var map = {
            'provider_tps_list': [],
            'provider_art_list': [],
            'consumer_tps_list': [],
            'consumer_art_list': []
        };
        methodChartMap[method] = map;
    });
    if ($("#bar_body").html().trim() == "") {
        //生成 多个div存放bar图
        var html = Mustache.render($('#bar_template').html(), {'list': div_list});
        $("#bar_body").html(html);
        Amm.changeiframeParentHeight();
    }

    $.each(div_list, function (i, method_name) {
        var tps_div = method_name + "_tps_bar_echarts";
        var art_div = method_name + "_art_bar_echarts";

        var tpsChart = echartsEc.init(document.getElementById(tps_div));
        var artChart = echartsEc.init(document.getElementById(art_div));
        tpsChart.showLoading({
            text: 'Loading...',
            effect: 'bubble',
            textStyle: {
                fontSize: 20
            }
        });
        artChart.showLoading({
            text: 'Loading...',
            effect: 'bubble',
            textStyle: {
                fontSize: 20
            }
        });
        ecChart_map[method_name] = {
            'tpsChart':tpsChart,
            'artChart':artChart
        };
    });

    var nowDate = Amm.getNowDate();
    var main_tps_key = nowDate + type + serviceName + "main_tps_serviceArtChart";
    var main_tps_option = JSON.parse(storage.getItem(main_tps_key));
    // 存在 走缓存
    if (main_tps_option != undefined) {
        // 任务代码
        $.each(div_list, function (i, method_name) {
            var chart_map = ecChart_map[method_name];
            var tpsChart = chart_map['tpsChart'];
            var artChart = chart_map['artChart'];

            var tps_key = nowDate + type + serviceName + method_name + "_tps_serviceArtChart";
            var tps_option = JSON.parse(storage.getItem(tps_key));
            tpsChart.setOption(tps_option);
            tpsChart.hideLoading();

            var art_key = nowDate + type + serviceName + method_name + "_art_serviceArtChart";
            var art_option = JSON.parse(storage.getItem(art_key));
            artChart.setOption(art_option);
            artChart.hideLoading();

        });

        return false;
    }

    //横坐标
    var x_key = "x_minute_list";
    var x_list = JSON.parse(storage.getItem(x_key));
    if (x_list == undefined) {
        x_list = [];
        for (var i = 0; i < 24; i++) {
            for (var j = 0; j < 60; j = j + 10) {
                var a = "";
                if (i < 10) {
                    a += "0" + i;
                }
                else {
                    a += i;
                }
                a += ":";
                if (j < 10) {
                    a += "0" + j;
                }
                else {
                    a += j;
                }
                x_list.push(a);
            }
        }
        storage.setItem(x_key, JSON.stringify(x_list));
    }


    $.get(headerUrl + "/monitor/services/getChartByName", {
        'serviceName': serviceName,
        'type': type
    }, function (resultVO) {
        if (!resultVO.success) {
            var html = Mustache.render($('#alert_danger_template').html(), {'msg': "出错啦：" + resultVO.msg});
            $("#bar_body").prepend(html);
            return false;
        }

        //{provider:时间：方法：成功失败值} 四层
        var resultMap = resultVO.data;
        var providerMap = resultMap['provider'];
        var consumerMap = resultMap['consumer'];
        console.log(new Date().toLocaleString());

        $.each(x_list, function (i, minuteTime) {
            methodChartMap = provider_chart_data(providerMap, minuteTime, div_list, methodChartMap);
            methodChartMap = consumer_chart_data(consumerMap, minuteTime, div_list, methodChartMap);
        });

        // 生成数据图表
        $.each(methodChartMap, function (method_name, map_list) {
            var provider_tps_list = map_list['provider_tps_list'];
            var provider_art_list = map_list['provider_art_list'];
            var consumer_tps_list = map_list['consumer_tps_list'];
            var consumer_art_list = map_list['consumer_art_list'];

            var chart_map = ecChart_map[method_name];
            var tpsChart = chart_map['tpsChart'];
            var artChart = chart_map['artChart'];

            var tps_option = option_String(method_name + "-TPS(t/s)", x_list, provider_tps_list, consumer_tps_list);
            tpsChart.setOption(tps_option);
            tpsChart.hideLoading();

            var art_option = option_String(method_name + "-ART(ms/t)", x_list, provider_art_list, consumer_art_list);
            artChart.setOption(art_option);
            artChart.hideLoading();


            var tps_key = nowDate + type + serviceName + method_name + "_tps_serviceArtChart";
            var art_key = nowDate + type + serviceName + method_name + "_art_serviceArtChart";
            storage.setItem(tps_key, JSON.stringify(tps_option));
            storage.setItem(art_key, JSON.stringify(art_option));

        });



    });

}


function consumer_chart_data(consumerMap, minuteTime, div_list, methodChartMap) {
    // 60s
    var timeParticle = 60 * 10;
    //小数点后保留4位
    var pointNumber = 4;
    var consumerTimeMap = consumerMap[minuteTime];
    if (consumerTimeMap == undefined) {
        $.each(div_list, function (i, method_name) {
            var listMap = methodChartMap[method_name];
            var consumer_tps_list = listMap['consumer_tps_list'];
            var consumer_art_list = listMap['consumer_art_list'];
            consumer_tps_list.push("0");
            consumer_art_list.push("0");
            listMap['consumer_tps_list'] = consumer_tps_list;
            listMap['consumer_art_list'] = consumer_art_list;
            methodChartMap[method_name] = listMap;
        });
    } else {
        var all_successNum = 0;
        var all_elapsedNum = 0;

        var all_method = [];
        $.each(consumerTimeMap, function (method_name, method_map) {
            var successNum = method_map['success'];
            var elapsedNum = method_map['elapsed'];
            all_successNum += successNum;
            all_elapsedNum += elapsedNum;

            all_method.push(method_name);
            //方法级别的 tps和art统计
            var tps = Number(successNum / timeParticle).toFixed(pointNumber) + "";
            var art = Number(elapsedNum / successNum).toFixed(1) + "";
            var listMap = methodChartMap[method_name];
            var consumer_tps_list = listMap['consumer_tps_list'];
            var consumer_art_list = listMap['consumer_art_list'];

            consumer_tps_list.push(tps);
            consumer_art_list.push(art);
            listMap['consumer_tps_list'] = consumer_tps_list;
            listMap['consumer_art_list'] = consumer_art_list;
            methodChartMap[method_name] = listMap;
        });
        //此时间段不存在的方法，均插入0
        $.each(div_list, function (i, method_name) {
            if ($.inArray(method_name, div_list) == -1) {
                if (method_name != 'main') {
                    var listMap = methodChartMap[method_name];
                    var consumer_tps_list = listMap['consumer_tps_list'];
                    var consumer_art_list = listMap['consumer_art_list'];

                    consumer_tps_list.push("0");
                    consumer_art_list.push("0");
                    listMap['consumer_tps_list'] = consumer_tps_list;
                    listMap['consumer_art_list'] = consumer_art_list;
                    methodChartMap[method_name] = listMap;
                }
            }

        });

        //tps:此为一分钟的数据
        var all_tps = Number(all_successNum / timeParticle).toFixed(pointNumber) + "";
        var all_art = Number(all_elapsedNum / all_successNum).toFixed(1) + "";

        var listMap = methodChartMap['main'];
        var consumer_tps_list = listMap['consumer_tps_list'];
        var consumer_art_list = listMap['consumer_art_list'];

        consumer_tps_list.push(all_tps);
        consumer_art_list.push(all_art);
        listMap['consumer_tps_list'] = consumer_tps_list;
        listMap['consumer_art_list'] = consumer_art_list;
        methodChartMap['main'] = listMap;
    }

    return methodChartMap;
}

function provider_chart_data(providerMap, minuteTime, div_list, methodChartMap) {
    // 60s
    var timeParticle = 60 * 10;
    //小数点后保留4位
    var pointNumber = 4;
    var providerTimeMap = providerMap[minuteTime];
    if (providerTimeMap == undefined) {
        $.each(div_list, function (i, method_name) {
            var listMap = methodChartMap[method_name];
            var provider_tps_list = listMap['provider_tps_list'];
            var provider_art_list = listMap['provider_art_list'];
            provider_tps_list.push("0");
            provider_art_list.push("0");
            listMap['provider_tps_list'] = provider_tps_list;
            listMap['provider_art_list'] = provider_art_list;
            methodChartMap[method_name] = listMap;
        });
    } else {
        var all_successNum = 0;
        var all_elapsedNum = 0;

        var all_method = [];
        $.each(providerTimeMap, function (method_name, method_map) {
            var successNum = method_map['success'];
            var elapsedNum = method_map['elapsed'];
            all_successNum += successNum;
            all_elapsedNum += elapsedNum;

            all_method.push(method_name);
            //方法级别的 tps和art统计
            var tps = Number(successNum / timeParticle).toFixed(pointNumber) + "";
            var art = Number(elapsedNum / successNum).toFixed(1) + "";
            var listMap = methodChartMap[method_name];
            var provider_tps_list = listMap['provider_tps_list'];
            var provider_art_list = listMap['provider_art_list'];
            provider_tps_list.push(tps);
            provider_art_list.push(art);
            listMap['provider_tps_list'] = provider_tps_list;
            listMap['provider_art_list'] = provider_art_list;
            methodChartMap[method_name] = listMap;
        });
        //此时间段不存在的方法，均插入0
        $.each(div_list, function (i, method_name) {
            if ($.inArray(method_name, div_list) == -1) {
                if (method_name != 'main') {
                    var listMap = methodChartMap[method_name];
                    var provider_tps_list = listMap['provider_tps_list'];
                    var provider_art_list = listMap['provider_art_list'];

                    provider_tps_list.push("0");
                    provider_art_list.push("0");
                    listMap['provider_tps_list'] = provider_tps_list;
                    listMap['provider_art_list'] = provider_art_list;
                    methodChartMap[method_name] = listMap;
                }
            }

        });

        //tps:此为一分钟的数据
        var all_tps = Number(all_successNum / timeParticle).toFixed(pointNumber) + "";
        var all_art = Number(all_elapsedNum / all_successNum).toFixed(1) + "";

        var listMap = methodChartMap['main'];
        var provider_tps_list = listMap['provider_tps_list'];
        var provider_art_list = listMap['provider_art_list'];

        provider_tps_list.push(all_tps);
        provider_art_list.push(all_art);
        listMap['provider_tps_list'] = provider_tps_list;
        listMap['provider_art_list'] = provider_art_list;
        methodChartMap['main'] = listMap;
    }

    return methodChartMap;
}

function option_String(titleName, time_data_array_string, provider_data, consumer_data) {

    var option = {
        title: {
            text: titleName
        },
        tooltip: {
            trigger: 'axis'
        },
        legend: {
            data: ['provider', 'consumer']
        },
        toolbox: {
            show: true,
            feature: {
                mark: {show: true},
                dataView: {show: true, readOnly: false},
                magicType: {show: true, type: ['line', 'bar']},
                restore: {show: true},
                saveAsImage: {show: true}
            }
        },
        calculable: true,
        xAxis: [
            {
                type: 'category',
                boundaryGap: false,
                data: time_data_array_string
            }
        ],
        yAxis: [
            {
                type: 'value'
            }
        ],
        series: [
            {
                name: 'provider',
                type: 'line',
                data: provider_data
            },
            {
                name: 'consumer',
                type: 'line',
                data: consumer_data
            }
        ]
    };


    return option;
}