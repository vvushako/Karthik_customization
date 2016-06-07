<%@ page trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="template" tagdir="/WEB-INF/tags/desktop/template" %>
<%@ taglib prefix="theme" tagdir="/WEB-INF/tags/shared/theme" %>
<%@ taglib prefix="nav" tagdir="/WEB-INF/tags/desktop/nav" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="cms" uri="http://hybris.com/tld/cmstags" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="ycommerce" uri="http://hybris.com/tld/ycommercetags" %>
<%@ taglib prefix="common" tagdir="/WEB-INF/tags/desktop/common" %>
<%@ taglib prefix="breadcrumb" tagdir="/WEB-INF/tags/desktop/nav/breadcrumb" %>
<%@ taglib prefix="format" tagdir="/WEB-INF/tags/shared/format" %>
<%@ taglib prefix="giftCardForm" uri="http://www.springframework.org/tags/form" %>
 
<template:solsticepage pageTitle="${pageTitle}">
<link type="text/css" rel="stylesheet" href="${contextPath}/_ui/desktop/solstice/css/customer_service.css" />
 <div id="globalMessages">
  <common:globalMessages/>
 </div>
 
<div class="sixteen columns alpha omega row customer_service">
 <%--  <nav:customerServiceLeftNavigation /> --%>
   <div class="footer_link_pages">
  
			<div class="twelve columns omega row">
			
			<h1><cms:pageSlot position="giftCardNameHeader" var="giftCardNameHeader">
			<cms:component component="${giftCardNameHeader}" />
			</cms:pageSlot></h1>
			
			<h2>Gift card</h2>
				<div class="giftcertpurchase">
				<div class="giftcertcolumn">
					<div class="contentasset">
						<h2>Purchase a eGift Certificate</h2>
						<p>Any purchase of eGift for
								more than $200 will be placed on hold until voice verification with
								the credit card holder.</p>
						<p>eGift Certificates can be ordered in any amount starting at $5.00. They are sent via e-mail to any individual with a valid e-mail address. The recipient of a eGift Certificate will receive an e-mail with a eGift Certificate Code Redeemable Only at <a href="http://www.solsticesunglasses.com">solsticesunglasses.com</a>. The eGift Certificate code is activated after credit card approval via e-mail.
						</p>
					</div>
			
					<div class="giftcertpurchaseform">
						<form action="/cart/add/gcproduct" id="GiftCertificateForm" method="post">
							<div class="entry_fields">
								<label class="labeltext">Your Name <span class="requiredindicator">*</span> </label>
								<input type="text" class="textinput" name="yourName" ><!-- value="Karthik"> -->
							</div>
							<div class="entry_fields">
							
								<label class="labeltext">Friend's Name <span class="requiredindicator">*</span> </label>
								<input type="text" class="textinput" name="gcProductFriendsName" ><!--  value="Mayank"> --> 
							</div>
							<div class="entry_fields">
							
								<label class="labeltext">Friend's Last Name <span class="requiredindicator">*</span> </label>
								<input type="text" class="textinput" name="gcProductFriendsLastName" ><!-- value="Rathi"> -->
							</div>
							<div class="entry_fields">
								<label class="labeltext">Friend's Email  <span class="requiredindicator">*</span> </label>
								<input type="text" class="textinput" name="gcProductFriendsEmail" id="gcEmail" ><!-- value="mayank@gmail.com"> -->
							</div>
							<div class="entry_fields">
								<label class="labeltext">Confirm Friend's Email <span class="requiredindicator">*</span> </label>
								<input type="text" class="textinput" name="gcProductFriendsEmailConf" ><!-- value="mayank@gmail.com"> -->
							</div>
							<div class="entry_fields">
								<label class="labeltext">Message (Optional)</label>
								<textarea name="gcProductMsg"></textarea>
							</div>
							<div class="entry_fields">
								<label class="labeltext">Gift Amount <span class="requiredindicator">*</span> </label>
								<div class="select-style giftAmout">
									<select class="" name="giftAmout">
										<option title="" value="">Select Amount</option>
										<option title="0000000001" value="25">$25.00</option>
										<option title="0000000007" value="50">$50.00</option>
										<option title="0000000008" value="75">$75.00</option>
										<option title="0000000009" value="100">$100.00</option>
										<option title="0000000011" value="125">$125.00</option>
										<option title="0000000010" value="200">$200.00</option>
										<option title="0000000012" value="250">$250.00</option>
										
										<!-- <option title="" value="100">$100.00</option> -->
										<!-- <option title="827886944538" value="400">$400.00</option>
										<option title="GCVP01" value="500">$500.00</option> -->
									</select>
								</div>
							</div>
							<input type="hidden" id="productCodePost" name="productCodePost" value="0000000001">
							<div class="entry_fields">
								<button id="AddToBasketButton" class="button-gold-large" type="submit"><span>Add to Bag</span></button>
								<%-- dskjfaksdfjaskl
								<c:forEach items="${cartData.entries}" var="entry" varStatus="index">
									<c:if test="${entry.product.type eq 'SORXLensVariant'}">
										<c:set var="isRXProduct" value="true" scope="request" />
											<c:if test="${entry.product.isBifocalProduct}">
												<c:set var="isBifocalProduct" value="true" scope="request" />
											</c:if>
									</c:if>
								</c:forEach> --%>
							</div>
						</form>
					</div>
				</div>
			
				<div class="giftcertcolumn">
					<h2>Already Have a eGift Certificate?</h2>
					
					<p>Here's how to redeem a eGift Certificate online. On the billing page, provide the card's XX-digit number code. You can also check the balance of your Gift Certificate online. Please enter your Gift Certificate number here:</p>
					<div class="checkbalanceForm">
						<form action="/solsticeGiftCard/checkBalance" id="giftCardCheckBalance" method="get">
							<div class="entry_fields">
								<input type="text" class="textinput" name="giftCardNumber">
							</div>
							<div class="giftCardAvailableBalance">
								<c:if test="${not empty giftCardPaymentInfoData}">
									<h3>Remaining balance on your gift card is $<fmt:formatNumber value=" ${giftCardPaymentInfoData.availableAmount}" minFractionDigits="2" /> </h3>
								</c:if>
							</div>
							<div class="entry_fields">
								<button id="CheckBalanceButton" class="button-gold-large" type="submit">CHECK BALANCE</button>
							</div>
						</form>
					</div>
					
					<p><strong>Important Information About eGift Certificates</strong></p>
					<div class="gift_certificates_info">
						<ul>
							<li><span>You cannot purchase an eGift Certificate with another eGift Certificate.</span></li>
							<li><span>You cannot return an eGift Certificate.</span></li>
							<li><span>If the amount of your eGift Certificate(s) does NOT cover the total order amount, you will need to pay the remainder of the purchase with a valid credit card.</span></li>
							<li><span>If the amount of your eGift Certificate(s) is for MORE than the total order amount, the balance will be stored with your solsticesunglasses.com account for your next purchase using the same eGift Certificate code.</span></li>
							<li><span>If you have any additional questions, please contact our Customer Service team.</span></li>
							<li><span>eGift Certificates are NOT Redeemable in Stores, non-transferable, non-refundable and are not redeemable for cash (except where required by state law).</span></li>
							<li><span>We cannot replace lost or stolen eGift Certificates.</span></li>
							<li><span>We do not charge sales tax when you buy an eGift Certificate because it is charged when the eGift. Certificate is used. State tax may be added to all orders shipped to addresses in the states indicated here.</span></li>
						</ul>
					</div>
				</div>
			
			</div>
		</div>

   </div>
  </div>
  <script type="text/javascript">
      

    //this is a dumb place for this but I don't have time to fix it...
    
    $(document).ready(function() {
  
        $('.giftAmout select').on("change",function() {
             $('.giftcertpurchaseform #productCodePost').val($(this).find("option:selected").attr("title"));
        });
       /*  $('#GiftCertificateForm').on("submit",function() {
        	
       }); */
        
    });
      function addToGiftcartFunction() {  
        $.ajax({
              type: "POST",
              url:$("#GiftCertificateForm").attr('action'),
              contentType: "application/x-www-form-urlencoded",
              data: $("#GiftCertificateForm").serialize(),
              beforeSend: function(){
                //add product to cart or show loading
                $("body").prepend($("<div class='plpAjaxLoader'></div><img class='plpAjaxLoaderImg' src='/_ui/desktop/solstice/images/ajax.gif'>"));
                $("html, body").animate({ scrollTop: 0 }, 600);
                $('#account_nav_cart').fadeIn();
                $('#account_nav_cart .minicart').css("visible","hidden");
                $('#account_nav_cart .minicart_loading').fadeIn();
				
                cartCount = parseInt($(".cart_count").text());

                if(isNaN(cartCount)){
                  cartCount = 0;
                }
                $(".cart_count").text(cartCount + 1);

                //$('#cart_content').trigger('click');
              },
              success: function(data){
            	$(".global_message_container.error").remove();
                if(data.status == "error"){
                	$("body").prepend(data.errorText);
                	hideCartPopup();
                	$(".cart_count").text(cartCount);
                }else{
                  $("#globalMessages").html("");
                  $('#account_nav_cart').html(data);
                  cartTimeout = setTimeout('hideCartPopup()', 2000);
                  //ACC.product.displayAddToCartPopup();
                }
                $(".plpAjaxLoader, .plpAjaxLoaderImg").remove();
              },
              error: function(data){
                console.log(data);
              }
          }); 
      } 
      </script>
      <script type="text/javascript"> 
		$(document).ready(function(){
			$("#GiftCertificateForm").validate({
				  errorClass:"error",
			        rules: {
			         "yourName":{required: true,rangelength: [1,255]},
			         "gcProductFriendsName":{required: true,rangelength: [1, 255]},
			         "gcProductFriendsLastName":{required: true,rangelength: [1, 255]},
			         "gcProductFriendsEmail":{required:true,email:true}, 
			         "gcProductFriendsEmailConf":{required:true,equalTo:'#gcEmail'},
			         "giftAmout":{required: true},
				  },
				  messages: {
				   "yourName":{required:'<spring:theme code="text.addgift.yourname" text="field your name is mandatory"/>'},
				   "gcProductFriendsName":{required:'<spring:theme code="text.addgift.friendsname" text="field friend&#39s name is mandatory" />'},
				   "gcProductFriendsLastName":{required:'<spring:theme code="text.addgift.friendslastname" text="field friend&#39s last name is mandatory" />'},
				   "gcProductFriendsEmail":{required:'<spring:theme code="text.addgift.friendsemail" text="field friend&#39s email is mandatory" />'},
				   "gcProductFriendsEmailConf":{required:'<spring:theme code="text.addgift.confirmfriendsemail" text="field confirm friend&#39s email is mandatory" />'},
				   "giftAmout":{required:'<spring:theme code="text.addgift.giftamount" text="field gift amount is mandatory" />'},
				  },
			        errorPlacement: function(error, element) {
			            if (element.is("select")) {
			                error.insertAfter(element.parent());
			            } else {
			                error.insertAfter(element);
			            }
			        },
			        submitHandler: function(form) {
			        	addToGiftcartFunction();
			          }

				 })
				 /* jQuery.validator.addMethod("alpha", function(value, element) {
				    return this.optional(element) || value == value.match(/^[a-zA-Z]+$/);
				},"Only Characters Allowed."); */
 
				 
			// giftCardCheckBalance
			$("#giftCardCheckBalance").validate({
				  errorClass:"error",
			        rules: {
			         "giftCardNumber":{required: true}
				  },
				  messages: {
				   "giftCardNumber":{required:'<spring:theme code="text.giftcard.checkbalance" text="Pleae enter your Gift Certificate number here"/>'}
				  },
		          submitHandler: function(form) {
		        	giftCardCheckBalance();
		          }

				 })
				 function giftCardCheckBalance(){
				$.ajax({
		              type: "GET",
		              url:$("#giftCardCheckBalance").attr('action'),
		              data: $("#giftCardCheckBalance").serialize(),
		              beforeSend: function(){
		                  $("body").prepend($("<div class='plpAjaxLoader'></div><img class='plpAjaxLoaderImg' src='/_ui/desktop/solstice/images/ajax.gif'>"));
		                  $("html, body").animate({ scrollTop: 0 }, 600);
		              },
		              success: function(data){
		                var checkAvailableBalance = $(data).find(".giftCardAvailableBalance").html();
		                if(checkAvailableBalance==undefined){
		                	$(".AvailableBalanceError").remove();
		                	$(".giftCardAvailableBalance").empty();
		                	$("body").prepend('<div class="global_message_container error AvailableBalanceError" id="error_0"><div class="container"><spring:theme code="text.giftcard.invalid.number" text="Not a valid Gift Certificate number. Please enter valid one."/></div></div>');
		                	//alert("111");
		                }else{
		                	$(".giftCardAvailableBalance").html(checkAvailableBalance);
		                	$(".AvailableBalanceError").remove();
		                	//alert("2222");
		                }
		                $(".plpAjaxLoader, .plpAjaxLoaderImg").remove();
		                
		              },
		              error: function(data){
		                console.log(data);
		              }
		          });
				}
	
		});
	</script>
</template:solsticepage>