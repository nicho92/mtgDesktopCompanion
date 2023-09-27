package org.magic.game.actions.turns;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JButton;

import org.magic.api.beans.game.GameManager;
import org.magic.api.beans.game.Turn;
import org.magic.game.gui.components.GamePanelGUI;

public class BlockPhase extends AbstractAction {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	public BlockPhase() {
		super("Block");
		putValue(SHORT_DESCRIPTION, "Block Phase");
		setEnabled(false);
	}

	@Override
	public void actionPerformed(ActionEvent ae) {
		GameManager.getInstance().getActualTurn().setCurrentPhase(Turn.PHASES.BLOCK);

		GamePanelGUI.getInstance().getTurnsPanel().disableButtonsTo((JButton) ae.getSource());
		setEnabled(false);
	}

}
