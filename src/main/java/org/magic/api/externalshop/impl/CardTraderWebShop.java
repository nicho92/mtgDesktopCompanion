package org.magic.api.externalshop.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.api.cardtrader.modele.Categorie;
import org.api.cardtrader.services.CardTraderService;
import org.api.mkm.modele.Product.PRODUCT_ATTS;
import org.magic.api.beans.MagicEdition;
import org.magic.api.beans.enums.EnumItems;
import org.magic.api.beans.shop.Category;
import org.magic.api.beans.shop.Contact;
import org.magic.api.beans.shop.Transaction;
import org.magic.api.interfaces.MTGProduct;
import org.magic.api.interfaces.MTGStockItem;
import org.magic.api.interfaces.abstracts.AbstractExternalShop;
import org.magic.api.interfaces.abstracts.AbstractProduct;
import org.magic.api.interfaces.abstracts.AbstractStockItem;

public class CardTraderWebShop extends AbstractExternalShop {

	
	private static final String TOKEN = "TOKEN";
	private CardTraderService service;
	
	
	private void init()
	{
		try {
		if(service==null)
			service = new CardTraderService(getAuthenticator().get(TOKEN));
		}
		catch(Exception e)
		{
			logger.error("No authenticator "+e);
		}
	}
	
	@Override
	public List<MTGStockItem> loadStock(String search) throws IOException {
		return service.listStock(search).stream().map(mp->{
			var it = AbstractStockItem.generateDefault();
								    it.setId(mp.getId());
								    it.setAltered(mp.isAltered());
								    it.setComment("");
								    it.setFoil(mp.isFoil());
								    it.setSigned(mp.isSigned());
								    it.setLanguage(mp.getLanguage());
								    it.setQte(mp.getQty());
								    it.setPrice(mp.getPrice().getValue());
								var prod = AbstractProduct.createDefaultProduct();
								prod.setProductId(mp.getIdBlueprint());
								prod.setName(mp.getNameEn()); 
								prod.setEdition(toExpansion(mp.getExpansion()));
								prod.setCategory(toCategory(mp.getCategorie()));
								prod.setTypeProduct(prod.getName().contains("Booster")?EnumItems.SEALED:EnumItems.CARD);
								
								it.setProduct(prod);
			
								return (MTGStockItem)it;
		}).toList();
	}
	
	
	@Override
	public List<MTGProduct> listProducts(String name) throws IOException {
		
		init();
		return service.listBluePrintsByIds(null, name, null).stream().map(bp->{
			
			var product = AbstractProduct.createDefaultProduct();
				product.setName(bp.getName());
				product.setUrl(bp.getImageUrl());
				product.setProductId(bp.getId());
				product.setCategory(toCategory(bp.getCategorie()));				
				product.setEdition(toExpansion(bp.getExpansion()));
				
				
				notify(product);
				
			return product;
			
		}).toList();
	}

	private MagicEdition toExpansion(org.api.cardtrader.modele.Expansion expansion) {
		if(expansion==null)
			return null;
		var exp = new MagicEdition();
		exp.setId(expansion.getCode());
		exp.setSet(expansion.getName());
		return exp;
	}

	private Category toCategory(Categorie categorie) {
		if(categorie==null)
			return null;
		
		var cat = new Category();
		cat.setCategoryName(categorie.getName());
		cat.setIdCategory(categorie.getId());
		
		return cat;
	}

	@Override
	protected void createTransaction(Transaction t) throws IOException {
		throw new IOException("Can't create transation to " + getName());

	}

	@Override
	public int createProduct(MTGProduct t, Category c) throws IOException {
		throw new IOException("Can't create product to " + getName());
	}

	
	
	@Override
	public List<Category> listCategories() throws IOException {
		init();
		try {
		return service.listCategories().stream().map(this::toCategory).toList();
		}
		catch(Exception e)
		{
			return new ArrayList<>();
		}
	}

	@Override
	public String getName() {
		return "CardTrader";
	}

	@Override
	protected List<Transaction> loadTransaction() throws IOException {
		init();
		return service.listOrders(1).stream().map(o->{
			var trans = new Transaction();
			trans.setId(o.getId());
			trans.setDateSend(o.getDateSend());
			trans.setDatePayment(o.getDateCreditAddedToSeller());
			trans.setSourceShopName(getName());
			
			Contact c = new Contact();
					c.setName(o.getBuyer().getUsername());
					
					
					c.setAddress(o.getBillingAddress().getStreet());
					c.setZipCode(o.getBillingAddress().getZip());
					c.setCity(o.getBillingAddress().getCity());
					c.setCountry(o.getBillingAddress().getCountry());
					c.setEmail(o.getBuyer().getEmail());
					c.setTelephone(o.getBuyer().getPhone());	
					
			trans.setContact(c);
		
			return trans;
		}).toList();
	}
	

	@Override
	public List<String> listAuthenticationAttributes() {
		return List.of(TOKEN);
	}

	@Override
	public Integer saveOrUpdateContact(Contact c) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Contact getContactByEmail(String email) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int saveOrUpdateTransaction(Transaction t) throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public MTGStockItem getStockById(EnumItems typeStock, Integer id) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void saveOrUpdateStock(EnumItems typeStock, MTGStockItem stock) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<Contact> listContacts() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void deleteContact(Contact contact) throws IOException {
		// TODO Auto-generated method stub
		
	}
	

}
