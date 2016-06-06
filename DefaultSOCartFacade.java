/**
 *
 */
package com.so.facades.cart.impl;

import de.hybris.platform.cms2.servicelayer.services.CMSSiteService;
import de.hybris.platform.commercefacades.order.data.CartModificationData;
import de.hybris.platform.commercefacades.order.data.CartRestorationData;
import de.hybris.platform.commercefacades.order.data.PrescriptionFormData;
import de.hybris.platform.commercefacades.order.impl.DefaultCartFacade;
import de.hybris.platform.commerceservices.order.CommerceCartModification;
import de.hybris.platform.commerceservices.order.CommerceCartModificationException;
import de.hybris.platform.commerceservices.order.CommerceCartRestorationException;
import de.hybris.platform.core.model.order.AbstractOrderEntryModel;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.core.model.order.payment.GiftCardPaymentInfoModel;
import de.hybris.platform.core.model.product.ProductModel;
import de.hybris.platform.core.model.user.CustomerModel;
import de.hybris.platform.order.CartService;
import de.hybris.platform.servicelayer.media.MediaService;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.storelocator.model.PointOfServiceModel;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import javax.annotation.Resource;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile;

import com.so.core.model.NoChargePaymentInfoModel;
import com.so.core.model.PrescriptionMajorsModel;
import com.so.core.model.PrescriptionMediaModel;
import com.so.core.model.SORXCoatingVariantModel;
import com.so.core.model.SORXFrameVariantModel;
import com.so.core.model.SORXLensVariantModel;
import com.so.core.model.SOSunglassGiftCardVariantModel;
import com.so.core.model.SOSunglassVariantModel;
import com.so.facades.cart.SOCartFacade;



public class DefaultSOCartFacade extends DefaultCartFacade implements SOCartFacade
{
	private static final Logger logger = Logger.getLogger(DefaultSOCartFacade.class);

	@Resource
	private CMSSiteService cmsSiteService;

	@Autowired
	private CartService cartService;

	@Autowired
	private ModelService modelService;

	@Autowired
	private MediaService mediaService;

	/**
	 * This method is overridden, to add the product specific to current catalog version and to overcome ambiguous
	 * exception
	 */
	@Override
	public CartModificationData addToCart(final String code, final long quantity) throws CommerceCartModificationException
	{
		final ProductModel product = getProductService().getProductForCode(getCmsSiteService().getCurrentCatalogVersion(), code);
		final CartModel cartModel = getCartService().getSessionCart();

		final CommerceCartModification modification = getCommerceCartService().addToCart(cartModel, product, quantity,
				product.getUnit(), false);

		return getCartModificationConverter().convert(modification);
	}

	@Override
	public void mergeUserCarts()
	{
		final CustomerModel currentCustomer = (CustomerModel) getUserService().getCurrentUser();

		final Collection<CartModel> carts = currentCustomer.getCarts();
		for (final CartModel cartModel : carts)
		{
			if (!cartModel.getCode().equals(getSessionCart().getCode()))
			{
				final List<AbstractOrderEntryModel> entries = cartModel.getEntries();
				for (final AbstractOrderEntryModel abstractOrderEntryModel : entries)
				{
					try
					{
						if (!(abstractOrderEntryModel.getProduct() instanceof SORXFrameVariantModel
								|| abstractOrderEntryModel.getProduct() instanceof SORXLensVariantModel || abstractOrderEntryModel
									.getProduct() instanceof SORXCoatingVariantModel) && isRXProductInCart())
						{

							addToCart(abstractOrderEntryModel.getProduct().getCode(), abstractOrderEntryModel.getQuantity().intValue());
						}
					}
					catch (final CommerceCartModificationException e)
					{
						e.printStackTrace();
					}
				}

				modelService.remove(cartModel);
			}
		}
	}

	@Override
	public CartRestorationData restoreSavedCart(final String guid) throws CommerceCartRestorationException
	{
		if (hasSessionCart() && getSessionCart().getEntries().isEmpty())
		{
			getCartService().setSessionCart(null);
		}

		final CartModel cartModel = getCommerceCartService().getCartForGuidAndSiteAndUser(guid,
				getBaseSiteService().getCurrentBaseSite(), getUserService().getCurrentUser());
		clearPaymentDetailsFromCart(cartModel, (CustomerModel) getUserService().getCurrentUser());
		return getCartRestorationConverter().convert(getCommerceCartService().restoreCart(cartModel));
	}

