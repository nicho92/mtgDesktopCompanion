package org.magic.gui.tools;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class JListFilterDecorator {
 private JPanel contentPanel;
 private JTextField filterField;

  public static <T> JListFilterDecorator decorate(JList<T> jList, BiPredicate<T, String> userFilter) {
      if (!(jList.getModel() instanceof DefaultListModel)) {
          throw new IllegalArgumentException("List model must be an instance of DefaultListModel");
      }
      DefaultListModel<T> model = (DefaultListModel<T>) jList.getModel();
      List<T> items = getItems(model);
      JTextField textField = new JTextField();
      textField.getDocument().addDocumentListener(new DocumentListener() {
          @Override
          public void insertUpdate(DocumentEvent e) {
              filter();
          }

          @Override
          public void removeUpdate(DocumentEvent e) {
              filter();
          }

          @Override
          public void changedUpdate(DocumentEvent e) {
              filter();
          }

          private void filter() {
              model.clear();
              String s = textField.getText();
              for (T item : items) {
                  if(userFilter.test(item, s)){
                      model.addElement(item);
                  }
              }
          }
      });

      JPanel panel = new JPanel(new BorderLayout());
      panel.add(textField, BorderLayout.SOUTH);
      JScrollPane pane = new JScrollPane(jList);
      panel.add(pane);


      JListFilterDecorator decorator = new JListFilterDecorator();
      decorator.contentPanel =panel;
      decorator.filterField = textField;
      return decorator;
  }

  private static <T> List<T> getItems(DefaultListModel<T> model) {
      List<T> list = new ArrayList<>();
      for (var i = 0; i < model.size(); i++) {
          list.add(model.elementAt(i));
      }
      return list;
  }

  public JTextField getFilterField() {
      return filterField;
  }

  public JPanel getContentPanel() {
      return contentPanel;
  }
}