package org.magic.api.dashboard.impl;

import static org.magic.tools.MTG.getEnabledPlugin;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.magic.api.beans.CardDominance;
import org.magic.api.beans.CardShake;
import org.magic.api.beans.EditionsShakers;
import org.magic.api.beans.HistoryPrice;
import org.magic.api.beans.MagicCard;
import org.magic.api.beans.MagicEdition;
import org.magic.api.beans.MagicFormat;
import org.magic.api.beans.Packaging;
import org.magic.api.interfaces.MTGCardsProvider;
import org.magic.api.interfaces.abstracts.AbstractDashBoard;
import org.magic.services.MTGConstants;
import org.magic.tools.UITools;
import org.magic.tools.URLTools;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class MTGPriceDashBoard extends AbstractDashBoard {
	private static final String WEBSITE = "WEBSITE";
	private Date updateTime;

	@Override
	public STATUT getStatut() {
		return STATUT.DEV;
	}

	
	@Override
	public List<CardShake> getOnlineShakerFor(MagicFormat.FORMATS f) throws IOException {
		List<CardShake> list = new ArrayList<>();
		String url = getString(WEBSITE) + "/taneLayout/mtg_price_tracker.jsp?period=" + getString("PERIOD");
		logger.debug("Get Shake for " + url);
		
		Document doc = URLTools.extractHtml(url);
		try {
			
			String date = doc.getElementsByClass("span6").get(1).text().replace("Updated:", "")
					.replace("UTC ", "").trim();
			SimpleDateFormat forma = new SimpleDateFormat("E MMMM dd hh:mm:ss yyyy", Locale.ENGLISH);
			updateTime = forma.parse(date);
		} catch (ParseException e1) {
			logger.error(e1);
		}
		String gameFormat= MagicFormat.toString(f);
		if (f == MagicFormat.FORMATS.LEGACY || f == MagicFormat.FORMATS.COMMANDER)
			gameFormat = "All";

		Element table = doc.getElementById("top50" + gameFormat);
		Element table2 = doc.getElementById("bottom50" + gameFormat);

		try {

			for (Element e : table.select(MTGConstants.HTML_TAG_TR)) {
				CardShake cs = new CardShake();
				cs.setName(e.getElementsByTag(MTGConstants.HTML_TAG_TD).get(0).text().trim());
				cs.setPrice(UITools.parseDouble(e.getElementsByTag(MTGConstants.HTML_TAG_TD).get(2).text().replaceAll("\\$", "")));
				cs.setPriceDayChange(UITools.parseDouble(e.getElementsByTag(MTGConstants.HTML_TAG_TD).get(4).text().replaceAll("\\$", "")));
				cs.setProviderName(getName());
				String set = e.getElementsByTag(MTGConstants.HTML_TAG_TD).get(1).text();
				set = set.replaceAll("_\\(Foil\\)", "");
				cs.setEd(getCodeForExt(set));
				list.add(cs);
				notify(cs);

			}

			for (Element e : table2.select(MTGConstants.HTML_TAG_TR)) {
				CardShake cs = new CardShake();
				cs.setName(e.getElementsByTag(MTGConstants.HTML_TAG_TD).get(0).text().trim());
				cs.setPrice(UITools.parseDouble(e.getElementsByTag(MTGConstants.HTML_TAG_TD).get(2).text().replaceAll("\\$", "")));
				cs.setPriceDayChange(UITools.parseDouble(e.getElementsByTag(MTGConstants.HTML_TAG_TD).get(4).text().replaceAll("\\$", "")));
				cs.setProviderName(getName());
				String set = e.getElementsByTag(MTGConstants.HTML_TAG_TD).get(1).text();
				set = set.replaceAll("_\\(Foil\\)", "");
				cs.setEd(getCodeForExt(set));
				list.add(cs);
				notify(cs);
			}
		} catch (Exception e) {
			logger.error("error retrieve cardshake for " + gameFormat, e);
		}
		return list;
	}

	private String getCodeForExt(String name) {
		try {
			for (MagicEdition ed : getEnabledPlugin(MTGCardsProvider.class).listEditions())
				if (ed.getSet().toUpperCase().contains(name.toUpperCase()))
					return ed.getId();
		} catch (Exception e) {
			logger.error(e);
		}

		return name;
	}

	@Override
	protected EditionsShakers getOnlineShakesForEdition(MagicEdition edition) throws IOException {

		String name = convert(edition.getSet()).replace(" ", "_");

		String url = getString(WEBSITE)+"/spoiler_lists/" + name;
		logger.debug("get Prices for " + name + " " + url);

		Document doc =URLTools.extractHtml(url);

		Element table = doc.getElementsByTag("body").get(0).getElementsByTag("script").get(2);

		EditionsShakers list = new EditionsShakers();
		list.setProviderName(getName());
		list.setEdition(edition);
		list.setDate(new Date());
		
		
		String data = table.html();
		data = data.substring(data.indexOf('['), data.indexOf(']') + 1);
		JsonElement root = URLTools.toJson(data);
		JsonArray arr = root.getAsJsonArray();
		for (var i = 0; i < arr.size(); i++) {
			JsonObject card = arr.get(i).getAsJsonObject();
			CardShake shake = new CardShake();
			shake.setProviderName(getName());
			shake.setName(card.get("name").getAsString());
			shake.setEd(edition.getId());
			shake.setPrice(card.get("fair_price").getAsDouble());
			try {
				shake.setPercentDayChange(card.get("percentageChangeSinceYesterday").getAsDouble());
			} catch (Exception e) {
				// do nothing
			}
			try {
				shake.setPercentWeekChange(card.get("percentageChangeSinceOneWeekAgo").getAsDouble());
			} catch (Exception e) {
				// do nothing
			}
			shake.setPriceDayChange(card.get("absoluteChangeSinceYesterday").getAsDouble());
			shake.setPriceWeekChange(card.get("absoluteChangeSinceOneWeekAgo").getAsDouble());
			notify(shake);
			list.addShake(shake);
		}

		return list;
	}

	private String convert(String name) {
		if (name.equalsIgnoreCase("Limited Edition Alpha"))
			return "Alpha";

		return name;
	}
	
	@Override
	protected HistoryPrice<MagicEdition> getOnlinePricesVariation(MagicEdition ed) throws IOException {
		return new HistoryPrice<>(ed);
	}
	

	@Override
	public HistoryPrice<MagicCard> getOnlinePricesVariation(MagicCard mc, boolean foil) throws IOException {

		HistoryPrice<MagicCard> historyPrice = new HistoryPrice<>(mc);
		historyPrice.setFoil(foil);
		String name = "";

		if (mc == null)
		{
			logger.error("no magiccard defined");
			return historyPrice;
		}

		name = mc.getName().replace(" ", "_");

		String edition = "";

		edition = mc.getCurrentSet().getSet();

		edition = edition.replace(" ", "_");

		String url = getString(WEBSITE)+"/sets/" + edition + "/" + name;
		Document d = URLTools.extractHtml(url);

		logger.debug("get Prices for " + name + " " + url);

		Element js = d.getElementsByTag("body").get(0).getElementsByTag("script").get(29);

		String html = js.html();
		html = html.substring(html.indexOf("[[") + 1, html.indexOf("]]") + 1);

		Pattern p = Pattern.compile("\\[(.*?)\\]");
		Matcher m = p.matcher(html);
		while (m.find()) {

			Date date = new Date(Long.parseLong(m.group(1).split(",")[0]));
			Double val = Double.parseDouble(m.group(1).split(",")[1]);

			historyPrice.put(date, val);
		}
		return historyPrice;
	}

	@Override
	public String getName() {
		return "MTGPrice";
	}

	@Override
	public Date getUpdatedDate() {
		return updateTime;
	}

	@Override
	public List<CardDominance> getBestCards(MagicFormat.FORMATS f, String filter) throws IOException {
		return new ArrayList<>();
	}

	

	@Override
	public void initDefault() {
		setProperty("PERIOD", "WEEKLY");
		setProperty(WEBSITE, "https://www.mtgprice.com");
	}

	@Override
	public String getVersion() {
		return "0.2";
	}


	@Override
	public HistoryPrice<Packaging> getOnlinePricesVariation(Packaging packaging) throws IOException {
		return new HistoryPrice<>(packaging);
	}
	

}
