/*
 * [y] hybris Platform
 *
 * Copyright (c) 2000-2013 hybris AG
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of hybris
 * ("Confidential Information"). You shall not disclose such Confidential
 * Information and shall use it only in accordance with the terms of the
 * license agreement you entered into with hybris.
 * 
 *  
 */
package com.so.storefront.controllers.misc;

import de.hybris.platform.commercefacades.order.data.CartModificationData;
import de.hybris.platform.cms2.servicelayer.services.CMSComponentService;
import de.hybris.platform.cms2.exceptions.CMSItemNotFoundException;
import de.hybris.platform.commercefacades.product.ProductOption;
import de.hybris.platform.acceleratorcms.model.components.MiniCartComponentModel;
import de.hybris.platform.commerceservices.order.CommerceCartModificationException;
import de.hybris.platform.core.model.order.AbstractOrderEntryModel;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.order.CartService;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.session.SessionService;
import de.hybris.platform.commercefacades.order.data.CartData;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Collections;

import javax.annotation.Resource;
import javax.validation.Valid;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.so.core.model.AbstractPromotionConfigurationModel;
import com.so.core.model.CategoryQuantityLimitConfModel;
import com.so.facades.cart.SOCartFacade;
import com.so.facades.product.SOProductFacade;
import com.so.facades.promotion.SOPromoValidationFacade;
import com.so.storefront.controllers.AbstractController;
import com.so.storefront.controllers.ControllerConstants;
import com.so.storefront.controllers.util.GlobalMessages;
import com.so.storefront.forms.AddGiftCardToCartForm;
import com.so.storefront.forms.AddToCartForm;


/**
 * Controller for Add to Cart functionality which is not specific to a certain page.
 */
@Controller
@Scope("tenant")
public class AddToCartController extends AbstractController
{
	private static final String TYPE_MISMATCH_ERROR_CODE = "typeMismatch";
	private static final String ERROR_MSG_TYPE = "errorMsg";
	private static final String QUANTITY_INVALID_BINDING_MESSAGE_KEY = "basket.error.quantity.invalid.binding";
	//horrible way to identify the cartneeds to be fixed...
	private static final String COMPONENT_UID = "MiniCart";

	protected static final Logger LOG = Logger.getLogger(AddToCartController.class);

	@Resource(name = "cartFacade")
	private SOCartFacade cartFacade;

	@Resource(name = "productFacade")
	private SOProductFacade productFacade;

	@Resource
	private SessionService sessionService;

	@Resource(name = "promoValidationFacade")
	private SOPromoValidationFacade promoValidationFacade;

	@Resource
	private CartService cartService;

	@Resource(name = "modelService")
	private ModelService modelService;

	@Resource(name = "cmsComponentService")
	private CMSComponentService cmsComponentService;

