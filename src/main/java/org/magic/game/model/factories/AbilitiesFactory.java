package org.magic.game.model.factories;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.magic.api.beans.MTGKeyWord.TYPE;
import org.magic.api.beans.MagicCard;
import org.magic.game.model.abilities.AbstractAbilities;
import org.magic.game.model.abilities.ActivatedAbilities;
import org.magic.game.model.abilities.LoyaltyAbilities;
import org.magic.game.model.abilities.StaticAbilities;
import org.magic.game.model.abilities.TriggeredAbilities;
import org.magic.game.model.abilities.TriggeredAbilities.KEYWORDS;
import org.magic.game.model.costs.LoyaltyCost;
import org.magic.services.MTGControler;
import org.magic.tools.CardsPatterns;

public class AbilitiesFactory implements Serializable{

	private static final long serialVersionUID = 1L;
	private static AbilitiesFactory inst;
	
	public static AbilitiesFactory getInstance()
	{
		
		if(inst==null)
			inst=new AbilitiesFactory();
		
		return inst;
	}
	
	
	private AbilitiesFactory() {
		
	}
	
	private List<String> listSentences(MagicCard mc)
	{
		List<String> arr = new ArrayList<>();
		for(String s :  mc.getText().split("\n"))
			arr.add(s);
		return arr;
	}
	
	public List<AbstractAbilities> getAbilities(MagicCard mc)
	{
		
		mc.setText(removeParenthesis(mc.getText()));
		List<AbstractAbilities> ret = new ArrayList<>();
		ret.addAll(getActivatedAbilities(mc));
		ret.addAll(getLoyaltyAbilities(mc));
		ret.addAll(getTriggeredAbility(mc));
		ret.addAll(parseStaticAbilities(mc));
		return ret;
	}
	
	private String removeParenthesis(String text)
	{
		return text.replaceAll(CardsPatterns.REMINDER.getPattern(),"");
	}
	
	
	public List<ActivatedAbilities> getActivatedAbilities(MagicCard mc) {
		List<ActivatedAbilities> ret = new ArrayList<>();
		if(!mc.isPlaneswalker())
		{
			for(String s : listSentences(mc))
			{
				int end = s.indexOf('.');
				
				if(s.indexOf(':')>1 && s.indexOf(':')<end)
				{
					
					String[] costs = s.substring(0,s.indexOf(':')).split(",");
					ActivatedAbilities abs = new ActivatedAbilities();
					abs.setCard(mc);
					for(String c : costs)
						abs.addCost(CostsFactory.getInstance().parseCosts(c.trim()));
					
					abs.addEffect(EffectsFactory.getInstance().parseEffect(mc,s.substring(s.indexOf(':')+1)));
					
					
					ret.add(abs);
					
				}
			}
		}
		return ret;
	}

	public List<LoyaltyAbilities> getLoyaltyAbilities(MagicCard mc) {
		
		List<LoyaltyAbilities> list = new ArrayList<>();
		if(mc.isPlaneswalker())
		{
			for(String s : listSentences(mc))
			{
				if(s.contains(":")) {
					LoyaltyAbilities abilities = new LoyaltyAbilities();
					abilities.setCard(mc);
					
						String subs = s.substring(0,s.indexOf(':')+1);
						if(subs.startsWith("+"))
						{
							try{
								abilities.setCost(new LoyaltyCost(Integer.parseInt(subs.substring(1,subs.indexOf(':')))));
							}
							catch(Exception e)
							{
								abilities.setCost(new LoyaltyCost("+"));
							}
						}
						else if(subs.startsWith("0"))
						{
							abilities.setCost(new LoyaltyCost(0));
						}
						else
						{
							try{
								abilities.setCost(new LoyaltyCost(Integer.parseInt("-"+subs.substring(1,subs.indexOf(':')))));
							}
							catch(Exception e)
							{
								abilities.setCost(new LoyaltyCost("-"));
							}	
						}
						
						abilities.addEffect(EffectsFactory.getInstance().parseEffect(mc,s.substring(subs.length())));
						list.add(abilities);
				}
			}
		}
		return list;
		
	}

	public List<StaticAbilities> parseStaticAbilities(MagicCard mc) {
		return MTGControler.getInstance().getKeyWordManager().getKeywordsFrom(mc, TYPE.WORD).stream().map(StaticAbilities::new).collect(Collectors.toList());
	}

	public List<TriggeredAbilities> getTriggeredAbility(MagicCard mc)
	{
		List<TriggeredAbilities> arr =new ArrayList<>();
		int i=0;
		for(String s : listSentences(mc))
		{
			for(KEYWORDS k : TriggeredAbilities.KEYWORDS.values())
			{
				if(s.split(" ")[0].equalsIgnoreCase(k.name()))
				{
					TriggeredAbilities t = new TriggeredAbilities();
									   t.setCard(mc);
									   t.setCost(null);
									   t.init(k,listSentences(mc).subList(i, listSentences(mc).size()));
									   
					arr.add(t);
				}
			}
			i++;
		}
		return arr;
	}

	
}
