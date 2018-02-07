package unit.providers;

import java.net.MalformedURLException;

import org.apache.log4j.Level;
import org.junit.Before;
import org.junit.Test;
import org.magic.api.beans.MagicCard;
import org.magic.api.beans.MagicEdition;
import org.magic.api.interfaces.MagicCardsProvider;
import org.magic.api.providers.impl.DeckbrewProvider;
import org.magic.api.providers.impl.MagicTheGatheringIOProvider;
import org.magic.api.providers.impl.MtgjsonProvider;
import org.magic.api.providers.impl.PrivateMTGSetProvider;
import org.magic.api.providers.impl.ScryFallProvider;
import org.magic.services.MTGLogger;

public class CardsProviderTests {

	MagicCard mc;
	MagicEdition ed;
	
	@Before
	public void removeCache()
	{
		MTGLogger.changeLevel(Level.ERROR);
	}

	
	@Before
	public void createCards()
	{
		mc = new MagicCard();
		mc.setName("Black Lotus");
		mc.setLayout("normal");
		mc.setCost("{0}");
		mc.setCmc(0);
		mc.getTypes().add("Artifact");
		mc.setReserved(true);
		mc.setText("{T}, Sacrifice Black Lotus: Add three mana of any one color to your mana pool.");
		mc.setRarity("Rare");
		mc.setArtist("Christopher Rush");
		mc.setMciNumber("232");
					 ed = new MagicEdition();
					 ed.setId("lea");
					 ed.setSet("Limited Edition Alpha");
					 ed.setBorder("Black");
					 ed.setRarity("Rare");
					 ed.setArtist("Christopher Rush");
					 ed.setMultiverse_id("3");
					 ed.setNumber("232");
		
		mc.getEditions().add(ed);
	}
	
	@Test
	public void initTests()
	{
		testProviders(new ScryFallProvider(),"b0faa7f2-b547-42c4-a810-839da50dadfe");
		testProviders(new MagicTheGatheringIOProvider(),"c944c7dc960c4832604973844edee2a1fdc82d98");
		testProviders(new DeckbrewProvider(),"c944c7dc960c4832604973844edee2a1fdc82d98");
		testProviders(new MtgjsonProvider(),"c944c7dc960c4832604973844edee2a1fdc82d98");
		testProviders(new PrivateMTGSetProvider(),"c944c7dc960c4832604973844edee2a1fdc82d98");
	}
	
	
	
	public void testProviders(MagicCardsProvider p,String id)
	{
		
			p.init();
			System.out.println("***********"+p);
			System.out.println("NAME "+p.getName());
			System.out.println("VERS "+p.getVersion());
			System.out.println("STAT "+p.getStatut());
			System.out.println("LANG "+p.getLanguages());
			System.out.println("QUER "+p.getQueryableAttributs());
			System.out.println("PROP "+p.getProperties());
			System.out.println("TYPE "+p.getType());
			System.out.println("ENAB "+p.isEnable());
			mc.setId(id);
			try {
				p.loadEditions();
				System.out.println("LOAD EDITION :OK");
			} catch (Exception e) {
				System.out.println("LOAD EDITION :ERROR " + e);
			}
			try {
				p.searchCardByCriteria("name", mc.getName(), ed, true);
				System.out.println("SEARCH CARD :OK");
			} catch (Exception e) {
				System.out.println("SEARCH CARD :ERROR " + e);
			}
			try {
				p.searchCardByCriteria("name", mc.getName(), null, false);
				System.out.println("SEARCH CARD :OK");
			} catch (Exception e) {
				System.out.println("SEARCH CARD :ERROR " + e);
			}
			try {
				p.getSetById(ed.getId());
				System.out.println("SET BY ID :OK");
			} catch (Exception e) {
				System.out.println("SET BY ID :ERROR " + e);
			}
			try {
				p.getCardById(mc.getId());
				System.out.println("CARD BY ID :OK");
			} catch (Exception e) {
				System.out.println("CARD BY ID :ERROR " + e);
			}
		
			try {
				System.out.println("WEB  "+p.getWebSite());
			} catch (MalformedURLException e) {
				System.out.println("WEB ERROR" + e);
			}
		
	}
	
	
}
