package org.magic.services.workers;

import java.io.File;
import java.util.List;

import javax.swing.SwingWorker;

import org.apache.logging.log4j.Logger;
import org.magic.api.beans.MagicCard;
import org.magic.api.beans.MagicDeck;
import org.magic.api.beans.technical.MTGNotification;
import org.magic.api.beans.technical.MTGNotification.MESSAGE_TYPE;
import org.magic.api.interfaces.MTGCardsExport;
import org.magic.gui.abstracts.AbstractBuzyIndicatorComponent;
import org.magic.services.MTGControler;
import org.magic.services.logging.MTGLogger;
import org.utils.patterns.observer.Observable;
import org.utils.patterns.observer.Observer;

public class CardExportWorker extends SwingWorker<Void, MagicCard> {

	protected MTGCardsExport exp;
	protected Logger logger = MTGLogger.getLogger(this.getClass());
	protected Observer o;
	protected List<MagicCard> cards = null;
	protected AbstractBuzyIndicatorComponent buzy;
	private File f;
	private MagicDeck export;
	private Exception err;


	public CardExportWorker(MTGCardsExport exp,List<MagicCard> export,AbstractBuzyIndicatorComponent buzy,File f) {

		var d = new MagicDeck();
		d.setName("Export Alerts");
		export.stream().forEach(mc->d.getMain().put(mc, 1));



		init(exp,d,buzy,f);
	}


	public CardExportWorker(MTGCardsExport exp,MagicDeck export,AbstractBuzyIndicatorComponent buzy,File f) {
		init(exp,export,buzy,f);
	}

	public void init(MTGCardsExport exp,MagicDeck export,AbstractBuzyIndicatorComponent buzy,File f) {
		this.exp=exp;
		this.buzy=buzy;
		this.f=f;
		this.export=export;
		err=null;
		o=(Observable obs, Object c)->publish((MagicCard)c);
		exp.addObserver(o);
	}



	@Override
	protected Void doInBackground(){
		try {
			exp.exportDeck(export, f);
		} catch (Exception e) {
			err=e;
			logger.error("error export with {}",exp,e);
		}
		return null;
	}

	@Override
	protected void process(List<MagicCard> chunks) {
		buzy.progressSmooth(chunks.size());
	}

	@Override
	protected void done() {
		try {
			exp.removeObserver(o);
		} catch (Exception e) {
			logger.error(e);
		}
		buzy.end();

		if(err!=null)
		{
			MTGControler.getInstance().notify(err);
		}
		else
		{
			MTGControler.getInstance().notify(new MTGNotification(
					exp.getName() + " "+ MTGControler.getInstance().getLangService().get("FINISHED"),
					MTGControler.getInstance().getLangService().combine("EXPORT", "FINISHED"),
					MESSAGE_TYPE.INFO
					));
		}

	}

}