	@SuppressWarnings("boxing")
	@RequestMapping(value = "/cart/add", method = RequestMethod.POST, produces = "application/json")
	public String addToCart(@RequestParam("productCodePost") final String code, final Model model, @Valid final AddToCartForm form,
			final BindingResult bindingErrors, final RedirectAttributes redirectAttributes) throws CMSItemNotFoundException
	{


		if (cartFacade.isRXProductInCart())
		{
			GlobalMessages.addMessage(model, GlobalMessages.ERROR_MESSAGES_HOLDER, "cart.page.add.to.cart.error", null);
			return ControllerConstants.Views.Fragments.Cart.AddToCartError;
		}

		if (cartFacade.isGiftCardProductInCart() || cartFacade.isNormProductInCart(code))
		{
			/*
			 * GlobalMessages.addMessage(model, GlobalMessages.ERROR_MESSAGES_HOLDER,
			 * "cart.page.add.giftcardproduct.to.cart.error", null);
			 */
			GlobalMessages.addMessage(model, GlobalMessages.ERROR_MESSAGES_HOLDER, "cart.page.add.giftcardproduct.to.cart.error",
					null);
			return ControllerConstants.Views.Fragments.Cart.AddToCartError;
		}
		//check promotion validation
		//check if the special account promotion is available
		final Long quantity = null;
		Long entryNumber = null;
		HashMap promoMap = new HashMap<>();
		HashMap validationMap = new HashMap<>();
		final CartModel sessionCart = cartService.getSessionCart();
		final List<AbstractOrderEntryModel> entries = sessionCart.getEntries();
		for (final AbstractOrderEntryModel entry : entries)
		{
			//final ProductModel productModel = entry.getProduct();
			//final Product product = modelService.getSource(productModel);

			final String entryCode = entry.getProduct().getCode();
			if (entryCode != null && code != null && entryCode.equalsIgnoreCase(code))
			{
				entryNumber = entry.getEntryNumber().longValue();
			}

		}
		final AbstractPromotionConfigurationModel promoConfig = (AbstractPromotionConfigurationModel) sessionService
				.getAttribute("SPECIALACCOUNTCONFIGURATION");
		if (promoConfig != null)
		{
			final List<CategoryQuantityLimitConfModel> productLimitList = promoConfig.getProductLimitList();
			promoMap = promoValidationFacade.validatePromoConfiguration(productLimitList, entryNumber, quantity, code);
		}
		//check if this item is already added to the cart and if yes, then need to send the entry Number to isValidEntry method


		//check voucher validation
		final boolean validEntry = promoValidationFacade.isValidEntry(code, null, entryNumber);
		final boolean promoApplied = promoValidationFacade.promoApplied();
		if (promoApplied)
		{
			validationMap = promoValidationFacade.validateVoucherConfiguration(entryNumber, quantity, code);
		}

		final long qty = form.getQty();

		if (qty <= 0)
		{
			model.addAttribute(ERROR_MSG_TYPE, "basket.error.quantity.invalid");
			model.addAttribute("quantity", Long.valueOf(0L));
		}

		//implies that promotion validation failed
		else if (promoMap != null && !promoMap.isEmpty())
		{
			
			if (entryNumber != null)
			{
				GlobalMessages.addMessage(model, GlobalMessages.ERROR_MESSAGES_HOLDER, "item.promo.quantity.not.allowed", new Object[]
				{ promoMap.get("productCode"), promoMap.get("quantity"), promoMap.get("category") });
				return ControllerConstants.Views.Fragments.Cart.AddToCartError;
			}
			else
			{
				GlobalMessages.addMessage(model, GlobalMessages.ERROR_MESSAGES_HOLDER, "item.promo.not.allowed", new Object[]
				{ promoMap.get("category") });
				return ControllerConstants.Views.Fragments.Cart.AddToCartError;
			}

		}
		else if (validationMap != null && !validationMap.isEmpty())
		{

			if (entryNumber != null)
			{
				GlobalMessages.addMessage(model, GlobalMessages.ERROR_MESSAGES_HOLDER, "item.voucher.quantity.not.allowed",
						new Object[]
				{ validationMap.get("productCode"), validationMap.get("quantity"), validationMap.get("category") });
				return ControllerConstants.Views.Fragments.Cart.AddToCartError;
			}
			else
			{
				GlobalMessages.addMessage(model, GlobalMessages.ERROR_MESSAGES_HOLDER, "item.voucher.not.allowed", new Object[]
				{ validationMap.get("productCode"), validationMap.get("quantity"), validationMap.get("category") });
				return ControllerConstants.Views.Fragments.Cart.AddToCartError;
			}

		}
		else
		{
			if (!cartFacade.isAllowToAddToCart(code))
			{
				GlobalMessages.addMessage(model, GlobalMessages.ERROR_MESSAGES_HOLDER, "cart.page.add.to.cart.error", null);
				return ControllerConstants.Views.Fragments.Cart.AddToCartError;
			}
			try
			{
				final CartModificationData cartModification = cartFacade.addToCart(code, qty);

				model.addAttribute("quantity", Long.valueOf(cartModification.getQuantityAdded()));
				model.addAttribute("entry", cartModification.getEntry());

				if (cartModification.getQuantityAdded() == 0L)
				{
					GlobalMessages.addMessage(model, GlobalMessages.ERROR_MESSAGES_HOLDER,
							"basket.information.quantity.noItemsAdded.noStock", null);
					return ControllerConstants.Views.Fragments.Cart.AddToCartError;

				}
				else if (cartModification.getQuantityAdded() < qty)
				{
					GlobalMessages.addMessage(model, GlobalMessages.ERROR_MESSAGES_HOLDER,
							"basket.information.quantity.noItemsAdded.noStock", null);
					return ControllerConstants.Views.Fragments.Cart.AddToCartError;
				}
			}
			catch (final CommerceCartModificationException ex)
			{
				GlobalMessages.addMessage(model, GlobalMessages.ERROR_MESSAGES_HOLDER, "basket.error.occurred", null);
				return ControllerConstants.Views.Fragments.Cart.AddToCartError;
			}
		}

		model.addAttribute("product", productFacade.getProductBasedOnTheCatalogVersion(code, Arrays.asList(ProductOption.BASIC)));

		return getCartView(model);
	}

