package org.magic.gui.renderer;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;

import org.magic.api.beans.MagicNews;
import org.magic.services.MTGConstants;

public class NewsTreeCellRenderer implements TreeCellRenderer {
	@Override
	public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded,boolean isLeaf, int row, boolean focused) {
		JLabel c = (JLabel)new DefaultTreeCellRenderer().getTreeCellRendererComponent(tree, value, selected, expanded, isLeaf, row, focused);

		if (((DefaultMutableTreeNode) value).getUserObject() instanceof MagicNews) {
			try {
				c.setIcon(((MagicNews) ((DefaultMutableTreeNode) value).getUserObject()).getProvider().getIcon());
			}
			catch(Exception e)
			{
				c.setIcon(null);
			}

		}

		if (((DefaultMutableTreeNode) value).getUserObject() instanceof String)
			c.setIcon(MTGConstants.ICON_NEWS);

		return c;
	}
}
