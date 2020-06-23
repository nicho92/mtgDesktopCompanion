package org.magic.api.exports.impl;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.MediaTracker;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.imageio.ImageIO;

import org.magic.api.beans.MagicCard;
import org.magic.api.beans.MagicDeck;
import org.magic.api.interfaces.MTGPictureProvider;
import org.magic.api.interfaces.abstracts.AbstractCardExport;
import org.magic.services.MTGConstants;
import org.magic.services.MTGControler;
import org.magic.services.MTGDeckManager;
import org.magic.sorters.TypesSorter;
import org.magic.tools.ImageTools;

public class ImageExporter extends AbstractCardExport{
	private static final String FORMAT = "FORMAT";
	
	int cardSpace = 25;
	int columnsCount = 5;
	int cardGroup = 4;
	int columnsSpace = 10;
	int cardWidthSize = 175;
	int headerSize=75;

	
	public BufferedImage generateImageFor(MagicDeck d)
	{
		List<MagicCard> cards =  d.getMainAsList();
		Collections.sort(cards, new Comparator<MagicCard>() {
			@Override
			public int compare(MagicCard o1, MagicCard o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});

		
		int suggestedNbLines = cards.size()/((cardGroup)*columnsCount);
		
		logger.debug(cards.size()+ " cards, by group of "+cardGroup + " and " + columnsCount + "columns = " + suggestedNbLines + " lines");
		
		BufferedImage tempPic = MTGControler.getInstance().getEnabled(MTGPictureProvider.class).getBackPicture();
		tempPic=ImageTools.scaleResize(tempPic,cardWidthSize);
		int  picHeight = suggestedNbLines * (tempPic.getHeight()+((cardGroup+1)*cardSpace))+headerSize;
		
		
		BufferedImage ret = new BufferedImage((cardWidthSize+columnsSpace)*columnsCount, picHeight, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = (Graphics2D) ret.getGraphics();
		
		
		int start = headerSize + 10;
		int ycard=start;
		int xcard=0;
		int cardCount = 0;
		int columnNumber=1;
		
		drawHeader(g,d,ret);
		
		for(MagicCard mc : cards) 
		{
			
			try {
				BufferedImage cardPic = MTGControler.getInstance().getEnabled(MTGPictureProvider.class).getPicture(mc);
				cardPic=ImageTools.scaleResize(cardPic,cardWidthSize);
			
				if(cardCount<(cardGroup))
				{
					g.drawImage(cardPic, null, xcard, ycard);
					
					ycard+=cardSpace;
					cardCount++;
					logger.debug(mc +" " + columnNumber +" " + xcard+"/"+ycard);
				}
				else
				{
					cardCount=0;
					columnNumber++;
					xcard=xcard+cardPic.getWidth()+columnsSpace;
					ycard=start;
					if(columnNumber==(columnsCount+1))
					{
						columnNumber=1;
						start = start + (cardPic.getHeight()+(cardGroup*cardSpace))+20;
						xcard=0;
						cardCount=0;
						logger.debug("new Line");
						
					}	
				}

				
				
				notify(mc);
				
			
			} catch (IOException e) {
				logger.error(e);
			}
			
		}
		
		g.dispose();
		return ret;
	}
	
	
	private void drawHeader(Graphics2D g, MagicDeck d, BufferedImage ret) {
		
		
		g.setColor(Color.ORANGE);
		g.fillRect(0, 0, ret.getWidth(),headerSize);
		try {
			g.drawImage(ImageIO.read(ImageExporter.class.getResource( "/icons/logo_src.png" )) , 10, 10,50,50, null);
		} catch (IOException e) {
			logger.error("error loading logo_src.png : "+e.getMessage());
		}
		
		g.setFont(MTGControler.getInstance().getFont().deriveFont((float)headerSize-30)); 
		g.setColor(Color.WHITE);
		g.drawString(d.getName(),70,headerSize-25);
	}


	public static void main(String[] args) throws IOException {
		new ImageExporter().exportDeck(new MTGDeckManager().getDeck("Temur Delver"), new File("d:/test.png"));
	}


	@Override
	public String getFileExtension() {
		return "."+getString(FORMAT);
	}


	@Override
	public void exportDeck(MagicDeck deck, File dest) throws IOException {
		ImageTools.saveImage(generateImageFor(deck), dest,getString(FORMAT));

	}


	@Override
	public MagicDeck importDeck(String f, String name) throws IOException {
		throw new IOException("Not Implemented");
	}

	
	@Override
	public MODS getMods() {
		return MODS.EXPORT;
	}

	@Override
	public String getName() {
		return "Image";
	}

	@Override
	public void initDefault() {
		setProperty(FORMAT, "png");
		setProperty("SORTER","ColorSorter"); 
		setProperty("COPY_CLIPBOARD","true");
	}
	
}
