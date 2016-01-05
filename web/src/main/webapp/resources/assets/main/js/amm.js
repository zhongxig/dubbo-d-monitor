
var Amm = function() {

    var changeiframeParentHeight = function (){
        var height = $('body').height();
        $(parent.document).find('iframe').height(height);
    };

    var nowDate = function show(){
        var mydate = new Date();
        var str = "" + mydate.getFullYear() + "-"+(mydate.getMonth()+1)+"-"+mydate.getDate();
        return str;
    };

    return {

        init: function () {
            changeiframeParentHeight();
        },

        changeiframeParentHeight: function () {
            changeiframeParentHeight();
        },

        getNowDate: function(){
            return nowDate();
        },

        animate:function(scroll_offset){
            $(parent.document).find('body').animate({
                scrollTop: scroll_offset.top  //让body的scrollTop等于pos的top，就实现了滚动
            }, 1200);
        }


    };

}();