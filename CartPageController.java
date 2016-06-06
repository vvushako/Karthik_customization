package com.so.storefront.controllers.pages;

import de.hybris.platform.acceleratorservices.config.SiteConfigService;
import de.hybris.platform.acceleratorservices.controllers.page.PageType;
import de.hybris.platform.acceleratorservices.enums.CheckoutFlowEnum;
import de.hybris.platform.acceleratorservices.enums.CheckoutPciOptionEnum;
import de.hybris.platform.cms2.exceptions.CMSItemNotFoundException;
import de.hybris.platform.commercefacades.order.data.CartData;
import de.hybris.platform.commercefacades.order.data.CartModificationData;
import de.hybris.platform.commercefacades.order.data.CartRestorationData;
import de.hybris.platform.commercefacades.order.data.OrderEntryData;
import de.hybris.platform.commercefacades.order.data.PrescriptionFormData;
import de.hybris.platform.commercefacades.product.ProductOption;
import de.hybris.platform.commercefacades.product.data.PriceData;
import de.hybris.platform.commercefacades.product.data.ProductData;
import de.hybris.platform.commercefacades.voucher.VoucherFacade;
import de.hybris.platform.commercefacades.voucher.exceptions.VoucherOperationException;
import de.hybris.platform.commerceservices.order.CommerceCartModificationException;
import de.hybris.platform.commerceservices.order.CommerceCartService;
import de.hybris.platform.core.model.order.AbstractOrderEntryModel;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.core.model.order.payment.GiftCardPaymentInfoModel;
import de.hybris.platform.core.model.user.CustomerModel;
import de.hybris.platform.order.CartService;
import de.hybris.platform.order.exceptions.CalculationException;
import de.hybris.platform.product.ProductService;
import de.hybris.platform.promotions.model.PromotionResultModel;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.session.SessionService;
import de.hybris.platform.servicelayer.user.UserService;
import de.hybris.platform.store.services.BaseStoreService;
import de.hybris.platform.voucher.VoucherModelService;
import de.hybris.platform.voucher.VoucherService;
import de.hybris.platform.voucher.jalo.Voucher;
import de.hybris.platform.voucher.jalo.VoucherManager;
import de.hybris.platform.voucher.model.VoucherModel;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.so.core.cartentry.SOCartService;
import com.so.core.model.AbstractPromotionConfigurationModel;
import com.so.core.model.CategoryQuantityLimitConfModel;
import com.so.core.model.PrescriptionMediaModel;
import com.so.core.promotion.SOPromotionService;
import com.so.core.services.order.impl.DefaultSOCommerceCartCalculationStrategy;
import com.so.core.voucher.service.SOVoucherService;
import com.so.facades.cart.SOCartFacade;
import com.so.facades.flow.impl.SessionOverrideCheckoutFlowFacade;
import com.so.facades.product.SOProductFacade;
import com.so.facades.promotion.SOPromoValidationFacade;
import com.so.storefront.annotations.RequireHardLogIn;
import com.so.storefront.breadcrumb.ResourceBreadcrumbBuilder;
import com.so.storefront.constants.WebConstants;
import com.so.storefront.controllers.ControllerConstants;
import com.so.storefront.controllers.util.GlobalMessages;
import com.so.storefront.forms.GuestForm;
import com.so.storefront.forms.PrescriptionMajorsForm;
import com.so.storefront.forms.RegisterForm;
import com.so.storefront.forms.UpdateQuantityForm;


/**
 * Controller for cart page
 */
@Controller
@Scope("tenant")
@RequestMapping(value = "/cart")
public class CartPageController extends AbstractPageController
{
	private static final Logger LOG = Logger.getLogger(CartPageController.class);

	public static final String SHOW_CHECKOUT_STRATEGY_OPTIONS = "storefront.show.checkout.flows";

	private static final String CART_CMS_PAGE_LABEL = "cart";
	private static final String CONTINUE_URL = "continueUrl";

	@Resource(name = "productService")
	private ProductService productService;

	@Resource(name = "productFacade")
	private SOProductFacade productFacade;

	@Resource(name = "cartFacade")
	private SOCartFacade cartFacade;

	//@Resource(name = "voucherFacade")
	//private SOVoucherFacade soVoucherFacade;

	@Resource(name = "defaultVoucherFacade")
	private VoucherFacade voucherFacade;

	@Resource(name = "siteConfigService")
	private SiteConfigService siteConfigService;

	@Resource(name = "sessionService")
	private SessionService sessionService;

	@Resource(name = "simpleBreadcrumbBuilder")
	private ResourceBreadcrumbBuilder resourceBreadcrumbBuilder;

	@Resource(name = "promoValidationFacade")
	private SOPromoValidationFacade promoValidationFacade;

	@Resource
	private BaseStoreService baseStoreService;

	@Resource
	private SOPromotionService soPromotionService;

	@Resource
	private CartService cartService;

	@Resource
	private VoucherService voucherService;

	@Resource
	private SOVoucherService soVoucherService;


	@Resource(name = "modelService")
	private ModelService modelService;

	@Resource(name = "voucherModelService")
	private VoucherModelService voucherModelService;

	@Resource(name = "userService")
	private UserService userService;

	@Resource
	private CommerceCartService commerceCartService;

	@Resource(name = "socartService")
	private SOCartService soCartService;

	@Resource(name = "commerceCartCalculationStrategy")
	private DefaultSOCommerceCartCalculationStrategy commerceCartCalculationStrategy;

	// Public getter used in a test
	@Override
	public SiteConfigService getSiteConfigService()
	{
		return siteConfigService;
	}

	@ModelAttribute("showCheckoutStrategies")
	public boolean isCheckoutStrategyVisible()
	{
		return getSiteConfigService().getBoolean(SHOW_CHECKOUT_STRATEGY_OPTIONS, false);
	}

	/*
	 * Display the cart page
	 */
	@RequestMapping(method = RequestMethod.GET)
	public String showCart(final Model model, final HttpServletRequest request)
			throws CMSItemNotFoundException, CommerceCartModificationException
	{
		prepareDataForPage(model);

		final CartModel sessionCart = cartService.getSessionCart();

		final Collection<String> appliedVoucherCodes = voucherService.getAppliedVoucherCodes(sessionCart);
		for (final String appliedVoucherCode : appliedVoucherCodes)
		{
			final VoucherModel appliedVoucherModel = getVoucher(appliedVoucherCode);
			if (appliedVoucherModel.getVoucherType() != null)
			{
				getSessionService().setAttribute("specialCustomer", "yes");
			}
		}
		model.addAttribute("specialCustomer", getSessionService().getAttribute("specialCustomer"));
		model.addAttribute(new RegisterForm());
		return getViewForPage(model);

	}

	public VoucherModel getVoucher(final String voucherCode)
	{

		final Voucher voucher = VoucherManager.getInstance().getVoucher(voucherCode);
		if (voucher == null)
		{
			return null;
		}
		else
		{
			return modelService.get(voucher);
		}
	}