	/**
	 * Removing the payment details from cart
	 * 
	 * @param userModel
	 */
	private void clearPaymentDetailsFromCart(final CartModel cartModel, final CustomerModel customerModel)
	{
		if (cartModel != null && cartModel.getPaymentInfo() != null)
		{
			modelService.remove(cartModel.getPaymentInfo());
			cartModel.setPaymentInfo(null);
			modelService.save(cartModel);
			modelService.refresh(cartModel);
		}

		if (customerModel != null && CollectionUtils.isNotEmpty(customerModel.getPaymentInfos()))
		{
			modelService.removeAll(customerModel.getPaymentInfos());
			customerModel.setPaymentInfos(Collections.EMPTY_LIST);
			modelService.save(customerModel);
			modelService.refresh(customerModel);
		}
	}

	/**
	 * This method is overridden, to add the product specific to current catalog version and to overcome ambiguous
	 * exception
	 */
	@Override
	public CartModificationData addToCart(final String code, final long quantity, final String storeId)
			throws CommerceCartModificationException
	{
		if (storeId == null)
		{
			return addToCart(code, quantity);
		}
		else
		{

			final ProductModel product = getProductService().getProductForCode(getCmsSiteService().getCurrentCatalogVersion(), code);

			final CartModel cartModel = getCartService().getSessionCart();

			final PointOfServiceModel pointOfServiceModel = getPointOfServiceService().getPointOfServiceForName(storeId);
			final CommerceCartModification modification = getCommerceCartService().addToCart(cartModel, product,
					pointOfServiceModel, quantity, product.getUnit(), false);

			return getCartModificationConverter().convert(modification);


		}
	}