	@SuppressWarnings("boxing")
	@RequestMapping(value = "/cart/add/gcproduct", method = RequestMethod.POST, produces = "application/json")
	public String addGiftCardProductToCart(@RequestParam("productCodePost") final String code, final Model model,
			@Valid final AddGiftCardToCartForm giftCardForm, final BindingResult bindingErrors,
			final RedirectAttributes redirectAttributes) throws CMSItemNotFoundException
	{

		if (cartFacade.isRXProductInCart())
		{
			GlobalMessages.addMessage(model, GlobalMessages.ERROR_MESSAGES_HOLDER, "cart.page.add.to.cart.error", null);
			return ControllerConstants.Views.Fragments.Cart.AddToCartError;
		}

		if (cartFacade.isGiftCardProductInCart() || cartFacade.isNormProductInCart(code))
		{
			/*
			 * GlobalMessages.addMessage(model, GlobalMessages.ERROR_MESSAGES_HOLDER,
			 * "cart.page.add.giftcardproduct.to.cart.error", null);
			 */

			GlobalMessages.addMessage(model, GlobalMessages.ERROR_MESSAGES_HOLDER, "cart.page.add.giftcardproduct.to.cart.error",
					null);
			return ControllerConstants.Views.Fragments.Cart.AddToCartError;
		}
		//check promotion validation
		//check if the special account promotion is available
		final Long quantity = null;
		Long entryNumber = null;
		HashMap promoMap = new HashMap<>();
		HashMap validationMap = new HashMap<>();
		final CartModel sessionCart = cartService.getSessionCart();
		final List<AbstractOrderEntryModel> entries = sessionCart.getEntries();
		for (final AbstractOrderEntryModel entry : entries)
		{
			//final ProductModel productModel = entry.getProduct();
			//final Product product = modelService.getSource(productModel);

			final String entryCode = entry.getProduct().getCode();
			if (entryCode != null && code != null && entryCode.equalsIgnoreCase(code))
			{
				entryNumber = entry.getEntryNumber().longValue();
			}

		}
		final AbstractPromotionConfigurationModel promoConfig = (AbstractPromotionConfigurationModel) sessionService
				.getAttribute("SPECIALACCOUNTCONFIGURATION");
		if (promoConfig != null)
		{
			final List<CategoryQuantityLimitConfModel> productLimitList = promoConfig.getProductLimitList();
			promoMap = promoValidationFacade.validatePromoConfiguration(productLimitList, entryNumber, quantity, code);
		}
		//check if this item is already added to the cart and if yes, then need to send the entry Number to isValidEntry method

		//check voucher validation
		final boolean validEntry = promoValidationFacade.isValidEntry(code, null, entryNumber);
		final boolean promoApplied = promoValidationFacade.promoApplied();
		if (promoApplied)
		{
			validationMap = promoValidationFacade.validateVoucherConfiguration(entryNumber, quantity, code);
		}

		final long qty = giftCardForm.getQty();

		if (qty <= 0)
		{
			model.addAttribute(ERROR_MSG_TYPE, "basket.error.quantity.invalid");
			model.addAttribute("quantity", Long.valueOf(0L));
		}

		//implies that promotion validation failed
		else if (promoMap != null && !promoMap.isEmpty())
		{

			if (entryNumber != null)
			{
				GlobalMessages.addMessage(model, GlobalMessages.ERROR_MESSAGES_HOLDER, "item.promo.quantity.not.allowed", new Object[]
				{ promoMap.get("productCode"), promoMap.get("quantity"), promoMap.get("category") });
				return ControllerConstants.Views.Fragments.Cart.AddToCartError;
			}
			else
			{
				GlobalMessages.addMessage(model, GlobalMessages.ERROR_MESSAGES_HOLDER, "item.promo.not.allowed", new Object[]
				{ promoMap.get("category") });
				return ControllerConstants.Views.Fragments.Cart.AddToCartError;
			}

		}
		else if (validationMap != null && !validationMap.isEmpty())
		{

			if (entryNumber != null)
			{
				GlobalMessages.addMessage(model, GlobalMessages.ERROR_MESSAGES_HOLDER, "item.voucher.quantity.not.allowed",
						new Object[]
				{ validationMap.get("productCode"), validationMap.get("quantity"), validationMap.get("category") });
				return ControllerConstants.Views.Fragments.Cart.AddToCartError;
			}
			else
			{
				GlobalMessages.addMessage(model, GlobalMessages.ERROR_MESSAGES_HOLDER, "item.voucher.not.allowed", new Object[]
				{ validationMap.get("productCode"), validationMap.get("quantity"), validationMap.get("category") });
				return ControllerConstants.Views.Fragments.Cart.AddToCartError;
			}

		}
		else
		{
			if (!cartFacade.isAllowToAddToCart(code))
			{
				GlobalMessages.addMessage(model, GlobalMessages.ERROR_MESSAGES_HOLDER, "cart.page.add.to.cart.error", null);
				return ControllerConstants.Views.Fragments.Cart.AddToCartError;
			}
			try
			{
				final CartModificationData cartModification = cartFacade.addToCart(code, qty);

				final CartModel sessionCartModel = cartService.getSessionCart();
				if (sessionCartModel != null && cartFacade.isGiftCardProductInCart())
				{
					if (giftCardForm.getGcProductFriendsEmail() != null && giftCardForm.getGcProductFriendsName() != null
							&& giftCardForm.getGcProductFriendsLastName() != null && !giftCardForm.getGcProductFriendsEmail().isEmpty()
							&& !giftCardForm.getGcProductFriendsName().isEmpty()
							&& !giftCardForm.getGcProductFriendsLastName().isEmpty())
					{
						sessionCartModel.setGcProductFriendsEmail(giftCardForm.getGcProductFriendsEmail());
						sessionCartModel.setGcProductFriendsName(giftCardForm.getGcProductFriendsName());
						sessionCartModel.setGcProductFriendsLastName(giftCardForm.getGcProductFriendsLastName());
						if (giftCardForm.getGcProductMsg() != null && !giftCardForm.getGcProductMsg().isEmpty())
						{
							sessionCartModel.setGiftProductMessage(giftCardForm.getGcProductMsg());
						}
						modelService.save(sessionCartModel);
					}
				}

				model.addAttribute("quantity", Long.valueOf(cartModification.getQuantityAdded()));
				model.addAttribute("entry", cartModification.getEntry());

				if (cartModification.getQuantityAdded() == 0L)
				{
					
					GlobalMessages.addMessage(model, GlobalMessages.ERROR_MESSAGES_HOLDER, "basket.information.quantity.noItemsAdded.noStock", null);
					return ControllerConstants.Views.Fragments.Cart.AddToCartError;

				}
				else if (cartModification.getQuantityAdded() < qty)
				{
					GlobalMessages.addMessage(model, GlobalMessages.ERROR_MESSAGES_HOLDER, "basket.information.quantity.noItemsAdded.noStock", null);
					return ControllerConstants.Views.Fragments.Cart.AddToCartError;
				}
			}
			catch (final CommerceCartModificationException ex)
			{
				GlobalMessages.addMessage(model, GlobalMessages.ERROR_MESSAGES_HOLDER, "basket.error.occurred", null);
				return ControllerConstants.Views.Fragments.Cart.AddToCartError;
			}
		}


		model.addAttribute("product", productFacade.getProductBasedOnTheCatalogVersion(code, Arrays.asList(ProductOption.BASIC)));

		return getCartView(model);
	}

