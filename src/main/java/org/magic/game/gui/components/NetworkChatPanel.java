package org.magic.game.gui.components;

import static org.magic.services.tools.MTG.capitalize;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;

import org.jdesktop.swingx.JXTable;
import org.magic.api.beans.MTGNotification;
import org.magic.api.interfaces.MTGNetworkClient;
import org.magic.api.network.actions.SpeakAction;
import org.magic.api.network.impl.ActiveMQNetworkClient;
import org.magic.game.model.Player.STATUS;
import org.magic.gui.abstracts.MTGUIComponent;
import org.magic.gui.components.widgets.JLangLabel;
import org.magic.gui.models.PlayerTableModel;
import org.magic.services.MTGConstants;
import org.magic.services.MTGControler;
import org.magic.services.threads.MTGRunnable;
import org.magic.services.threads.ThreadManager;
import org.magic.services.tools.UITools;


public class NetworkChatPanel extends MTGUIComponent {

	private static final long serialVersionUID = 1L;
	private JTextField txtServer;
	private JXTable table;
	private transient MTGNetworkClient client;
	private PlayerTableModel mod;
	private JList<String> list = new JList<>(new DefaultListModel<>());
	private JButton btnConnect;
	private JButton btnLogout;
	private JTextArea editorPane;
	private JComboBox<STATUS> cboStates;
	private JButton btnColorChoose;
	private JButton btnSearch;



	public NetworkChatPanel() {
		setLayout(new BorderLayout(0, 0));

		btnLogout = new JButton(capitalize("LOGOUT"));
		var lblIp = new JLangLabel("HOST",true);
		btnConnect = new JButton(capitalize("CONNECT"));
		var panneauHaut = new JPanel();
		txtServer = new JTextField();
		mod = new PlayerTableModel();
		table = UITools.createNewTable(mod);
		var panneauBas = new JPanel();
		var panel = new JPanel();
		editorPane = new JTextArea();
		var panel1 = new JPanel();
		btnColorChoose = new JButton(MTGConstants.ICON_GAME_COLOR);
		cboStates = new JComboBox<>(new DefaultComboBoxModel<>(STATUS.values()));
		var panelChatBox = new JPanel();

		txtServer.setText("tcp://localhost:8081");
		txtServer.setColumns(10);
		btnLogout.setEnabled(false);
		panel.setLayout(new BorderLayout());
		panelChatBox.setLayout(new BorderLayout());
		editorPane.setText(capitalize("CHAT_INTRO_TEXT"));
		editorPane.setLineWrap(true);
		editorPane.setWrapStyleWord(true);
		editorPane.setRows(2);
		table.setRowHeight(100);
		btnSearch = new JButton("Search");

		try {
			editorPane.setForeground(new Color(Integer.parseInt(MTGControler.getInstance().get("/game/player-profil/foreground"))));
		} catch (Exception e) {
			editorPane.setForeground(Color.BLACK);
		}
//
//		list.setCellRenderer(new DefaultListCellRenderer() {
//			private static final long serialVersionUID = 1L;
//			@Override
//			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,boolean cellHasFocus) {
//				var label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected,cellHasFocus);
//				try {
//					label.setForeground(((SpeakAction) value).getColor());
//				} catch (Exception e) {
//					// do nothing
//				}
//				return label;
//			}
//		});

		add(panneauHaut, BorderLayout.NORTH);
		panneauHaut.add(lblIp);
		panneauHaut.add(txtServer);
		panneauHaut.add(btnConnect);
		panneauHaut.add(btnLogout);

		add(new JScrollPane(table), BorderLayout.CENTER);
		add(panneauBas, BorderLayout.SOUTH);
		add(panel, BorderLayout.EAST);
		panel.add(new JScrollPane(list), BorderLayout.CENTER);
		panel.add(panelChatBox, BorderLayout.SOUTH);

		panelChatBox.add(editorPane, BorderLayout.CENTER);
		panelChatBox.add(panel1, BorderLayout.NORTH);

		panel1.add(btnColorChoose);
		panel1.add(cboStates);
		panel1.add(btnSearch);

		initActions();
	}

	private void initActions() {

		
		btnConnect.addActionListener(ae -> {
			try {
				client = new ActiveMQNetworkClient();
				client.join(MTGControler.getInstance().getProfilPlayer(),  txtServer.getText());
				client.switchTopic("welcome");
				ThreadManager.getInstance().executeThread(new MTGRunnable() {

					@Override
					protected void auditedRun() {
						while (client.isActive()) {
							txtServer.setEnabled(!client.isActive());
							btnConnect.setEnabled(false);
							btnLogout.setEnabled(true);
						}
						txtServer.setEnabled(true);
						btnConnect.setEnabled(true);
						btnLogout.setEnabled(false);

					}

				},"alived connection listener");

			} catch (Exception e) {
				MTGControler.getInstance().notify(new MTGNotification(MTGControler.getInstance().getLangService().getError(),e));
			}
			
			var sw = new SwingWorker<Void, String>(){

				@Override
				protected Void doInBackground() throws Exception {
					while(client.isActive())
					{
						publish(client.consume());	
					}
					return null;
				}

				@Override
				protected void process(List<String> chunks) {
					 
					for(var s : chunks)
						((DefaultListModel<String>)list.getModel()).addElement(s);
				}
			};
			
			ThreadManager.getInstance().runInEdt(sw, "NetworkClient listening");
			
			
		});



		btnLogout.addActionListener(ae -> {
			try {
				client.logout();
			} catch (IOException e) {
				logger.error(e);
			}
		});

		btnColorChoose.addActionListener(ae -> {
			var c = JColorChooser.showDialog(null, "Choose Text Color", Color.BLACK);

			if(c!=null) {
				editorPane.setForeground(c);
				MTGControler.getInstance().setProperty("/game/player-profil/foreground", c.getRGB());
			}
		});

		editorPane.addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(FocusEvent fe) {
				if (editorPane.getText()
						.equals(capitalize("CHAT_INTRO_TEXT")))
					editorPane.setText("");
			}
		});

		editorPane.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(java.awt.event.KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					try {
						client.sendMessage(editorPane.getText(), editorPane.getForeground());
					} catch (IOException e1) {
						logger.error(e1);
					}
					editorPane.setText("");
				}

			}

		});


		cboStates.addItemListener(ie -> {
			if(ie.getStateChange()==ItemEvent.SELECTED)
				try {
					client.changeStatus((STATUS) cboStates.getSelectedItem());
				} catch (IOException e1) {
					logger.error(e1);
				}
		});

	}

	@Override
	public String getTitle() {
		return "Network";
	}


	@Override
	public ImageIcon getIcon() {
		return MTGConstants.ICON_TAB_CHAT;
	}



}


