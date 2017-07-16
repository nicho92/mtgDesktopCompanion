package org.magic.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jdesktop.swingx.JXTable;
import org.magic.api.beans.EnumCondition;
import org.magic.api.beans.MagicCard;
import org.magic.api.beans.MagicCardStock;
import org.magic.gui.components.MagicCardDetailPanel;
import org.magic.gui.models.CardStockTableModel;
import org.magic.gui.renderer.MagicCardListRenderer;
import org.magic.gui.renderer.MagicDeckQtyEditor;
import org.magic.gui.renderer.MagicStockEditor;
import org.magic.gui.renderer.StockTableRenderer;
import org.magic.services.MTGControler;
import org.magic.services.ThreadManager;

import net.coderazzi.filters.gui.AutoChoices;
import net.coderazzi.filters.gui.TableFilterHeader;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.JLabel;
import org.magic.gui.components.editor.CardStockLinePanel;
import org.magic.api.beans.MagicCollection;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import javax.swing.JCheckBox;
import javax.swing.JSpinner;
import javax.swing.JTextPane;
import java.awt.Font;
import javax.swing.SpinnerNumberModel;

public class StockPanelGUI extends JPanel {
	private JXTable table;
	private CardStockTableModel model;
	private JTextField txtSearch;
	private DefaultListModel<MagicCard> resultListModel = new DefaultListModel<MagicCard>();
	private JList<MagicCard> listResult ;
	private JComboBox<String> cboAttributs ;
	private JButton btnSearch;

	private JButton btnAdd = new JButton();
	private JButton btnDelete = new JButton();
	private JButton btnSave = new JButton();
	
	private MagicCardDetailPanel magicCardDetailPanel;
	private JSplitPane splitPane;
    private TableFilterHeader filterHeader;

	private MagicCardStock selectedStock;
	private List<MagicCard> selectedCard;
	private JButton btnReload;
    
	static final Logger logger = LogManager.getLogger(StockPanelGUI.class.getName());
	private JLabel lblLoading;
	private JPanel rightPanel;
	private JLabel lblQte;
	private JLabel lblLanguage;
	private JLabel lblComment;
	private JSpinner spinner;
	private JComboBox<String> cboLanguages;
	private JTextPane textPane;
	private JComboBox<Boolean> cboFoil;
	private JLabel lblFoil;
	private JLabel lblSigned;
	private JLabel lblAltered;
	private JComboBox<Boolean> cboSigned;
	private JComboBox<Boolean> cboAltered;
	private JButton btnshowMassPanel;
	private JButton btnApplyModification;
	
	private static Boolean[] values = {null,true,false};
	private JLabel lblQuality;
	private JComboBox<EnumCondition> cboQuality;
	
