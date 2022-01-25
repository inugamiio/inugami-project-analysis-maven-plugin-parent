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

/*
$( document ).on( "mousemove", function( event ) {
    const mouseMoveEvent = new Event('onMouseMove');
    document.dispatchEvent(mouseMoveEvent, {x:event.pageX, y:event.pageY});
});
*/