	/**
	 * Handle the '/cart/checkout' request url. This method checks to see if the cart is valid before allowing the
	 * checkout to begin. Note that this method does not require the user to be authenticated and therefore allows us to
	 * validate that the cart is valid without first forcing the user to login. The cart will be checked again once the
	 * user has logged in.
	 *
	 * @return The page to redirect to
	 */
	@RequestMapping(value = "/checkout", method = RequestMethod.GET)
	@RequireHardLogIn
	public String cartCheck(final Model model, final RedirectAttributes redirectModel) throws CommerceCartModificationException
	{
		SessionOverrideCheckoutFlowFacade.resetSessionOverrides();

		if (!cartFacade.hasSessionCart() || cartFacade.getSessionCart().getEntries().isEmpty())
		{
			LOG.info("Missing or empty cart");

			// No session cart or empty session cart. Bounce back to the cart page.
			return REDIRECT_PREFIX + "/cart";
		}


		if (validateCart(redirectModel))
		{
			return REDIRECT_PREFIX + "/cart";
		}

		// Redirect to the start of the checkout flow to begin the checkout process
		// We just redirect to the generic '/checkout' page which will actually select the checkout flow
		// to use. The customer is not necessarily logged in on this request, but will be forced to login
		// when they arrive on the '/checkout' page.
		return REDIRECT_PREFIX + "/checkout";
	}

	@RequestMapping(value = "/reloadCartEntries.json", method = RequestMethod.POST, produces = "application/json")
	public String reloadCartEntries(final Model model, final RedirectAttributes redirectModel)
	{
		commerceCartCalculationStrategy.calculateCart(cartService.getSessionCart());
		final CartData cartData = cartFacade.getSessionCart();
		model.addAttribute("cartData", cartData);
		return ControllerConstants.Views.Fragments.Cart.CartItemsPopup;
	}

	@RequestMapping(value = "/updateCartEntries.json", method = RequestMethod.POST, produces = "application/json")
	public String updateCartEntries(final Model model, final RedirectAttributes redirectModel, final Long entryNumber,
			final Long quantity, @RequestParam("productCode") final String code)
	{

		final CartModel cart = cartService.getSessionCart();
		final List<AbstractOrderEntryModel> entries = cart.getEntries();

		final Long quantityDifference = quantity - entries.get(entryNumber.intValue()).getQuantity();


		HashMap promoMap = new HashMap<>();
		HashMap validationMap = new HashMap();
		//check promotion validation
		//check if the special account promotion is available
		final AbstractPromotionConfigurationModel promoConfig = sessionService.getAttribute("SPECIALACCOUNTCONFIGURATION");

		//check voucher validation, validEntry is called to determing if the quantity is added or removed
		final boolean validEntry = promoValidationFacade.isValidEntry(code, quantity, entryNumber);
		//promoApplied is called to determine if the voucher is applied to the current cart
		final boolean promoApplied = promoValidationFacade.promoApplied();

		if (promoApplied)
		{
			validationMap = promoValidationFacade.validateVoucherConfiguration(entryNumber, quantity, code);
		}


		if (promoConfig != null && quantityDifference > 0)
		{
			final List<CategoryQuantityLimitConfModel> productLimitList = promoConfig.getProductLimitList();
			promoMap = promoValidationFacade.validatePromoConfiguration(productLimitList, entryNumber, quantity, code);
		}



		if (promoMap != null && !promoMap.isEmpty() && quantityDifference > 0)
		{
			GlobalMessages.addMessage(model, GlobalMessages.ERROR_MESSAGES_HOLDER, "item.promo.not.allowed", new Object[]
			{ promoMap.get("productCode"), promoMap.get("quantity"), promoMap.get("category") });
			return reloadCartEntries(model, redirectModel);
		}
		else if (validEntry && promoApplied && validationMap != null && !validationMap.isEmpty())
		{
			GlobalMessages.addMessage(model, GlobalMessages.ERROR_MESSAGES_HOLDER, "item.promo.not.allowed", new Object[]
			{ validationMap.get("productCode"), validationMap.get("quantity"), validationMap.get("category") });
			return reloadCartEntries(model, redirectModel);
		}
		else if (cartFacade.getSessionCart().getEntries() != null)
		{
			if (cartFacade.isRXProductInCart())
			{



				if (entries != null && !entries.isEmpty())
				{
					for (final AbstractOrderEntryModel entry : entries)
					{
						try
						{
							final CartModificationData rxItemRemoved0 = cartFacade.updateCartEntry(entry.getEntryNumber(),
									quantity.longValue());
						}
						catch (final CommerceCartModificationException ex)
						{
							LOG.warn("Couldn't remove RX product" + ex);
						}

					}
				}


				// Success in removing entry
				GlobalMessages.addFlashMessage(redirectModel, GlobalMessages.CONF_MESSAGES_HOLDER, "basket.page.message.remove");
				return reloadCartEntries(model, redirectModel);
			}
			else
			{
				try
				{
					final CartModificationData cartModification = cartFacade.updateCartEntry(entryNumber, quantity.longValue());
					model.addAttribute("stockStatus", cartModification.getStatusCode());
					model.addAttribute("current_entry_no", entryNumber);

					final CartModel sessionCartModel = cartService.getSessionCart();
					if (sessionCartModel != null && !cartFacade.isGiftCardProductInCart())
					{
						sessionCartModel.setGcProductFriendsEmail(null);
						sessionCartModel.setGcProductFriendsName(null);
						sessionCartModel.setGcProductFriendsLastName(null);
						sessionCartModel.setGiftProductMessage(null);
						modelService.save(sessionCartModel);
					}

					if (cartModification.getQuantity() == quantity.longValue())
					{
						// Success
						if (cartModification.getQuantity() == 0)
						{
							// Success in removing entry
							GlobalMessages.addFlashMessage(redirectModel, GlobalMessages.CONF_MESSAGES_HOLDER,
									"basket.page.message.remove");
						}
						else
						{
							// Success in update quantity
							GlobalMessages.addFlashMessage(redirectModel, GlobalMessages.CONF_MESSAGES_HOLDER,
									"basket.page.message.update");
						}
					}
					return reloadCartEntries(model, redirectModel);
				}
				catch (final CommerceCartModificationException ex)
				{
					LOG.warn("Couldn't update product with the entry number: " + entryNumber + ".", ex);
				}
			}
		}
		try
		{
			prepareDataForPage(model);
		}
		catch (final CMSItemNotFoundException e)
		{
			// YTODO Auto-generated catch block
			e.printStackTrace();
		}
		return reloadCartEntries(model, redirectModel);
	}

	@ResponseBody
	@RequestMapping(value = "/savePrescriptionMedia.json", method = RequestMethod.POST, produces = "application/json")
	public HashMap<String, Object> savePrescriptionMedia(@RequestParam("file") final MultipartFile file)
	{
		final HashMap<String, Object> response = new HashMap<String, Object>();

		try
		{
			final PrescriptionMediaModel prescriptionMedia = cartFacade.savePrescriptionDocument(file);
			response.put("status", "success");
			response.put("mediaUrl", prescriptionMedia.getUrl());
			response.put("mediaName", prescriptionMedia.getRealFileName());
			response.put("mediaDate", prescriptionMedia.getCreationtime());
		}
		catch (final IOException error)
		{
			response.put("status", "error");
		}

		return response;
	}

