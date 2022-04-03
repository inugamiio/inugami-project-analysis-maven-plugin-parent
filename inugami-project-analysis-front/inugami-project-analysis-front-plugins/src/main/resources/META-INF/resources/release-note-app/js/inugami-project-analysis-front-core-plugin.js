document.addEventListener("onInit", function(e) {
    $( "#navbarHeaderButton" ).click(function() {
        var button = $("#navbarHeaderButton");
        var target = $(button.attr("data-target"));
        target.toggleClass( "show" );
    });
    $( "body" ).toggleClass( "loading" );
});

document.addEventListener('highlight', function(event){
    hljs.highlightAll();
});