	@RequestMapping(value = "/cart/add/rx", method = RequestMethod.POST, produces = "application/json")
	public String addRXToCart(@RequestParam("product") final String code, @RequestParam("lens") final String lens,
			@RequestParam("coat") final String coat, final Model model, @Valid final AddToCartForm form,
			final BindingResult bindingErrors, final RedirectAttributes redirectAttributes) throws CMSItemNotFoundException
	{
		// if (bindingErrors.hasErrors())
		// {
		// 	return getViewWithBindingErrorMessages(model, bindingErrors);
		// }

		final long qty = form.getQty();

		if (qty <= 0)
		{
			model.addAttribute(ERROR_MSG_TYPE, "basket.error.quantity.invalid");
			model.addAttribute("quantity", Long.valueOf(0L));
		}
		else
		{
			if (StringUtils.isBlank(lens) && StringUtils.isNotBlank(code))
			{
				if (!cartFacade.isAllowToAddToCart(code))
				{
					GlobalMessages.addMessage(redirectAttributes, GlobalMessages.ERROR_MESSAGES_HOLDER,
							"cart.page.add.to.cart.error", null);
					return REDIRECT_PREFIX + "/cart";
				}
			}
			else if (!cartFacade.isAllowToAddToCart(lens))
			{
				GlobalMessages.addMessage(model, GlobalMessages.ERROR_MESSAGES_HOLDER,
						"cart.page.add.to.cart.error", null);
				return REDIRECT_PREFIX + "/cart";
			}
			try
			{
				final CartModificationData cartModification = cartFacade.addToCart(code, qty);
				model.addAttribute("quantity", Long.valueOf(cartModification.getQuantityAdded()));
				model.addAttribute("entry", cartModification.getEntry());
				if (StringUtils.isNotBlank(lens))
				{
					final CartModificationData lensCartModification = cartFacade.addToCart(lens, qty);
					if (cartModification.getQuantityAdded() == 0L || lensCartModification.getQuantityAdded() == 0L)
					{
						GlobalMessages.addMessage(model, GlobalMessages.ERROR_MESSAGES_HOLDER, "basket.information.quantity.noItemsAdded.noStock", null);
						return ControllerConstants.Views.Fragments.Cart.AddToCartError;
					}
					else if (cartModification.getQuantityAdded() < qty)
					{
						GlobalMessages.addMessage(model, GlobalMessages.ERROR_MESSAGES_HOLDER, "basket.information.quantity.noItemsAdded.noStock", null);
						return ControllerConstants.Views.Fragments.Cart.AddToCartError;
					}
				}
				if (StringUtils.isNotBlank(coat))
				{
					final CartModificationData coatCartModification = cartFacade.addToCart(coat, qty);
					if (cartModification.getQuantityAdded() == 0L || coatCartModification.getQuantityAdded() == 0L)
					{
						GlobalMessages.addMessage(model, GlobalMessages.ERROR_MESSAGES_HOLDER, "basket.information.quantity.noItemsAdded.noStock", null);
						return ControllerConstants.Views.Fragments.Cart.AddToCartError;
					}
					else if (cartModification.getQuantityAdded() < qty)
					{
						GlobalMessages.addMessage(model, GlobalMessages.ERROR_MESSAGES_HOLDER, "basket.information.quantity.noItemsAdded.noStock", null);
						return ControllerConstants.Views.Fragments.Cart.AddToCartError;
					}
				}
			}
			catch (final CommerceCartModificationException ex)
			{
				model.addAttribute(ERROR_MSG_TYPE, "basket.error.occurred");
				model.addAttribute("quantity", Long.valueOf(0L));
			}
		}
		model.addAttribute("product", productFacade.getProductBasedOnTheCatalogVersion(code, Arrays.asList(ProductOption.BASIC)));

		return getCartView(model);

	}