	@ResponseBody
	@RequestMapping(value = "/removePrescriptionMedia.json", method = RequestMethod.POST, produces = "application/json")
	public String savePrescriptionMedia()
	{

		cartFacade.removePrescriptionDocument();
		return "success";
	}

	@ResponseBody
	@RequestMapping(value = "/savePrescriptionDetails.json", method = RequestMethod.POST, produces = "application/json")
	public String savePrescriptionMajors(@Valid final PrescriptionMajorsForm prescriptionForm)
	{
		if (prescriptionForm.getSphereLeft() != null && prescriptionForm.getSphereRight() != null
				&& ((prescriptionForm.getCylinderLeft() != null) == (prescriptionForm.getCylinderRight() != null) == (prescriptionForm
						.getAxisLeft() != null) == (prescriptionForm.getAxisRight() != null))
				&& prescriptionForm.getPupilDistance() != null)
		{
			// final FloatValidator floatValidator = FloatValidator.getInstance();
			// if (prescriptionForm.getFromBifocalPage())
			// {
			// 	if (prescriptionForm.getBifocalADDLeft() != null && prescriptionForm.getBifocalADDRight() != null)
			// 	{
			// 		if (!floatValidator.isInRange(prescriptionForm.getBifocalADDLeft(), 1.25, 2.50)
			// 				|| !floatValidator.isInRange(prescriptionForm.getBifocalADDRight(), 1.25, 2.50))
			// 		{
			// 			return "invalidBifocal";
			// 		}
			// 	}
			// 	else
			// 	{
			// 		return "nullForm";
			// 	}
			// }
			// if (!floatValidator.isInRange(prescriptionForm.getSphereLeft(), -6, 3.25)
			// 		|| !floatValidator.isInRange(prescriptionForm.getSphereRight(), -6, 3.25))
			// {
			// 	return "invalidSphere";
			// }

			// if ((prescriptionForm.getCylinderLeft() != null) && (prescriptionForm.getCylinderRight() != null)){
			// 	if (!floatValidator.isInRange(prescriptionForm.getCylinderLeft(), -2, 2) || !floatValidator.isInRange(prescriptionForm.getCylinderRight(), -2, 2))
			// 	{
			// 		return "invalidCylinder";
			// 	}
			// }

			// if((prescriptionForm.getAxisLeft() != null) && (prescriptionForm.getAxisRight() != null)){
			// 	if (!floatValidator.isInRange(prescriptionForm.getAxisLeft().floatValue(), 0, 180) || !floatValidator.isInRange(prescriptionForm.getAxisRight().floatValue(), 0, 180))
			// 	{
			// 		return "invalidAxis";
			// 	}
			// }

			final PrescriptionFormData prescriptionFormData = new PrescriptionFormData();
			prescriptionFormData.setSphereLeft(prescriptionForm.getSphereLeft());
			prescriptionFormData.setSphereRight(prescriptionForm.getSphereRight());
			prescriptionFormData.setCylinderLeft(prescriptionForm.getCylinderLeft());
			prescriptionFormData.setCylinderRight(prescriptionForm.getCylinderRight());
			prescriptionFormData.setAxisLeft(prescriptionForm.getAxisLeft());
			prescriptionFormData.setAxisRight(prescriptionForm.getAxisRight());
			prescriptionFormData.setAxisLeft(prescriptionForm.getAxisLeft());
			prescriptionFormData.setBifocalADDLeft(prescriptionForm.getBifocalADDLeft());
			prescriptionFormData.setBifocalADDRight(prescriptionForm.getBifocalADDRight());
			if (prescriptionForm.getPupilDistance())
			{
				prescriptionFormData.setPupilDistanceSingle(prescriptionForm.getPupilDistanceSingle());
				prescriptionFormData.setPupilDistanceSeperateLeft(null);
				prescriptionFormData.setPupilDistanceSeperateRight(null);
			}
			else
			{
				prescriptionFormData.setPupilDistanceSingle(null);
				prescriptionFormData.setPupilDistanceSeperateLeft(prescriptionForm.getPupilDistanceSeperateLeft());
				prescriptionFormData.setPupilDistanceSeperateRight(prescriptionForm.getPupilDistanceSeperateRight());
			}
			cartFacade.savePrescriptionDetails(prescriptionFormData);

			return "success";
		}
		return "nullForm";
	}

	// This controller method is used to allow the site to force the visitor through a specified checkout flow.
	// If you only have a static configured checkout flow then you can remove this method.
	@RequestMapping(value = "/checkout/select-flow", method = RequestMethod.GET)
	@RequireHardLogIn
	public String initCheck(final Model model, final RedirectAttributes redirectModel,
			@RequestParam(value = "flow", required = false) final CheckoutFlowEnum checkoutFlow,
			@RequestParam(value = "pci", required = false) final CheckoutPciOptionEnum checkoutPci)
					throws CommerceCartModificationException
	{
		SessionOverrideCheckoutFlowFacade.resetSessionOverrides();

		if (!cartFacade.hasSessionCart() || cartFacade.getSessionCart().getEntries().isEmpty())
		{
			LOG.info("Missing or empty cart");

			// No session cart or empty session cart. Bounce back to the cart page.
			return REDIRECT_PREFIX + "/cart";
		}

		// Override the Checkout Flow setting in the session
		if (checkoutFlow != null && StringUtils.isNotBlank(checkoutFlow.getCode()))
		{
			SessionOverrideCheckoutFlowFacade.setSessionOverrideCheckoutFlow(checkoutFlow);
		}

		// Override the Checkout PCI setting in the session
		if (checkoutPci != null && StringUtils.isNotBlank(checkoutPci.getCode()))
		{
			SessionOverrideCheckoutFlowFacade.setSessionOverrideSubscriptionPciOption(checkoutPci);
		}

		// Redirect to the start of the checkout flow to begin the checkout process
		// We just redirect to the generic '/checkout' page which will actually select the checkout flow
		// to use. The customer is not necessarily logged in on this request, but will be forced to login
		// when they arrive on the '/checkout' page.
		return REDIRECT_PREFIX + "/checkout";
	}

	@RequestMapping(value = "/data", method = RequestMethod.GET)
	public String getCart(final Model model)
	{
		//cartService.calculateCart(cartService.getSessionCart());
		commerceCartCalculationStrategy.calculateCart(cartService.getSessionCart());
		final CartData cartData = cartFacade.getSessionCart();
		model.addAttribute("totalPrice", cartData.getTotalPrice());
		model.addAttribute("subTotal", cartData.getSubTotal());
		if (cartData.getDeliveryCost() != null)
		{
			final PriceData withoutDelivery = cartData.getDeliveryCost();
			withoutDelivery.setValue(cartData.getTotalPrice().getValue().subtract(cartData.getDeliveryCost().getValue()));
			model.addAttribute("totalNoDelivery", withoutDelivery);
		}
		else
		{
			model.addAttribute("totalNoDelivery", cartData.getTotalPrice());
		}
		model.addAttribute("totalItems", cartData.getTotalUnitCount());
		model.addAttribute("cartData", cartData);
		return ControllerConstants.Views.Fragments.Cart.CartTotalPopup;
	}

