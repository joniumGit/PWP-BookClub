package pwp.client.ui;

import net.miginfocom.swing.MigLayout;

import javax.swing.*;

public class EntityPanel extends JTabbedPane {

    private final JPanel display;

    public EntityPanel() {
        display = new JPanel(new MigLayout("fill"));
        addTab("Display", display);
        setEnabled(false);
    }

    public void showObject(Object o) {
        setEnabled(true);
    }

}
