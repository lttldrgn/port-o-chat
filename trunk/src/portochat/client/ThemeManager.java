/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package portochat.client;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.HashMap;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.Border;

/**
 *
 * @author Brandon
 */
public class ThemeManager extends JDialog implements ActionListener {
    private static final Logger logger = Logger.getLogger(ThemeManager.class.getName());
    public static final String TOP_PANE_BACKGROUND = "topPaneBackground";
    public static final String TOP_PANE_FOREGROUND = "topPaneForeground";
    private static final String APPLY = "APPLY";
    private static final String CANCEL = "CANCEL";
    private static final String OK = "OK";
    private static ThemeManager instance = null;
    private PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private ChatPane chatPane = null;
    private final String values[] = {TOP_PANE_BACKGROUND, TOP_PANE_FOREGROUND};
    private JComboBox themeChooser = new JComboBox();
    private JComboBox componentChooser = new JComboBox(values);
    private JButton ok = new JButton("OK");
    private JButton apply = new JButton("Apply");
    private JButton cancel = new JButton("Cancel");
    private HashMap<String, String> lookAndFeelMap = new HashMap<String, String>();
    private Component topLevelComponent;
    private LookAndFeel currentLAF;
    
    private ThemeManager() {
        super();
        setTitle("Settings");
    }
    
    public static synchronized ThemeManager getInstance() {
        if (instance == null) {
            instance = new ThemeManager();
            instance.init();
        }
        return instance;
    }
    
    private void init() {

        for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                System.out.println(info.getName());
                themeChooser.addItem(info.getName());
                lookAndFeelMap.put(info.getName(), info.getClassName());
        }

        setSize(800, 600);
        JPanel top = new JPanel();
        top.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        top.add(themeChooser,c);
        c.gridy++;
        //top.add(componentChooser,c);
        chatPane = ChatPane.createChatPane(null, "recipient", "yourname", false);
        add(top, BorderLayout.PAGE_START);
        Border border = BorderFactory.createTitledBorder("Example");
        chatPane.setBorder(border);
        
        add(chatPane, BorderLayout.CENTER);
        themeChooser.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {
                setLookAndFeel(ThemeManager.this);
            }
            
        });
        
        JPanel bottom = new JPanel();
        bottom.add(cancel);
        bottom.add(ok);
        bottom.add(apply);
        ok.addActionListener(this);
        ok.setActionCommand(OK);
        apply.addActionListener(this);
        apply.setActionCommand(APPLY);
        cancel.addActionListener(this);
        cancel.setActionCommand(CANCEL);
        add(bottom, BorderLayout.PAGE_END);
        
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                restorePreviousLookAndFeel();
            }
        });
    }

    @Override
    public void setVisible(boolean b) {
        super.setVisible(b);
        currentLAF = UIManager.getLookAndFeel();
        themeChooser.setSelectedItem(currentLAF.getName());
    }
    
    public void addThemeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }
    
    public void removeThemeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }
    
    @Override
    public void actionPerformed(ActionEvent event) {
        if (event.getActionCommand().equals(OK) ) {
            setLookAndFeel(topLevelComponent);
            setVisible(false);
            currentLAF = UIManager.getLookAndFeel();
        } else if (event.getActionCommand().equals(APPLY)) {
            setLookAndFeel(topLevelComponent);
            currentLAF = UIManager.getLookAndFeel();
        } else if (event.getActionCommand().equals(CANCEL)) {
            restorePreviousLookAndFeel();
            setVisible(false);
        }
    }

    private void restorePreviousLookAndFeel() {
        try {
            if (!UIManager.getLookAndFeel().equals(currentLAF)) {
                UIManager.setLookAndFeel(currentLAF);
                SwingUtilities.updateComponentTreeUI(this);
            }
        } catch (UnsupportedLookAndFeelException ex) {
            logger.severe("Unable to restore previous LAF: " + currentLAF.getName());
        }
    }
    
    private void setLookAndFeel(final Component topLevel) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {

                try {
                    String lookAndFeelName = (String) themeChooser.getSelectedItem();
                    UIManager.setLookAndFeel(lookAndFeelMap.get(lookAndFeelName));
                    SwingUtilities.updateComponentTreeUI(topLevel);

                } catch (Exception e) {

                }
            }
        });
    }
    
    public Component getTopLevelComponent() {
        return topLevelComponent;
    }

    /**
     * Sets the  top level component that the look and feel will be applied to.
     * This will usually be the main application frame.
     * @param topLevelComponent 
     */
    public void setTopLevelComponent(Component topLevelComponent) {
        this.topLevelComponent = topLevelComponent;
    }
}