	@SuppressWarnings("boxing")
	@RequestMapping(value = "/update", method = RequestMethod.POST)
	public String updateCartQuantities(@RequestParam("entryNumber") final long entryNumber, final Model model,
			@Valid final UpdateQuantityForm form, final BindingResult bindingResult, final HttpServletRequest request,
			final RedirectAttributes redirectModel, @RequestParam("productCode") final String code) throws CMSItemNotFoundException
	{
		HashMap promoMap = new HashMap<>();
		//check promotion validation
		//check if the special account promotion is available
		final AbstractPromotionConfigurationModel promoConfig = sessionService.getAttribute("SPECIALACCOUNTCONFIGURATION");

		//check voucher validation
		final boolean validEntry = promoValidationFacade.isValidEntry(code, form.getQuantity(), entryNumber);
		final boolean promoApplied = promoValidationFacade.promoApplied();
		HashMap validationMap = new HashMap();
		if (promoApplied)
		{
			validationMap = promoValidationFacade.validateVoucherConfiguration(entryNumber, form.getQuantity(), code);
		}


		// for (final ObjectError error : bindingResult.getAllErrors())
		// {
		// 	if (error.getCode().equals("typeMismatch"))
		// 	{
		// 		GlobalMessages.addErrorMessage(model, "basket.error.quantity.invalid");
		// 	}
		// 	else
		// 	{
		// 		GlobalMessages.addErrorMessage(model, error.getDefaultMessage());
		// 	}
		// }



		if (promoConfig != null)
		{
			final List<CategoryQuantityLimitConfModel> productLimitList = promoConfig.getProductLimitList();
			promoMap = promoValidationFacade.validatePromoConfiguration(productLimitList, entryNumber, form.getQuantity(), code);
		}
		if (promoMap != null && !promoMap.isEmpty())
		{
			GlobalMessages.addMessage(model, GlobalMessages.ERROR_MESSAGES_HOLDER, "item.promo.not.allowed", new Object[]
			{ validationMap.get("productCode"), validationMap.get("quantity"), validationMap.get("category") });
		}
		else if (validEntry && promoApplied && validationMap != null && !validationMap.isEmpty())
		{
			GlobalMessages.addMessage(model, GlobalMessages.ERROR_MESSAGES_HOLDER, "item.promo.not.allowed", new Object[]
			{ validationMap.get("productCode"), validationMap.get("quantity"), validationMap.get("category") });
		}

		else if (cartFacade.getSessionCart().getEntries() != null)
		{
			final ProductData productData = productFacade.getProductForCodeAndOptions(form.getProductCode(),
					Arrays.asList(ProductOption.BASIC));
			if (form.getQuantity() == 0l && productData.getType().equals("SORXLensVariant"))
			{
				for (final OrderEntryData entry : cartFacade.getSessionCart().getEntries())
				{
					LOG.debug(entry);
					try
					{
						final CartModificationData cartModification = cartFacade.updateCartEntry(entryNumber,
								form.getQuantity().longValue());
						if (cartModification.getQuantity() == form.getQuantity().longValue())
						{
							// Success
						}
						else if (cartModification.getQuantity() > 0)
						{
							// Less than successful
							GlobalMessages.addFlashMessage(redirectModel, GlobalMessages.ERROR_MESSAGES_HOLDER,
									"basket.page.message.update.reducedNumberOfItemsAdded.lowStock", new Object[]
							{ cartModification.getEntry().getProduct().getName(), cartModification.getQuantity(), form.getQuantity(),
									request.getRequestURL().append(cartModification.getEntry().getProduct().getUrl()) });
						}
						else
						{
							// No more stock available
							GlobalMessages.addFlashMessage(redirectModel, GlobalMessages.ERROR_MESSAGES_HOLDER,
									"basket.page.message.update.reducedNumberOfItemsAdded.noStock", new Object[]
							{ cartModification.getEntry().getProduct().getName(),
									request.getRequestURL().append(cartModification.getEntry().getProduct().getUrl()) });
						}

						// Redirect to the cart page on update success so that the browser doesn't re-post again
					}
					catch (final CommerceCartModificationException ex)
					{
						LOG.warn("Couldn't update product with the entry number: " + entryNumber + ".", ex);
					}
				}

				// Success in removing entry
				GlobalMessages.addFlashMessage(redirectModel, GlobalMessages.CONF_MESSAGES_HOLDER, "basket.page.message.remove");

				return REDIRECT_PREFIX + "/cart";
			}
			else
			{
				try
				{
					final CartModificationData cartModification = cartFacade.updateCartEntry(entryNumber,
							form.getQuantity().longValue());
					if (cartModification.getQuantity() == form.getQuantity().longValue())
					{
						// Success

						if (cartModification.getQuantity() == 0)
						{
							// Success in removing entry
							GlobalMessages.addFlashMessage(redirectModel, GlobalMessages.CONF_MESSAGES_HOLDER,
									"basket.page.message.remove");
						}
						else
						{
							// Success in update quantity
							GlobalMessages.addFlashMessage(redirectModel, GlobalMessages.CONF_MESSAGES_HOLDER,
									"basket.page.message.update");
						}
					}
					else if (cartModification.getQuantity() > 0)
					{
						// Less than successful
						GlobalMessages.addFlashMessage(redirectModel, GlobalMessages.ERROR_MESSAGES_HOLDER,
								"basket.page.message.update.reducedNumberOfItemsAdded.lowStock", new Object[]
						{ cartModification.getEntry().getProduct().getName(), cartModification.getQuantity(), form.getQuantity(),
								request.getRequestURL().append(cartModification.getEntry().getProduct().getUrl()) });
					}
					else
					{
						// No more stock available
						GlobalMessages.addFlashMessage(redirectModel, GlobalMessages.ERROR_MESSAGES_HOLDER,
								"basket.page.message.update.reducedNumberOfItemsAdded.noStock", new Object[]
						{ cartModification.getEntry().getProduct().getName(),
								request.getRequestURL().append(cartModification.getEntry().getProduct().getUrl()) });
					}

					// Redirect to the cart page on update success so that the browser doesn't re-post again
					return REDIRECT_PREFIX + "/cart";
				}
				catch (final CommerceCartModificationException ex)
				{
					LOG.warn("Couldn't update product with the entry number: " + entryNumber + ".", ex);
				}
			}
		}

		prepareDataForPage(model);
		return ControllerConstants.Views.Pages.Cart.CartPage;
	}