	protected String getCartView(final Model model) throws CMSItemNotFoundException
	{

		final CartData cartData = cartFacade.getSessionCart();
		model.addAttribute("cartData", cartData);
		
		//horrible way to identify the cartneeds to be fixed...
		final MiniCartComponentModel component = (MiniCartComponentModel) cmsComponentService.getSimpleCMSComponent(COMPONENT_UID);

		final List entries = cartData.getEntries();
		if (entries != null)
		{
			Collections.reverse(entries);
			model.addAttribute("entries", entries);

			model.addAttribute("numberItemsInCart", Integer.valueOf(entries.size()));
			if (entries.size() < component.getShownProductCount())
			{
				model.addAttribute("numberShowing", Integer.valueOf(entries.size()));
			}
			else
			{
				model.addAttribute("numberShowing", Integer.valueOf(component.getShownProductCount()));
			}
		}
		model.addAttribute("lightboxBannerComponent", component.getLightboxBannerComponent());

		return ControllerConstants.Views.Fragments.Cart.CartPopup;

	}

	// protected String getViewWithBindingErrorMessages(final Model model, final BindingResult bindingErrors)
	// {
	// 	for (final ObjectError error : bindingErrors.getAllErrors())
	// 	{
	// 		if (isTypeMismatchError(error))
	// 		{
	// 			model.addAttribute(ERROR_MSG_TYPE, QUANTITY_INVALID_BINDING_MESSAGE_KEY);
	// 		}
	// 		else
	// 		{
	// 			model.addAttribute(ERROR_MSG_TYPE, error.getDefaultMessage());
	// 		}
	// 	}
	// 	return ControllerConstants.Views.Fragments.Cart.AddToCartPopup;
	// }

	protected boolean isTypeMismatchError(final ObjectError error)
	{
		return error.getCode().equals(TYPE_MISMATCH_ERROR_CODE);
	}
}
