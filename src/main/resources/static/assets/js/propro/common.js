
var changeBtn=$(".change").eq(0);
//change language begin
var change=function () {
    var langCh=$(".Chinese").eq(0);
    var langEn=$(".English").eq(0);
    var url=window.location.href.split("?")[0];
    console.log("已经执行change");
    langCh.attr("href",url+"?"+"lang=ch");
    langEn.attr("href",url+"?"+"lang=en");
};
changeBtn.hover(change);

//change language end


//high level choose begin
var highPower=$("#m_accordion_1_item_1_head");
var icon=$("#icon");
var newAdd=function () {
    var changeIcon=function () {
            if(icon.attr("class").indexOf("fa-chevron-down")<0){
                icon.removeClass("fa-chevron-up");
                icon.addClass("fa-chevron-down");
            }else{
                icon.removeClass("fa-chevron-down");
                icon.addClass("fa-chevron-up");
            }
    };
    return {
        init:function () {
            changeIcon();

        }
    }
}();
highPower.click(function() {
    newAdd.init();
});
////high level choose end

//another button  begin
var form=$(".m-form").eq(0);
var first_button=$("#first-button");
first_button.on("click",function () {
    form.submit();
});