	/*
	 * @RequestMapping(value = "/applyVoucher", method = RequestMethod.GET) public String
	 * applyVoucher(@RequestHeader(value = "referer", required = false) final String referer,
	 *
	 * @RequestParam(required = false) final String voucherCode, final Model model, final HttpServletRequest request,
	 * final HttpServletResponse response, final RedirectAttributes redirectModel) throws CMSItemNotFoundException,
	 * CommerceCartModificationException { if (StringUtils.isEmpty(voucherCode)) { model.addAttribute("invalidvoucher",
	 * Boolean.TRUE); GlobalMessages.addFlashMessage(redirectModel, GlobalMessages.ERROR_MESSAGES_HOLDER,
	 * "invalid.voucher.code"); if (referer != null) { return REDIRECT_PREFIX + referer; } }
	 *
	 * final VoucherModel voucher = voucherFacade.isValidVoucher(voucherCode); if (voucher == null) {
	 * GlobalMessages.addFlashMessage(redirectModel, GlobalMessages.ERROR_MESSAGES_HOLDER, "invalid.voucher.code"); if
	 * (referer != null) { return REDIRECT_PREFIX + referer; } }
	 *
	 * if (voucher != null && soPromotionService.getVoucherConfiguration(voucher.getVoucherType()) == null) {
	 *
	 * GlobalMessages.addFlashMessage(redirectModel, GlobalMessages.ERROR_MESSAGES_HOLDER, "invalid.voucher.setup");
	 * return voucherredirect(); }
	 *
	 * //check if the current voucher is already applied to the current Cart final boolean cartVoucher =
	 * voucherFacade.isCartVoucher(voucherCode); if (cartVoucher) { soVoucherService.removeVoucher();
	 *
	 * final boolean voucherRedeemed = voucherFacade.redeemVoucher(voucherCode); if (voucherRedeemed) {
	 * GlobalMessages.addFlashMessage(redirectModel, GlobalMessages.ERROR_MESSAGES_HOLDER, "voucher.reapplied"); return
	 * voucherredirect(); } else { GlobalMessages.addFlashMessage(redirectModel, GlobalMessages.ERROR_MESSAGES_HOLDER,
	 * "invalid.voucher.code"); if (referer != null) { return REDIRECT_PREFIX + referer; } } }
	 *
	 * // final boolean redeemable = voucherFacade.isRedeemable(voucherCode);
	 *
	 *
	 * // if (!redeemable) // { // GlobalMessages.addFlashMessage(redirectModel, GlobalMessages.ERROR_MESSAGES_HOLDER,
	 * "invalid.voucher"); // return REDIRECT_PREFIX + "/cart"; // }
	 *
	 * //check if it is a regular voucher which doesn't require any validation if (voucher.getVoucherType() == null) {
	 * final boolean voucherRedeemed = voucherFacade.redeemVoucher(voucherCode); if (voucherRedeemed) {
	 * GlobalMessages.addFlashMessage(redirectModel, GlobalMessages.CONF_MESSAGES_HOLDER, "Voucher applied"); return
	 * voucherredirect(); } else { GlobalMessages.addFlashMessage(redirectModel, GlobalMessages.ERROR_MESSAGES_HOLDER,
	 * "invalid.voucher.code"); if (referer != null) { return REDIRECT_PREFIX + referer; } } } else { //the following
	 * checks will be for voucherType validation //check if promotion already exists
	 *
	 * final HashMap promoPrecedenceMap = voucherFacade.checkPromoPrecedence(voucherCode); //if no promotion exists then
	 * validate voucher-voucher precedence else validate promo-voucher precedence if (promoPrecedenceMap != null &&
	 * promoPrecedenceMap.isEmpty()) {
	 *
	 *
	 * //this check is to validate voucher configuration in hmc and check if limit is not exceeded final
	 * VoucherPromotionConfigurationModel voucherConfiguration = soPromotionService.getVoucherConfiguration(voucher
	 * .getVoucherType()); final List<CategoryQuantityLimitConfModel> productLimitList =
	 * voucherConfiguration.getProductLimitList(); final HashMap configMap =
	 * voucherFacade.validateVoucherConfiguration(productLimitList);
	 *
	 *
	 * if (!configMap.isEmpty()) { GlobalMessages.addFlashMessage(redirectModel, GlobalMessages.ERROR_MESSAGES_HOLDER,
	 * "promo.not.allowed", new Object[] { configMap.get("productCode"), configMap.get("quantity"),
	 * configMap.get("category") });
	 *
	 * LOG.info("trying to apply"); return voucherredirect(); // return REDIRECT_PREFIX + "/cart"; }
	 *
	 * final HashMap vouchePrecedenceMap = voucherFacade.checkVoucherPrecedence(voucherCode); //this implies that already
	 * existing voucher has low precedence and is released if (vouchePrecedenceMap != null &&
	 * !vouchePrecedenceMap.isEmpty() && vouchePrecedenceMap.get("voucherReleased") != null &&
	 * vouchePrecedenceMap.get("voucherReleased").equals("true")) { if (voucherFacade.redeemVoucher(voucherCode)) {
	 *
	 * GlobalMessages.addFlashMessage(redirectModel, GlobalMessages.CONF_MESSAGES_HOLDER,
	 * "voucher.code.applied.existing.voucher.released", new Object[] { vouchePrecedenceMap.get("currentVoucher"),
	 * vouchePrecedenceMap.get("appliedVoucher") }); return voucherredirect(); } else {
	 * GlobalMessages.addFlashMessage(redirectModel, GlobalMessages.ERROR_MESSAGES_HOLDER, "invalid.voucher.code"); } }
	 * else if (vouchePrecedenceMap != null && !vouchePrecedenceMap.isEmpty() &&
	 * vouchePrecedenceMap.get("voucherReleased") != null && vouchePrecedenceMap.get("voucherReleased").equals("false"))
	 * { GlobalMessages.addFlashMessage(redirectModel, GlobalMessages.ERROR_MESSAGES_HOLDER,
	 * "voucher.code.not.applied.existing.voucher.not.released", new Object[] {
	 * vouchePrecedenceMap.get("currentVoucher"), vouchePrecedenceMap.get("appliedVoucher") }); } else { //apply the
	 * voucher just as is final boolean voucherRedeemed = voucherFacade.redeemVoucher(voucherCode); if (voucherRedeemed)
	 * { GlobalMessages.addFlashMessage(redirectModel, GlobalMessages.CONF_MESSAGES_HOLDER, "Voucher applied"); return
	 * voucherredirect(); } }
	 *
	 * } //this implies that promotion exists and needs to be validated against voucher's precedence else if
	 * (promoPrecedenceMap != null && !promoPrecedenceMap.isEmpty() && promoPrecedenceMap.get("promoExists") != null &&
	 * promoPrecedenceMap.get("promoExists").equals("true") && promoPrecedenceMap.get("promoRemoved") != null &&
	 * promoPrecedenceMap.get("promoRemoved").equals("true")) {
	 *
	 * final HashMap vouchePrecedenceMap = voucherFacade.checkVoucherPrecedence(voucherCode);
	 *
	 * if (vouchePrecedenceMap != null && vouchePrecedenceMap.isEmpty()) { if (voucherFacade.redeemVoucher(voucherCode))
	 * { GlobalMessages.addFlashMessage(redirectModel, GlobalMessages.CONF_MESSAGES_HOLDER,
	 * "voucher.code.applied.promo.released"); return voucherredirect();
	 *
	 * } else { GlobalMessages.addFlashMessage(redirectModel, GlobalMessages.ERROR_MESSAGES_HOLDER,
	 * "invalid.voucher.code"); } }
	 *
	 * } else { //voucher cannot be applied as it has less precedence than promo
	 * GlobalMessages.addFlashMessage(redirectModel, GlobalMessages.ERROR_MESSAGES_HOLDER, "promo.precedence.more"); }
	 *
	 * }
	 *
	 * return REDIRECT_PREFIX + "/cart";
	 *
	 * GlobalMessages.addFlashMessage(redirectModel, GlobalMessages.INFO_MESSAGES_HOLDER, "Voucher Applied"); return
	 * REDIRECT_PREFIX + ROOT;
	 *
	 * }
	 */