	public StockPanelGUI() {
		logger.info("init StockManagment GUI");
		
		initGUI();
		
		listResult.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent lse) {
				
				selectedCard=listResult.getSelectedValuesList();
				if(selectedCard!=null)
				{
					btnAdd.setEnabled(true);
					magicCardDetailPanel.setMagicCard(selectedCard.get(0));
				}
				
				
			}
		});
		
		txtSearch.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				btnSearch.doClick();
			}
		});
		
		btnSearch.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {

				if (txtSearch.getText().equals(""))
					return;

				resultListModel.removeAllElements();

				ThreadManager.getInstance().execute(new Runnable() {
					public void run() {
						try {
							lblLoading.setVisible(true);
							String searchName = URLEncoder.encode(txtSearch.getText(), "UTF-8");
							List<MagicCard> cards = MTGControler.getInstance().getEnabledProviders().searchCardByCriteria(cboAttributs.getSelectedItem().toString(), searchName, null);
							for (MagicCard m : cards) 
									resultListModel.addElement(m);
							
							listResult.updateUI();
							lblLoading.setVisible(false);
						} catch (Exception e) {
							JOptionPane.showMessageDialog(null, e.getMessage(), "ERREUR", JOptionPane.ERROR_MESSAGE);
						}
					}
				}, "DeckSearchCards");
			}
		});
		
		btnSave.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				
				ThreadManager.getInstance().execute(new Runnable() {
					
					@Override
					public void run() {
						for(MagicCardStock ms : model.getList())
							if(ms.isUpdate())
								try {
									lblLoading.setVisible(true);
									MTGControler.getInstance().getEnabledDAO().saveOrUpdateStock(ms);
									ms.setUpdate(false);
									lblLoading.setVisible(false);
								} catch (SQLException e1) {
									JOptionPane.showMessageDialog(null, e1.getMessage(),"ERROR ON : " + String.valueOf(ms),JOptionPane.ERROR_MESSAGE);
									lblLoading.setVisible(false);
								}
						
						model.fireTableDataChanged();
						
					}
				}, "Batch stock save");
				
				
				
				
				
				
			}
			
		});
		
		btnAdd.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				
				for(MagicCard mc : selectedCard)
				{
					MagicCardStock ms = new MagicCardStock();
					ms.setIdstock(-1);
					ms.setUpdate(true);
					ms.setMagicCard(mc);
					model.add(ms);
				}
			}
		});
		
		table.getSelectionModel().addListSelectionListener(new ListSelectionListener(){
	        public void valueChanged(ListSelectionEvent event) {
	        	int viewRow = table.getSelectedRow();
	        	if(viewRow>-1)
	        	{
	        		int modelRow = table.convertRowIndexToModel(viewRow);
					selectedStock = (MagicCardStock)table.getModel().getValueAt(modelRow, 0);
					btnDelete.setEnabled(true);
					magicCardDetailPanel.setMagicCard(selectedStock.getMagicCard());
	        	}
	        }
	    });
		
		btnDelete.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
					try {
						int res = JOptionPane.showConfirmDialog(null, "Delete " + table.getSelectedRows().length + " item(s) ?","Confirm delete",JOptionPane.YES_NO_OPTION);
						if(res==JOptionPane.YES_OPTION)
						{
							
							for(int i : table.getSelectedRows())
							{
								MagicCardStock s = (MagicCardStock)table.getModel().getValueAt(table.getSelectedRows()[i], 0);
								//MagicCardStock s = (MagicCardStock)table.getModel().getValueAt(table.convertRowIndexToModel(i), 0);
								
								model.remove(s);
								if(s.getIdstock()>-1)
									MTGControler.getInstance().getEnabledDAO().deleteStock(selectedStock);
							}
						}
					} catch (SQLException e1) {
						JOptionPane.showMessageDialog(null, e1.getMessage(),"ERROR",JOptionPane.ERROR_MESSAGE);
					}
				}
				
			
		});
		
		btnReload.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				int res = JOptionPane.showConfirmDialog(null, "Cancel all changes ?","Confirm Undo",JOptionPane.YES_NO_OPTION);
				if(res==JOptionPane.YES_OPTION)
				{
					model.init();
				}
			}
		});

		
	}
	
	private void initGUI()
	{
		setLayout(new BorderLayout(0, 0));
		txtSearch = new JTextField();
		JPanel leftPanel = new JPanel();
		JScrollPane scrollList = new JScrollPane();
		JPanel searchPanel = new JPanel();
		
		model = new CardStockTableModel();
		
		listResult = new JList<MagicCard>(resultListModel);
		
		
		add(leftPanel, BorderLayout.WEST);
		leftPanel.setLayout(new BorderLayout(0, 0));
		leftPanel.add(scrollList, BorderLayout.CENTER);
		
	
		listResult.setCellRenderer(new MagicCardListRenderer());
		
		scrollList.setViewportView(listResult);
		
		leftPanel.add(searchPanel, BorderLayout.NORTH);
		
		String[] q = MTGControler.getInstance().getEnabledProviders().getQueryableAttributs();
		cboAttributs = new JComboBox<String>(new DefaultComboBoxModel<String>(q));
		searchPanel.add(cboAttributs);
	
		searchPanel.add(txtSearch);
		txtSearch.setColumns(10);
		
		btnSearch = new JButton("");
		
		btnSearch.setIcon(new ImageIcon(StockPanelGUI.class.getResource("/res/search.gif")));
		searchPanel.add(btnSearch);
		
		JPanel centerPanel = new JPanel();
		add(centerPanel, BorderLayout.CENTER);
		centerPanel.setLayout(new BorderLayout(0, 0));
		JPanel actionPanel = new JPanel();
		centerPanel.add(actionPanel, BorderLayout.NORTH);
				btnAdd.setEnabled(false);
		
				btnAdd.setIcon(new ImageIcon(StockPanelGUI.class.getResource("/res/new.png")));
				actionPanel.add(btnAdd);
				btnDelete.setEnabled(false);
				
	
				btnDelete.setIcon(new ImageIcon(StockPanelGUI.class.getResource("/res/delete.png")));
				actionPanel.add(btnDelete);
				btnSave.setToolTipText("Batch Save");
				
				
				btnSave.setIcon(new ImageIcon(StockPanelGUI.class.getResource("/res/save.png")));
				actionPanel.add(btnSave);
				
				btnReload = new JButton("");
				
				btnReload.setIcon(new ImageIcon(StockPanelGUI.class.getResource("/res/refresh.png")));
				actionPanel.add(btnReload);
				
				lblLoading = new JLabel("");
				lblLoading.setVisible(false);
				
				btnshowMassPanel = new JButton("");
				btnshowMassPanel.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						rightPanel.setVisible(!rightPanel.isVisible());
					}
				});
				btnshowMassPanel.setToolTipText("Mass Modification");
				btnshowMassPanel.setIcon(new ImageIcon(StockPanelGUI.class.getResource("/res/manual.png")));
				actionPanel.add(btnshowMassPanel);
				lblLoading.setIcon(new ImageIcon(StockPanelGUI.class.getResource("/res/load.gif")));
				actionPanel.add(lblLoading);
				
		JScrollPane scrollTable = new JScrollPane();
		
		table = new JXTable(model);
		StockTableRenderer render = new StockTableRenderer();
		
		table.setDefaultRenderer(Object.class,render);
		table.setDefaultEditor(EnumCondition.class, new MagicStockEditor());
		table.setDefaultEditor(Integer.class, new MagicDeckQtyEditor());
		
		table.packAll();
		filterHeader = new TableFilterHeader(table, AutoChoices.ENABLED);
		scrollTable.setViewportView(table);
		
		magicCardDetailPanel = new MagicCardDetailPanel();
		magicCardDetailPanel.enableThumbnail(true);
		
		splitPane = new JSplitPane();
		splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
		centerPanel.add(splitPane, BorderLayout.CENTER);
		splitPane.setLeftComponent(scrollTable);
		splitPane.setRightComponent(magicCardDetailPanel);
		
		rightPanel = new JPanel();
		rightPanel.setVisible(false);
		add(rightPanel, BorderLayout.EAST);
		GridBagLayout gbl_rightPanel = new GridBagLayout();
		gbl_rightPanel.columnWidths = new int[]{84, 103, 0};
		gbl_rightPanel.rowHeights = new int[]{83, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
		gbl_rightPanel.columnWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
		gbl_rightPanel.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		rightPanel.setLayout(gbl_rightPanel);
		
		lblQte = new JLabel("Qte : ");
		GridBagConstraints gbc_lblQte = new GridBagConstraints();
		gbc_lblQte.anchor = GridBagConstraints.EAST;
		gbc_lblQte.insets = new Insets(0, 0, 5, 5);
		gbc_lblQte.gridx = 0;
		gbc_lblQte.gridy = 1;
		rightPanel.add(lblQte, gbc_lblQte);
		
		spinner = new JSpinner();
		spinner.setModel(new SpinnerNumberModel(new Integer(0), new Integer(0), null, new Integer(1)));
		GridBagConstraints gbc_spinner = new GridBagConstraints();
		gbc_spinner.fill = GridBagConstraints.HORIZONTAL;
		gbc_spinner.insets = new Insets(0, 0, 5, 0);
		gbc_spinner.gridx = 1;
		gbc_spinner.gridy = 1;
		rightPanel.add(spinner, gbc_spinner);
		
		lblLanguage = new JLabel("Language :");
		GridBagConstraints gbc_lblLanguage = new GridBagConstraints();
		gbc_lblLanguage.anchor = GridBagConstraints.EAST;
		gbc_lblLanguage.insets = new Insets(0, 0, 5, 5);
		gbc_lblLanguage.gridx = 0;
		gbc_lblLanguage.gridy = 2;
		rightPanel.add(lblLanguage, gbc_lblLanguage);
		
		DefaultComboBoxModel lModel = new DefaultComboBoxModel();
		for(Locale l : Locale.getAvailableLocales())
			 lModel.addElement(l.getDisplayLanguage(Locale.US));
		
		cboLanguages = new JComboBox(lModel);
		GridBagConstraints gbc_cboLanguages = new GridBagConstraints();
		gbc_cboLanguages.insets = new Insets(0, 0, 5, 0);
		gbc_cboLanguages.fill = GridBagConstraints.HORIZONTAL;
		gbc_cboLanguages.gridx = 1;
		gbc_cboLanguages.gridy = 2;
		rightPanel.add(cboLanguages, gbc_cboLanguages);
		
		lblFoil = new JLabel("Foil :");
		GridBagConstraints gbc_lblFoil = new GridBagConstraints();
		gbc_lblFoil.anchor = GridBagConstraints.EAST;
		gbc_lblFoil.insets = new Insets(0, 0, 5, 5);
		gbc_lblFoil.gridx = 0;
		gbc_lblFoil.gridy = 3;
		rightPanel.add(lblFoil, gbc_lblFoil);
		
		cboFoil = new JComboBox(new DefaultComboBoxModel<Boolean>(values));
		GridBagConstraints gbc_cboFoil = new GridBagConstraints();
		gbc_cboFoil.insets = new Insets(0, 0, 5, 0);
		gbc_cboFoil.fill = GridBagConstraints.HORIZONTAL;
		gbc_cboFoil.gridx = 1;
		gbc_cboFoil.gridy = 3;
		rightPanel.add(cboFoil, gbc_cboFoil);
		
		lblSigned = new JLabel("Signed :");
		GridBagConstraints gbc_lblSigned = new GridBagConstraints();
		gbc_lblSigned.anchor = GridBagConstraints.EAST;
		gbc_lblSigned.insets = new Insets(0, 0, 5, 5);
		gbc_lblSigned.gridx = 0;
		gbc_lblSigned.gridy = 4;
		rightPanel.add(lblSigned, gbc_lblSigned);
		
		cboSigned = new JComboBox(new DefaultComboBoxModel<Boolean>(values));
		GridBagConstraints gbc_cboSigned = new GridBagConstraints();
		gbc_cboSigned.insets = new Insets(0, 0, 5, 0);
		gbc_cboSigned.fill = GridBagConstraints.HORIZONTAL;
		gbc_cboSigned.gridx = 1;
		gbc_cboSigned.gridy = 4;
		rightPanel.add(cboSigned, gbc_cboSigned);
		
		lblAltered = new JLabel("Altered :");
		GridBagConstraints gbc_lblAltered = new GridBagConstraints();
		gbc_lblAltered.anchor = GridBagConstraints.EAST;
		gbc_lblAltered.insets = new Insets(0, 0, 5, 5);
		gbc_lblAltered.gridx = 0;
		gbc_lblAltered.gridy = 5;
		rightPanel.add(lblAltered, gbc_lblAltered);
		
		cboAltered = new JComboBox(new DefaultComboBoxModel<Boolean>(values));
		GridBagConstraints gbc_cboAltered = new GridBagConstraints();
		gbc_cboAltered.insets = new Insets(0, 0, 5, 0);
		gbc_cboAltered.fill = GridBagConstraints.HORIZONTAL;
		gbc_cboAltered.gridx = 1;
		gbc_cboAltered.gridy = 5;
		rightPanel.add(cboAltered, gbc_cboAltered);
		
		lblQuality = new JLabel("Quality :");
		GridBagConstraints gbc_lblQuality = new GridBagConstraints();
		gbc_lblQuality.anchor = GridBagConstraints.EAST;
		gbc_lblQuality.insets = new Insets(0, 0, 5, 5);
		gbc_lblQuality.gridx = 0;
		gbc_lblQuality.gridy = 6;
		rightPanel.add(lblQuality, gbc_lblQuality);
		
		DefaultComboBoxModel qModel = new DefaultComboBoxModel();
		qModel.addElement(null);
		for(EnumCondition l : EnumCondition.values())
			 qModel.addElement(l);
		
		
		cboQuality = new JComboBox<EnumCondition>(qModel);
		
		
		
		GridBagConstraints gbc_cboQuality = new GridBagConstraints();
		gbc_cboQuality.insets = new Insets(0, 0, 5, 0);
		gbc_cboQuality.fill = GridBagConstraints.HORIZONTAL;
		gbc_cboQuality.gridx = 1;
		gbc_cboQuality.gridy = 6;
		rightPanel.add(cboQuality, gbc_cboQuality);
		
		lblComment = new JLabel("Comment :");
		GridBagConstraints gbc_lblComment = new GridBagConstraints();
		gbc_lblComment.insets = new Insets(0, 0, 5, 5);
		gbc_lblComment.gridx = 0;
		gbc_lblComment.gridy = 7;
		rightPanel.add(lblComment, gbc_lblComment);
		
		textPane = new JTextPane();
		GridBagConstraints gbc_textPane = new GridBagConstraints();
		gbc_textPane.insets = new Insets(0, 0, 5, 0);
		gbc_textPane.gridwidth = 2;
		gbc_textPane.gridheight = 2;
		gbc_textPane.fill = GridBagConstraints.BOTH;
		gbc_textPane.gridx = 0;
		gbc_textPane.gridy = 8;
		rightPanel.add(textPane, gbc_textPane);
		
		btnApplyModification = new JButton("Apply");
		btnApplyModification.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int res = JOptionPane.showConfirmDialog(null, "Change " + table.getSelectedRowCount() + " item(s)", "Confirmation", JOptionPane.YES_NO_CANCEL_OPTION);
				if(res==JOptionPane.YES_OPTION)
				{
					for(int i : table.getSelectedRows())
					{
						MagicCardStock s = (MagicCardStock)table.getModel().getValueAt(table.convertRowIndexToModel(i), 0);
						s.setUpdate(true);
						if(((Integer)spinner.getValue()).intValue()>0);
							s.setQte((Integer)spinner.getValue());
						if(!textPane.getText().equals(""))
							s.setComment(textPane.getText());
						if(cboAltered.getSelectedItem()!=null)
							s.setAltered((Boolean)cboAltered.getSelectedItem());
						if(cboSigned.getSelectedItem()!=null)
							s.setSigned((Boolean)cboSigned.getSelectedItem());
						if(cboFoil.getSelectedItem()!=null)
							s.setFoil((Boolean)cboFoil.getSelectedItem());
						if(cboLanguages!=null)
							s.setLanguage(String.valueOf(cboLanguages.getSelectedItem()));
						if(cboQuality.getSelectedItem()!=null)
							s.setCondition((EnumCondition)cboQuality.getSelectedItem());
						
						
					}
					model.fireTableDataChanged();
				}
				
				
			}
		});
		GridBagConstraints gbc_btnApplyModification = new GridBagConstraints();
		gbc_btnApplyModification.gridwidth = 2;
		gbc_btnApplyModification.gridx = 0;
		gbc_btnApplyModification.gridy = 11;
		rightPanel.add(btnApplyModification, gbc_btnApplyModification);
	}
	
	

	public static void main(String[] args) throws ClassNotFoundException, SQLException {
		JFrame f = new JFrame();
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		MTGControler.getInstance().getEnabledProviders().init();
		MTGControler.getInstance().getEnabledDAO().init();
		f.getContentPane().add(new StockPanelGUI());
		f.pack();
		f.setVisible(true);

	}

	
	
	
	
}