	@Override
	public boolean isRXProductInCart()
	{
		final CartModel cart = getCartService().getSessionCart();
		final List<AbstractOrderEntryModel> entries = cart.getEntries();
		if (entries != null && !entries.isEmpty())
		{
			for (final AbstractOrderEntryModel entry : entries)
			{
				if (entry.getProduct() instanceof SORXLensVariantModel)
				{
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public boolean isAllowToAddToCart(final String productCode)
	{
		final ProductModel product = getProductService().getProductForCode(getCmsSiteService().getCurrentCatalogVersion(),
				productCode);

		final CartModel cart = getCartService().getSessionCart();
		final List<AbstractOrderEntryModel> entries = cart.getEntries();
		if (product instanceof SORXLensVariantModel)
		{
			if (entries != null && !entries.isEmpty())
			{
				return false;
			}
		}
		else
		{
			if (entries != null && !entries.isEmpty())
			{
				for (final AbstractOrderEntryModel entry : entries)
				{
					if (entry.getProduct() instanceof SORXLensVariantModel || entry.getProduct() instanceof SORXCoatingVariantModel)
					{
						return false;
					}
				}
			}
		}
		return true;
	}

	/* Method to save Prescription details */
	@Override
	public void savePrescriptionDetails(final PrescriptionFormData formData)
	{
		final CartModel cartModel = cartService.getSessionCart();
		PrescriptionMajorsModel prescriptionModel = new PrescriptionMajorsModel();
		prescriptionModel.setLeft(formData.getSphereLeft());
		prescriptionModel.setRight(formData.getSphereRight());
		cartModel.setSphere(prescriptionModel);
		prescriptionModel = new PrescriptionMajorsModel();
		prescriptionModel.setLeft(formData.getCylinderLeft());
		prescriptionModel.setRight(formData.getCylinderRight());
		cartModel.setCylinder(prescriptionModel);
		prescriptionModel = new PrescriptionMajorsModel();
		prescriptionModel.setLeft(formData.getAxisLeft());
		prescriptionModel.setRight(formData.getAxisRight());
		cartModel.setAxis(prescriptionModel);
		prescriptionModel = new PrescriptionMajorsModel();
		prescriptionModel.setLeft(formData.getBifocalADDLeft());
		prescriptionModel.setRight(formData.getBifocalADDRight());
		cartModel.setBifocalADD(prescriptionModel);
		prescriptionModel = new PrescriptionMajorsModel();
		prescriptionModel.setLeft(formData.getPupilDistanceSeperateLeft());
		prescriptionModel.setRight(formData.getPupilDistanceSeperateRight());
		cartModel.setPupilDistanceSeperate(prescriptionModel);
		cartModel.setPupilDistanceSingle(formData.getPupilDistanceSingle());
		modelService.save(cartModel);
		modelService.refresh(cartModel);
	}

	@Override
	public PrescriptionMediaModel savePrescriptionDocument(final MultipartFile file) throws IOException
	{

		final CartModel cartModel = cartService.getSessionCart();

		final String userId = cartModel.getUser().getUid();

		final PrescriptionMediaModel prescriptionMediaModel = modelService.create(PrescriptionMediaModel.class);
		prescriptionMediaModel.setCode(userId + "_prescriptionmedia_" + UUID.randomUUID());
		prescriptionMediaModel.setMime(file.getContentType());
		prescriptionMediaModel.setRealFileName(file.getOriginalFilename());
		modelService.save(prescriptionMediaModel);
		mediaService.setDataForMedia(prescriptionMediaModel, file.getBytes());

		cartModel.setPrescriptionMedia(prescriptionMediaModel);
		modelService.save(cartModel);
		modelService.refresh(cartModel);

		return prescriptionMediaModel;

	}

	@Override
	public void removePrescriptionDocument()
	{
		final CartModel cartModel = cartService.getSessionCart();
		cartModel.setPrescriptionMedia(null);
		modelService.save(cartModel);
		modelService.refresh(cartModel);
	}

	/*
	 * Method to fetch current prescription Data.
	 */
	@Override
	public PrescriptionFormData getPrescriptionData()
	{
		final CartModel cartModel = cartService.getSessionCart();
		PrescriptionMajorsModel prescriptionModel = new PrescriptionMajorsModel();
		final PrescriptionFormData prescriptionData = new PrescriptionFormData();
		prescriptionModel = cartModel.getAxis();
		if (prescriptionModel != null)
		{
			prescriptionData.setAxisLeft(prescriptionModel.getLeft());
			prescriptionData.setAxisRight(prescriptionModel.getRight());
		}
		prescriptionModel = cartModel.getCylinder();
		if (prescriptionModel != null)
		{
			prescriptionData.setCylinderLeft(prescriptionModel.getLeft());
			prescriptionData.setCylinderRight(prescriptionModel.getRight());
		}
		prescriptionModel = cartModel.getSphere();
		if (prescriptionModel != null)
		{
			prescriptionData.setSphereLeft(prescriptionModel.getLeft());
			prescriptionData.setSphereRight(prescriptionModel.getRight());
		}
		prescriptionModel = cartModel.getPupilDistanceSeperate();
		if (prescriptionModel != null)
		{
			prescriptionData.setPupilDistanceSeperateLeft(prescriptionModel.getLeft());
			prescriptionData.setPupilDistanceSeperateRight(prescriptionModel.getRight());
		}
		prescriptionModel = cartModel.getBifocalADD();
		if (prescriptionModel != null)
		{
			prescriptionData.setBifocalADDLeft(prescriptionModel.getLeft());
			prescriptionData.setBifocalADDRight(prescriptionModel.getRight());
		}
		prescriptionData.setPupilDistanceSingle(cartModel.getPupilDistanceSingle());
		return prescriptionData;
	}

	public CMSSiteService getCmsSiteService()
	{
		return cmsSiteService;
	}

	public void setCmsSiteService(final CMSSiteService cmsSiteService)
	{
		this.cmsSiteService = cmsSiteService;
	}

	public boolean setNoChargePaymentInfo()
	{
		final CartModel cartModel = cartService.getSessionCart();

		final NoChargePaymentInfoModel noChargePaymentInfo = new NoChargePaymentInfoModel();

		noChargePaymentInfo.setUser(getUserService().getCurrentUser());
		noChargePaymentInfo.setTitle("No Charge");
		noChargePaymentInfo.setDescription("description");

		cartModel.setPaymentInfo(noChargePaymentInfo);

		return true;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.so.facades.cart.SOCartFacade#checkGiftCardCode(java.lang.String)
	 */
	@Override
	public boolean checkGiftCardCode(final String giftCardCode)
	{
		// YTODO Auto-generated method stub
		if (StringUtils.isBlank(giftCardCode))
		{
			logger.info("Gift Cart code is Blank. It can't be blank. Please provide valide one.");
			return false;
		}

		final CustomerModel customerModel = (CustomerModel) getUserService().getCurrentUser();
		final CartModel cartModel = cartService.getSessionCart();

		if (checkIfApplicableToCurrentCustomer(customerModel, giftCardCode) && checkIfApplicableToCurrentCart(cartModel))
		{
			return true;
			//updateGiftCardInfo(cartModel, giftCardCode, customerModel);
		}
		else
		{
			return false;
		}

	}

	/**
	 * @param cartModel
	 * @param giftCardCode
	 * @param customerModel
	 */

	@Override
	public GiftCardPaymentInfoModel updateGiftCardInfo(final AbstractOrderModel abstractOrderModel, final String giftCardCode,
			final CustomerModel customerModel, final GiftCardPaymentInfoModel giftCardPaymentModel)
	{
		// YTODO Auto-generated method stub
		final GiftCardPaymentInfoModel giftCardPaymentInfoModel = modelService.create(GiftCardPaymentInfoModel.class);
		giftCardPaymentInfoModel.setOwner(abstractOrderModel);
		giftCardPaymentInfoModel.setNumber(giftCardCode);
		giftCardPaymentInfoModel.setCode(customerModel.getUid() + "_" + UUID.randomUUID());
		giftCardPaymentInfoModel.setUser(customerModel);
		giftCardPaymentInfoModel.setAvailableAmount(giftCardPaymentModel.getAvailableAmount());
		giftCardPaymentInfoModel.setSaved(false);
		abstractOrderModel.setIsGiftCardAvailable(Boolean.TRUE);
		abstractOrderModel.setGiftCardPaymentInfo(giftCardPaymentInfoModel);

		modelService.saveAll(new Object[]
		{ giftCardPaymentInfoModel, abstractOrderModel });

		modelService.refresh(customerModel);
		modelService.refresh(abstractOrderModel);

		logger.info("Gift Card with code: '" + giftCardCode + "' has been successfully added to the cart.");

		return giftCardPaymentInfoModel;
	}

	protected boolean checkIfApplicableToCurrentCustomer(final CustomerModel customerModel, final String giftCardCode)
	{
		logger.info("Validating if Gift Card is applicable to current customer or not.");
		if (customerModel != null && CollectionUtils.isNotEmpty(customerModel.getGiftCardPaymentInfo()))
		{
			for (final Iterator<GiftCardPaymentInfoModel> iterator = customerModel.getGiftCardPaymentInfo().iterator(); iterator
					.hasNext();)
			{
				final GiftCardPaymentInfoModel gcPaymentInfoModel = iterator.next();
				if (giftCardCode.equals(gcPaymentInfoModel.getNumber().toString()))
				{
					logger.info("Customer Validation is successful.");
					return true;
				}
			}
			logger.info("Customer Validation is un-successful.");
			return false;
		}
		else
		{
			logger.error("Customer Validation is failed.");
			return false;
		}
	}

	protected boolean checkIfApplicableToCurrentCart(final CartModel cartModel)
	{
		logger.info("Validating if Gift Card is applicable to current cart or not.");
		if (cartModel != null && cartModel.getGiftCardPaymentInfo() == null)
		{
			logger.info("Cart Validation is successful.");
			return true;
		}
		else
		{
			logger.error("Cart Validation is failed.");
			return false;
		}
	}

	@Override
	public GiftCardPaymentInfoModel getGCPaymentInfoModelForNumber(final CustomerModel customerModel, final String giftCardCode)
	{
		if (customerModel != null && CollectionUtils.isNotEmpty(customerModel.getGiftCardPaymentInfo()))
		{
			for (final Iterator<GiftCardPaymentInfoModel> iterator = customerModel.getGiftCardPaymentInfo().iterator(); iterator
					.hasNext();)
			{
				final GiftCardPaymentInfoModel gcPaymentInfoModel = iterator.next();
				if (giftCardCode.equals(gcPaymentInfoModel.getNumber().toString()))
				{
					logger.info("Found GiftCardPaymentInfoModel with giftCardCode : " + giftCardCode + " on " + customerModel.getUid());
					return gcPaymentInfoModel;
				}
			}
			logger.info("No GiftCardPaymentInfoModel found on Customer : " + customerModel.getUid() + " with giftCardCode -- "
					+ giftCardCode);
			return null;
		}
		return null;
	}

	@Override
	public boolean isNormalProductInCart()
	{
		final CartModel cart = getCartService().getSessionCart();
		final List<AbstractOrderEntryModel> entries = cart.getEntries();
		if (entries != null && !entries.isEmpty())
		{
			for (final AbstractOrderEntryModel entry : entries)
			{
				if (entry.getProduct() instanceof SOSunglassVariantModel)
				{
					return true;
				}
			}
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.so.facades.cart.SOCartFacade#isGiftCardProductInCart()
	 */
	@Override
	public boolean isGiftCardProductInCart()
	{
		final CartModel cart = getCartService().getSessionCart();
		final List<AbstractOrderEntryModel> entries = cart.getEntries();
		if (entries != null && !entries.isEmpty())
		{
			for (final AbstractOrderEntryModel entry : entries)
			{
				if (entry.getProduct() instanceof SOSunglassGiftCardVariantModel)
				{
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public boolean isNormProductInCart(final String productCode)
	{
		if (productCode != null && !productCode.isEmpty())
		{
			final ProductModel product = getProductService().getProductForCode(getCmsSiteService().getCurrentCatalogVersion(),
					productCode);

			final CartModel cart = getCartService().getSessionCart();

			final List<AbstractOrderEntryModel> entries = cart.getEntries();
			if (entries != null && !entries.isEmpty())
			{
				if (product instanceof SOSunglassGiftCardVariantModel)
				{
					return true;
				}
			}
			return false;
		}
		return false;
	}



}