	@RequestMapping(value = "/applyVoucher", method = RequestMethod.GET)
	public String applyVoucher(@RequestHeader(value = "referer", required = false) final String referer,
			@RequestParam(required = false) final String voucherCode, final Model model, final HttpServletRequest request,
			final HttpServletResponse response, final RedirectAttributes redirectModel)
					throws CMSItemNotFoundException, CommerceCartModificationException
	{
		if (StringUtils.isEmpty(voucherCode))
		{
			model.addAttribute("invalidvoucher", Boolean.TRUE);
			GlobalMessages.addFlashMessage(redirectModel, GlobalMessages.ERROR_MESSAGES_HOLDER, "invalid.voucher.code");
			if (referer != null)
			{
				return REDIRECT_PREFIX + referer;
			}
		}

		final CartModel cartModel = cartService.getSessionCart();
		final Collection<String> appliedVoucherCodes = voucherService.getAppliedVoucherCodes(cartModel);

		if (CollectionUtils.isNotEmpty(appliedVoucherCodes))
		{
			LOG.info(
					"############ Multiple Voucher Codes Cannot be redeemed on Same Cart. There is a already a voucher present on cart. ##################");
			GlobalMessages.addFlashMessage(redirectModel, GlobalMessages.ERROR_MESSAGES_HOLDER, "invalid.multiple.voucher.codes");
			if (referer != null)
			{
				return REDIRECT_PREFIX + referer;
			}
		}

		if (checkIsPromotionInCart(cartModel))
		{
			LOG.info("############ Voucher cannot be redeemed as there is already a promotion applied to Cart. ##################");
			GlobalMessages.addFlashMessage(redirectModel, GlobalMessages.ERROR_MESSAGES_HOLDER, "invalid.voucher.redeem");
			if (referer != null)
			{
				return REDIRECT_PREFIX + referer;
			}
		}

		if (voucherFacade.checkVoucherCode(voucherCode))
		{
			try
			{
				voucherFacade.applyVoucher(voucherCode);
				commerceCartCalculationStrategy.calculateCart(cartService.getSessionCart());
			}
			catch (final VoucherOperationException e)
			{
				LOG.error("Error while applying the voucher" + e.getMessage());
				GlobalMessages.addFlashMessage(redirectModel, GlobalMessages.ERROR_MESSAGES_HOLDER, "invalid.voucher.code");
				if (referer != null)
				{
					return REDIRECT_PREFIX + referer;
				}
			}
			GlobalMessages.addFlashMessage(redirectModel, GlobalMessages.CONF_MESSAGES_HOLDER, "Voucher applied");
			return voucherredirect();
		}
		else
		{
			GlobalMessages.addFlashMessage(redirectModel, GlobalMessages.ERROR_MESSAGES_HOLDER, "invalid.voucher.code");
			if (referer != null)
			{
				return REDIRECT_PREFIX + referer;
			}
		}

		/*
		 * if (voucher != null && soPromotionService.getVoucherConfiguration(voucher.getVoucherType()) == null) {
		 *
		 * GlobalMessages.addFlashMessage(redirectModel, GlobalMessages.ERROR_MESSAGES_HOLDER, "invalid.voucher.setup");
		 * return voucherredirect(); }
		 */

		//check if the current voucher is already applied to the current Cart
		/*
		 * final boolean cartVoucher = soVoucherFacade.isCartVoucher(voucherCode); if (cartVoucher) {
		 * soVoucherService.removeVoucher();
		 *
		 * voucherFacade.applyVoucher(voucherCode); //final boolean voucherRedeemed =
		 * soVoucherFacade.redeemVoucher(voucherCode); if (voucherRedeemed) {
		 * GlobalMessages.addFlashMessage(redirectModel, GlobalMessages.ERROR_MESSAGES_HOLDER, "voucher.reapplied");
		 * return voucherredirect(); } else { GlobalMessages.addFlashMessage(redirectModel,
		 * GlobalMessages.ERROR_MESSAGES_HOLDER, "invalid.voucher.code"); if (referer != null) { return REDIRECT_PREFIX +
		 * referer; } } }
		 */

		// final boolean redeemable = voucherFacade.isRedeemable(voucherCode);


		// if (!redeemable)
		// {
		// 	GlobalMessages.addFlashMessage(redirectModel, GlobalMessages.ERROR_MESSAGES_HOLDER, "invalid.voucher");
		// 	return REDIRECT_PREFIX + "/cart";
		// }

		//check if it is a regular voucher which doesn't require any validation
		/*
		 * if (voucher.getVoucherType() == null) { final boolean voucherRedeemed =
		 * voucherFacade.redeemVoucher(voucherCode); if (voucherRedeemed) { GlobalMessages.addFlashMessage(redirectModel,
		 * GlobalMessages.CONF_MESSAGES_HOLDER, "Voucher applied"); return voucherredirect(); } else {
		 * GlobalMessages.addFlashMessage(redirectModel, GlobalMessages.ERROR_MESSAGES_HOLDER, "invalid.voucher.code"); if
		 * (referer != null) { return REDIRECT_PREFIX + referer; } } } else { //the following checks will be for
		 * voucherType validation //check if promotion already exists
		 *
		 * final HashMap promoPrecedenceMap = voucherFacade.checkPromoPrecedence(voucherCode); //if no promotion exists
		 * then validate voucher-voucher precedence else validate promo-voucher precedence if (promoPrecedenceMap != null
		 * && promoPrecedenceMap.isEmpty()) {
		 *
		 *
		 * //this check is to validate voucher configuration in hmc and check if limit is not exceeded final
		 * VoucherPromotionConfigurationModel voucherConfiguration = soPromotionService.getVoucherConfiguration(voucher
		 * .getVoucherType()); final List<CategoryQuantityLimitConfModel> productLimitList =
		 * voucherConfiguration.getProductLimitList(); final HashMap configMap =
		 * voucherFacade.validateVoucherConfiguration(productLimitList);
		 *
		 *
		 * if (!configMap.isEmpty()) { GlobalMessages.addFlashMessage(redirectModel, GlobalMessages.ERROR_MESSAGES_HOLDER,
		 * "promo.not.allowed", new Object[] { configMap.get("productCode"), configMap.get("quantity"),
		 * configMap.get("category") });
		 *
		 * LOG.info("trying to apply"); return voucherredirect(); // return REDIRECT_PREFIX + "/cart"; }
		 *
		 * final HashMap vouchePrecedenceMap = voucherFacade.checkVoucherPrecedence(voucherCode); //this implies that
		 * already existing voucher has low precedence and is released if (vouchePrecedenceMap != null &&
		 * !vouchePrecedenceMap.isEmpty() && vouchePrecedenceMap.get("voucherReleased") != null &&
		 * vouchePrecedenceMap.get("voucherReleased").equals("true")) { if (voucherFacade.redeemVoucher(voucherCode)) {
		 *
		 * GlobalMessages.addFlashMessage(redirectModel, GlobalMessages.CONF_MESSAGES_HOLDER,
		 * "voucher.code.applied.existing.voucher.released", new Object[] { vouchePrecedenceMap.get("currentVoucher"),
		 * vouchePrecedenceMap.get("appliedVoucher") }); return voucherredirect(); } else {
		 * GlobalMessages.addFlashMessage(redirectModel, GlobalMessages.ERROR_MESSAGES_HOLDER, "invalid.voucher.code"); }
		 * } else if (vouchePrecedenceMap != null && !vouchePrecedenceMap.isEmpty() &&
		 * vouchePrecedenceMap.get("voucherReleased") != null &&
		 * vouchePrecedenceMap.get("voucherReleased").equals("false")) { GlobalMessages.addFlashMessage(redirectModel,
		 * GlobalMessages.ERROR_MESSAGES_HOLDER, "voucher.code.not.applied.existing.voucher.not.released", new Object[] {
		 * vouchePrecedenceMap.get("currentVoucher"), vouchePrecedenceMap.get("appliedVoucher") }); } else { //apply the
		 * voucher just as is final boolean voucherRedeemed = voucherFacade.redeemVoucher(voucherCode); if
		 * (voucherRedeemed) { GlobalMessages.addFlashMessage(redirectModel, GlobalMessages.CONF_MESSAGES_HOLDER,
		 * "Voucher applied"); return voucherredirect(); } }
		 *
		 * } //this implies that promotion exists and needs to be validated against voucher's precedence else if
		 * (promoPrecedenceMap != null && !promoPrecedenceMap.isEmpty() && promoPrecedenceMap.get("promoExists") != null
		 * && promoPrecedenceMap.get("promoExists").equals("true") && promoPrecedenceMap.get("promoRemoved") != null &&
		 * promoPrecedenceMap.get("promoRemoved").equals("true")) {
		 *
		 * final HashMap vouchePrecedenceMap = voucherFacade.checkVoucherPrecedence(voucherCode);
		 *
		 * if (vouchePrecedenceMap != null && vouchePrecedenceMap.isEmpty()) { if
		 * (voucherFacade.redeemVoucher(voucherCode)) { GlobalMessages.addFlashMessage(redirectModel,
		 * GlobalMessages.CONF_MESSAGES_HOLDER, "voucher.code.applied.promo.released"); return voucherredirect();
		 *
		 * } else { GlobalMessages.addFlashMessage(redirectModel, GlobalMessages.ERROR_MESSAGES_HOLDER,
		 * "invalid.voucher.code"); } }
		 *
		 * } else { //voucher cannot be applied as it has less precedence than promo
		 * GlobalMessages.addFlashMessage(redirectModel, GlobalMessages.ERROR_MESSAGES_HOLDER, "promo.precedence.more"); }
		 *
		 * }
		 */

		return REDIRECT_PREFIX + "/cart";
		/*
		 * GlobalMessages.addFlashMessage(redirectModel, GlobalMessages.INFO_MESSAGES_HOLDER, "Voucher Applied"); return
		 * REDIRECT_PREFIX + ROOT;
		 */
	}

