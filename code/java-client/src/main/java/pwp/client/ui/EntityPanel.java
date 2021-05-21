package pwp.client.ui;

import dev.jonium.mason.impl.SimpleMason;
import net.miginfocom.swing.MigLayout;
import pwp.client.Main;
import pwp.client.http.Client;
import pwp.client.path.Relations;
import pwp.client.utils.Tuple;
import pwp.client.utils.reflection.ReflectedField;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class EntityPanel extends JTabbedPane {

    private final JPanel display;
    private final JTextArea errorDisplay;
    private final JPanel entityDisplay;
    private final JPanel buttons;
    private final Consumer<Boolean> toggle;
    private final JButton edit;

    private final AtomicReference<SimpleMason<?>> value = new AtomicReference<>();
    private final AtomicReference<List<Tuple<ReflectedField, JTextField>>> fields = new AtomicReference<>();

    private final CountDownLatch init = new CountDownLatch(1);


    public EntityPanel() {
        display = new JPanel(new MigLayout("fill, hidemode 3"));
        errorDisplay = new JTextArea();
        errorDisplay.setEditable(false);
        errorDisplay.setVisible(false);
        edit = new JButton("Save");
        entityDisplay = new EntityDisplay(new MigLayout("fillx, wrap 2", "[40%][60%]"));
        var sp = new JScrollPane(entityDisplay);
        sp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        display.add(errorDisplay, "growx, pushx, wrap, hmax 100");
        display.add(sp, "grow, push, wrap");
        buttons = new JPanel(new MigLayout("fill, hidemode 3"));
        toggle = b -> {
            for (var c : buttons.getComponents()) {
                c.setEnabled(b);
            }
        };
        addTab("Display", display);
        setEnabled(false);
        revalidate();
        SwingUtilities.invokeLater(this::buildDisplay);
    }

    private void buildDisplay() {
        var clear = new JButton("clear");
        clear.addActionListener(e -> {
            toggle.accept(false);
            setEnabled(false);
            try {
                entityDisplay.removeAll();
                value.set(null);
                revalidate();
            } finally {
                toggle.accept(true);
            }
        });
        var refresh = new JButton("Refresh");
        refresh.addActionListener(e -> {
            toggle.accept(false);
            var o = value.get();
            if (o != null && o.getControls().containsKey(Relations.SELF.getValue())) {
                Client.getInstance().get(
                        o.getControls().get(Relations.SELF.getValue()).getHref(),
                        (om, v) -> (SimpleMason<?>) om.readValue(v, om.getTypeFactory().constructParametricType(
                                o.getClass(),
                                o.getWrapped().getClass()
                        ))
                ).handleAsync((newModel, t) -> {
                    if (t != null) {
                        Main.handleException(t);
                        error(t.getMessage());
                        toggle.accept(true);
                    } else {
                        newModel.ifPresent(this::showObject);
                    }
                    return null;
                }, SwingUtilities::invokeLater);
            } else {
                toggle.accept(true);
            }
        });
        edit.setVisible(false);
        edit.addActionListener(e -> {
            var o = value.get();
            if (o != null && o.getControls().containsKey(Relations.EDIT.getValue())) {
                toggle.accept(false);
                try {
                    var fields = this.fields.get();
                    if (fields != null) {
                        for (var tuple : fields) {
                            var text = Optional.ofNullable(tuple.getB().getText()).map(String::trim).orElse("");
                            if (!text.isEmpty()) {
                                if (Integer.class.isAssignableFrom(tuple.getA().getType())) {
                                    tuple.getA().set(o.getWrapped(), Integer.valueOf(text));
                                } else {
                                    tuple.getA().set(o.getWrapped(), text);
                                }
                            }
                        }
                    }
                    Client.getInstance().put(
                            o.getControls().get(Relations.EDIT.getValue()).getHref(),
                            o.getWrapped()
                    ).handleAsync((triple, t) -> {
                        try {
                            if (t != null) {
                                Main.handleException(t);
                                error(t.getMessage());
                            } else {
                                if (triple.getA() >= 400) {
                                    error(
                                            triple.getC()
                                                  .map(me -> me.getHttpStatusCode() + ": " + me.getMessage())
                                                  .orElse("Error in PUT operation")
                                    );
                                } else {
                                    info(triple.getA());
                                    ListPanel.reload(o.getWrapped().getClass());
                                }
                            }
                        } finally {
                            toggle.accept(true);
                        }
                        return false;
                    }, SwingUtilities::invokeLater);
                } catch (Exception ex) {
                    Main.handleException(ex);
                    error(ex.getMessage());
                    toggle.accept(true);
                }
            }
        });
        buttons.add(edit, "sgx 1, sgy 1");
        buttons.add(clear, "sgx 1, sgy 1");
        buttons.add(refresh, "sgx 1, sgy 1, pushx");
        display.add(buttons, "growx, pushx");
        toggle.accept(false);
        init.countDown();
    }

    private void error(String text) {
        errorDisplay.setVisible(true);
        errorDisplay.setText(
                "An error occurred during the last operation:\n"
                + text
        );
    }

    private void info(Integer code) {
        errorDisplay.setVisible(true);
        errorDisplay.setText("Response: " + code);
    }

    public void showObject(SimpleMason<?> o) {
        setEnabled(false);
        value.set(null);
        toggle.accept(false);
        edit.setVisible(false);
        errorDisplay.setVisible(false);
        this.fields.set(null);
        try {
            init.await();
        } catch (InterruptedException ignore) {
        }
        try {
            value.set(o);
            entityDisplay.removeAll();
            var fields = EntityDialog.modelToPanel(
                    o.getWrapped(),
                    entityDisplay,
                    false
            );
            if (o.getControls().containsKey(Relations.EDIT.getValue())) {
                edit.setVisible(true);
                fields.stream()
                      .filter(f -> !f.getA().isId() && !f.getA().isImmutable())
                      .map(Tuple::getB)
                      .forEach(tf -> tf.setEditable(true));
                this.fields.set(fields);
            }
            toggle.accept(true);
            setEnabled(true);
        } catch (Throwable t) {
            Main.handleException(t);
        }
        revalidate();
    }

    private static class EntityDisplay extends JPanel implements Scrollable {
        public EntityDisplay(LayoutManager layout) {
            super(layout);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return null;
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 0;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

}
