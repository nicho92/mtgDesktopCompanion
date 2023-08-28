package org.magic.api.shopping.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Map;

import org.apache.commons.text.StringEscapeUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.magic.api.beans.MagicCardStock;
import org.magic.api.beans.SealedStock;
import org.magic.api.beans.enums.EnumCondition;
import org.magic.api.beans.enums.EnumItems;
import org.magic.api.beans.shop.Transaction;
import org.magic.api.beans.technical.RetrievableTransaction;
import org.magic.api.interfaces.MTGCardsProvider;
import org.magic.api.interfaces.MTGSealedProvider;
import org.magic.api.interfaces.MTGStockItem;
import org.magic.api.interfaces.abstracts.AbstractMagicShopper;
import org.magic.services.AccountsManager;
import org.magic.services.CardsManagerService;
import org.magic.services.network.RequestBuilder;
import org.magic.services.network.URLTools;
import org.magic.services.tools.MTG;
import org.magic.services.tools.UITools;

public class MagicBazarShopper extends AbstractMagicShopper {


	String urlBase= "https://en.play-in.com";

	String urlListOrders = urlBase + "/user/list_order.php";
	String urlLogin = urlBase+"/user/signin.php";
	
	private void init()
	{
		
		if(client==null)
		{
		
			client = URLTools.newClient();
			
			Map<String, String> nvps = client.buildMap().put("_email", getAuthenticator().getLogin())
																 .put("_pwd", getAuthenticator().getPassword())
																 .put("_submit_login", "Me connecter").build();
			try {
				var res = client.doPost(urlLogin, nvps, null);
				
				logger.debug("{} connection response : {}",getName(),res.getStatusLine());
				
			} catch (IOException e) {
				logger.error(e);
			}
		}
	}
	
	@Override
	public List<RetrievableTransaction> listOrders() throws IOException {
		
		init();
		
		List<RetrievableTransaction> entries = new ArrayList<>();
		
		Document listOrders = URLTools.toHtml(client.toString(client.doGet(urlListOrders)));
		Elements e = listOrders.select("div.total_list a");

		for (Element element : e) {
			
			var o = new RetrievableTransaction();
			 	 o.setSource(getName());
				 o.setSourceId(element.select("div.num_commande").text());
				 o.setDateTransaction(UITools.parseDate(element.select("div.hide_mobile").first().html(),"MM/dd/yy"));
				 o.setTotalValue(UITools.parseDouble(StringEscapeUtils.unescapeHtml4(element.select("div.price").html())));
				 o.setComments(element.select("div.num_item").get(1).html());
				 o.setUrl(element.attr("href"));
			entries.add(o);

		}
		return entries;
	}
	
	@Override
	public Transaction getTransaction(RetrievableTransaction rt) throws IOException {
		
		var t = buildTransaction(rt);
						  
	    var doc= RequestBuilder.build().setClient(client).url(urlBase+rt.getUrl()).get().toHtml();
	   
	    Elements table = doc.select("div.table div.tr");
	    table.remove(0);
	      for (Element e : table) {
			boolean iscard=e.hasAttr("attribute_default");
			var elementName = e.select("div.td.name");
			if(!elementName.isEmpty())
			{
					if(iscard)
					{
						var b = buildCard(e,elementName);
						if(b!=null)
							t.getItems().add(b);
					}
					else
					{	
						var b = buildSealed(e,elementName);
						if(b!=null)
							t.getItems().add(b);
					}
					notify(t);
			}
		  }
		
		return t;
	}
	
	private MTGStockItem buildSealed(Element e, Elements elementName) {
		String name = elementName.first().text();
		var value = UITools.parseDouble(StringEscapeUtils.unescapeHtml4(e.select("div.price").html().trim()));
		var qty = Integer.parseInt(StringEscapeUtils.unescapeHtml4(e.select("div.qty").html().trim()));
		var productUrl = elementName.first().getElementsByTag("a").first().attr("href").replace("/produit/","");
		
		try {
		var st = new SealedStock();
			  st.setComment(name);
			  st.setPrice(value);
			  st.setQte(qty);
			  
			  st.getTiersAppIds().put(getName(), productUrl.substring(0,productUrl.indexOf('-')));
			  var ed = CardsManagerService.detectEdition(st.getComment());
			  
			  if(ed==null)
			  {
				  logger.warn("can't found ed product for {}",name);
				  return null;
			  }
			  else
			  {
				  	var typeProduct = EnumItems.SET;
					if (name.toLowerCase().contains("booster"))
						typeProduct = EnumItems.BOOSTER;
					
					if (name.toLowerCase().startsWith("boite de") || name.contains("Display"))
						typeProduct = EnumItems.BOX;
					
					if (name.toLowerCase().contains("Bundle"))
						typeProduct = EnumItems.BUNDLE;
					
					if(name.contains("VF") || name.contains("French") || name.contains("FR"))
						st.setLanguage("French");
					else
						st.setLanguage("English");
					
					
				  var products = MTG.getEnabledPlugin(MTGSealedProvider.class).get(ed, typeProduct);
				  		st.setProduct(products.get(0));
			  		
			  }
			  
			  return st;
		}
		catch(Exception ex)
		{
			logger.error(ex);
			return null;
		}
	}

	
	

	private MTGStockItem buildCard(Element e, Elements elementName) {
		String name = elementName.first().text();
		String set = e.select("div.td.ext img").attr("title");
		var productUrl = elementName.get(1).getElementsByTag("a").first().attr("href").replace("/magic/carte/","");
	
		try {
			var edition = MTG.getEnabledPlugin(MTGCardsProvider.class).getSetByName(set);
			var card = MTG.getEnabledPlugin(MTGCardsProvider.class).searchCardByName(name, edition, false).get(0);
			var langEtat = e.select("div.update_variation_sell").first().text().split(" - ");
			
			var st = new MagicCardStock(card);
				 st.setPrice(UITools.parseDouble(e.attr("attribute_price")));
				 st.setLanguage(langEtat[0].equalsIgnoreCase("Fr")?"French":"English");
				 st.setCondition(aliases.getReversedConditionFor(this, langEtat[1], EnumCondition.NEAR_MINT));
				 st.setQte(Integer.parseInt(e.select("div.qty").first().text()));
				 st.getTiersAppIds().put(getName(), productUrl);
			return st;
		}
		catch(Exception ex)
		{
			logger.error(ex);
			return null;
		}
	}

	@Override
	public String getName() {
		return "MagicBazar";
	}

	@Override
	public List<String> listAuthenticationAttributes() {
		return AccountsManager.generateLoginPasswordsKeys();
	}

	@Override
	protected Currency getCurrency() {
		return Currency.getInstance("EUR");
	}


}