	/**
	 * @param cartModel
	 */
	private boolean checkIsPromotionInCart(final CartModel cartModel)
	{
		if (CollectionUtils.isNotEmpty(cartModel.getAllPromotionResults()))
		{
			for (final PromotionResultModel promotionResultModel : cartModel.getAllPromotionResults())
			{
				//if (promotionResultModel.getCertainty().equals(new Float("1.0")))
				if (promotionResultModel.getCertainty().floatValue() >= 1.0F)
				{
					return true;
				}
			}
		}
		return false;
	}

	@RequestMapping(value = "/removeVoucher", method = RequestMethod.GET)
	public String removeVoucher(@RequestHeader(value = "referer", required = false) final String referer,
			@RequestParam(required = false) final String voucherCode, final Model model, final HttpServletRequest request,
			final HttpServletResponse response, final RedirectAttributes redirectModel)
					throws CMSItemNotFoundException, CommerceCartModificationException
	{

		if (soVoucherService.removeVoucher())
		{
			GlobalMessages.addFlashMessage(redirectModel, GlobalMessages.CONF_MESSAGES_HOLDER, "voucher.released");
		}
		else
		{
			GlobalMessages.addFlashMessage(redirectModel, GlobalMessages.ERROR_MESSAGES_HOLDER, "voucher.release.error");
		}

		return REDIRECT_PREFIX + "/cart";
	}

	public String voucherredirect()
	{

		final CartModel sessionCart = cartService.getSessionCart();

		if (sessionCart.getEntries().isEmpty())
		{
			return REDIRECT_PREFIX + ROOT;
		}
		else
		{
			return REDIRECT_PREFIX + "/cart";
		}
	}

	protected void createProductList(final Model model) throws CMSItemNotFoundException
	{
		final CartData cartData = cartFacade.getSessionCartWithEntryOrdering(true);
		boolean hasPickUpCartEntries = false;
		if (cartData.getEntries() != null && !cartData.getEntries().isEmpty())
		{
			for (final OrderEntryData entry : cartData.getEntries())
			{
				if (!hasPickUpCartEntries && entry.getDeliveryPointOfService() != null)
				{
					hasPickUpCartEntries = true;
				}
				final UpdateQuantityForm uqf = new UpdateQuantityForm();
				uqf.setQuantity(entry.getQuantity());
				model.addAttribute("updateQuantityForm" + entry.getEntryNumber(), uqf);
			}
		}

		model.addAttribute("cartData", cartData);
		model.addAttribute("hasPickUpCartEntries", Boolean.valueOf(hasPickUpCartEntries));

		storeCmsPageInModel(model, getContentPageForLabelOrId(CART_CMS_PAGE_LABEL));
		setUpMetaDataForContentPage(model, getContentPageForLabelOrId(CART_CMS_PAGE_LABEL));

		model.addAttribute(WebConstants.BREADCRUMBS_KEY, resourceBreadcrumbBuilder.getBreadcrumbs("breadcrumb.cart"));
		model.addAttribute("pageType", PageType.CART.name());
	}

	protected void prepareDataForPage(final Model model) throws CMSItemNotFoundException
	{
		final String continueUrl = (String) sessionService.getAttribute(WebConstants.CONTINUE_URL);
		model.addAttribute(CONTINUE_URL, (continueUrl != null && !continueUrl.isEmpty()) ? continueUrl : ROOT);
		createProductList(model);
		model.addAttribute(new GuestForm());
		final CartRestorationData restorationData = (CartRestorationData) sessionService
				.getAttribute(WebConstants.CART_RESTORATION);
		model.addAttribute("restorationData", restorationData);
		model.addAttribute("isOmsEnabled", Boolean.valueOf(isOmsEnabled()));
		model.addAttribute("supportedCountries", cartFacade.getDeliveryCountries());
		final PrescriptionFormData prescriptionData = cartFacade.getPrescriptionData();
		final PrescriptionMajorsForm prescriptionForm = new PrescriptionMajorsForm();
		prescriptionForm.setAxisLeft(prescriptionData.getAxisLeft());
		prescriptionForm.setAxisRight(prescriptionData.getAxisRight());
		prescriptionForm.setBifocalADDLeft(prescriptionData.getBifocalADDLeft());
		prescriptionForm.setBifocalADDRight(prescriptionData.getBifocalADDRight());
		prescriptionForm.setCylinderLeft(prescriptionData.getCylinderLeft());
		prescriptionForm.setCylinderRight(prescriptionData.getCylinderRight());
		prescriptionForm.setPupilDistanceSingle(prescriptionData.getPupilDistanceSingle());
		prescriptionForm.setPupilDistanceSeperateLeft(prescriptionData.getPupilDistanceSeperateLeft());
		prescriptionForm.setPupilDistanceSeperateRight(prescriptionData.getPupilDistanceSeperateRight());
		prescriptionForm.setSphereLeft(prescriptionData.getSphereLeft());
		prescriptionForm.setSphereRight(prescriptionData.getSphereRight());
		if (prescriptionData.getPupilDistanceSingle() != null)
		{
			prescriptionForm.setPupilDistance(Boolean.TRUE);
		}
		else
		{
			prescriptionForm.setPupilDistance(Boolean.FALSE);
		}
		model.addAttribute("PrescriptionMajorsForm", prescriptionForm);

		final CartModel sessionCart = cartService.getSessionCart();
		model.addAttribute("PrescriptionMedia", sessionCart.getPrescriptionMedia());
	}

