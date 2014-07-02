$( document ).ready(function() {
	//hide loading div
	$(".loading").fadeOut();
	
	//initialize tooltip
	$( document ).tooltip();
	
	//move tooltip for input box to the right side of element
	$( "#x" ).tooltip({ 
		position: { 
			my: "right+100% top-25%", at: "right center" 
		} 
	});
	
	//show loading div when submit button or href is clicked
	$("#go, a.word").click(function(){
		$(".loading").fadeIn();
	});
	
	//hide loading div before leaving page
	$(window).on('unload', function(){
		$(".loading").fadeOut();
	});
	
	//scroll to top for when tab is clicked
	$("#tabs ul li a").click(function(){
		$("html, body").animate({ scrollTop: 0 }, "slow");
	});
});