	protected boolean isOmsEnabled()
	{
		return getSiteConfigService().getBoolean("oms.enabled", false);
	}

	@SuppressWarnings("boxing")
	@ResponseBody
	@RequestMapping(value = "/verifyBeforeUpdateCart", method =
	{ RequestMethod.POST, RequestMethod.GET })
	public long orderConfirmation(@RequestParam("cartItems") final String cartItems)
	{
		final ProductData data = productFacade.getProductForCodeAndOptions(cartItems, Collections.singleton(ProductOption.STOCK));
		return data.getStock().getStockLevel();
	}

	@SuppressWarnings("deprecation")
	@RequestMapping(value = "/applyGiftCard", method = RequestMethod.GET)
	public String applyGiftCard(@RequestHeader(value = "referer", required = false) final String referer,
			@RequestParam(required = false) final String giftCardCode, final Model model, final HttpServletRequest request,
			final HttpServletResponse response, final RedirectAttributes redirectModel)
					throws CMSItemNotFoundException, CommerceCartModificationException
	{
		LOG.info("############################# Applying Gift Card : giftCardCode = " + giftCardCode
				+ " to the cart #############################");

		if (StringUtils.isEmpty(giftCardCode))
		{
			LOG.info("Gift Cart code is Empty. Please provide valide one.");
			model.addAttribute("invalidgiftcard", Boolean.TRUE);
			GlobalMessages.addFlashMessage(redirectModel, GlobalMessages.ERROR_MESSAGES_HOLDER, "invalid.giftcard.code");
			return REDIRECT_PREFIX + "/cart";
		}

		if (cartFacade.checkGiftCardCode(giftCardCode))
		{
			final CartModel cartModel = cartService.getSessionCart();
			final CustomerModel customerModel = (CustomerModel) userService.getCurrentUser();

			final GiftCardPaymentInfoModel giftCardPaymentInfoModel = cartFacade.getGCPaymentInfoModelForNumber(customerModel,
					giftCardCode);
			if (giftCardPaymentInfoModel != null)
			{
				if (cartModel.getTotalPrice().doubleValue() > 0)
				{
					if (giftCardPaymentInfoModel.getAvailableAmount().doubleValue() > 0)
					{
						try
						{
							cartFacade.updateGiftCardInfo(cartModel, giftCardCode, customerModel, giftCardPaymentInfoModel);
							commerceCartService.recalculateCart(cartModel);
						}
						catch (final CalculationException e)
						{
							LOG.error("Error while applying the gift card" + e.getMessage());
							GlobalMessages.addFlashMessage(redirectModel, GlobalMessages.ERROR_MESSAGES_HOLDER, "invalid.giftcard.code");
							return REDIRECT_PREFIX + "/cart";
						}
					}

					else
					{
						GlobalMessages.addFlashMessage(redirectModel, GlobalMessages.ERROR_MESSAGES_HOLDER,
								"insuffiecient.giftcard.amount");
						return REDIRECT_PREFIX + "/cart";
					}
				}
				else
				{
					GlobalMessages.addFlashMessage(redirectModel, GlobalMessages.ERROR_MESSAGES_HOLDER, "insuffiecient.cart.total");
					return REDIRECT_PREFIX + "/cart";
				}

				final GiftCardPaymentInfoModel gcPayment = cartModel.getGiftCardPaymentInfo();
				if (giftCardPaymentInfoModel.getNumber().equals(gcPayment.getNumber()))
				{
					giftCardPaymentInfoModel.setAvailableAmount(
							Double.valueOf(gcPayment.getAvailableAmount().doubleValue() - gcPayment.getRedeemedAmount().doubleValue()));
				}

				LOG.info("############################# Gift Card - " + giftCardCode
						+ " has been successfully applied to the cart. #############################");

				GlobalMessages.addFlashMessage(redirectModel, GlobalMessages.CONF_MESSAGES_HOLDER, "GiftCard applied");
				return voucherredirect();
			}
			else
			{
				GlobalMessages.addFlashMessage(redirectModel, GlobalMessages.ERROR_MESSAGES_HOLDER, "invalid.giftcard.code");
				return REDIRECT_PREFIX + "/cart";
			}
		}
		else
		{
			GlobalMessages.addFlashMessage(redirectModel, GlobalMessages.ERROR_MESSAGES_HOLDER, "invalid.giftcard.code");
			return REDIRECT_PREFIX + "/cart";
		}

	}

	@RequestMapping(value = "/releaseGiftCard", method = RequestMethod.GET)
	public String releaseGiftCard(@RequestParam(required = false) final String giftCardCode,
			final RedirectAttributes redirectModel) throws CMSItemNotFoundException, CommerceCartModificationException
	{
		LOG.info("############################# Releasing Gift Card : giftCardCode = " + giftCardCode
				+ " from the cart #############################");

		if (StringUtils.isEmpty(giftCardCode))
		{
			LOG.info("Invalid Gift Cart code. Please provide valide one.");
			GlobalMessages.addFlashMessage(redirectModel, GlobalMessages.ERROR_MESSAGES_HOLDER, "invalid.giftcard.code");
			return REDIRECT_PREFIX + "/cart";
		}

		if (cartService.hasSessionCart())
		{
			final CartModel cartModel = cartService.getSessionCart();

			if (soCartService.releaseGiftCard(cartModel, giftCardCode))
			{
				LOG.info("############################# Gift Card - " + giftCardCode
						+ " has been successfully released from the cart. #############################");
				GlobalMessages.addFlashMessage(redirectModel, GlobalMessages.CONF_MESSAGES_HOLDER, "giftcard.released");
				return REDIRECT_PREFIX + "/cart";
			}
			else
			{
				GlobalMessages.addFlashMessage(redirectModel, GlobalMessages.ERROR_MESSAGES_HOLDER, "giftcard.release.error");
			}
		}
		else
		{
			LOG.info("Missing or empty cart");
			GlobalMessages.addFlashMessage(redirectModel, GlobalMessages.ERROR_MESSAGES_HOLDER, "giftcard.release.error");
			return REDIRECT_PREFIX + "/cart";
		}

		return REDIRECT_PREFIX + "/cart";
	}
}
