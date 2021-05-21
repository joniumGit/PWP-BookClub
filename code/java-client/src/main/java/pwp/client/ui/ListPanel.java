package pwp.client.ui;

import com.fasterxml.jackson.core.type.TypeReference;
import dev.jonium.mason.MasonControl;
import dev.jonium.mason.MasonError;
import dev.jonium.mason.impl.SimpleMason;
import net.miginfocom.swing.MigLayout;
import pwp.client.Main;
import pwp.client.http.Client;
import pwp.client.model.User;
import pwp.client.model.containers.Container;
import pwp.client.path.Relations;
import pwp.client.utils.reflection.ReflectionUtils;

import javax.swing.*;
import java.awt.*;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Objects;
import java.util.Optional;
import java.util.Vector;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import java.util.function.Function;

public class ListPanel<T> extends JPanel {

    private static final Vector<ListPanel<?>> instances = new Vector<>();

    public static void reload(Class<?> forClazz) {
        synchronized (instances) {
            if (User.class.isAssignableFrom(forClazz)) {
                instances.forEach(ListPanel::reloadData);
            } else {
                instances.stream().filter(lp -> Objects.equals(lp.modelClazz, forClazz)).forEach(ListPanel::reloadData);
            }
        }
    }

    private final MasonControl control;
    private final DefaultListModel<SimpleMason<T>> model;
    private final Function<T, String> handle;
    private final Class<T> modelClazz;
    private final Consumer<Boolean> buttonsEnabled;
    private final JPanel buttons;

    @SuppressWarnings("unchecked")
    public ListPanel(
            MasonControl control,
            EntityPanel panel,
            Class<T> clazz
    )
    {
        super(new MigLayout("fill, ins 0, gap 0, wrap 1"));
        instances.add(this);
        setEnabled(false);
        model = new DefaultListModel<>();
        buttons = new JPanel(new MigLayout("fill, rtl", "push[][]push"));

        try {
            modelClazz = clazz;
            var lu = MethodHandles.lookup();
            handle = (Function<T, String>) LambdaMetafactory.metafactory(
                    lu,
                    "apply",
                    MethodType.methodType(Function.class),
                    MethodType.methodType(Object.class, Object.class),
                    lu.unreflect(ReflectionUtils.getIdField(modelClazz).getGetter()),
                    MethodType.methodType(String.class, modelClazz)
            ).getTarget().invoke();
            Main.getLogger().fine(handle.toString());
        } catch (Throwable t) {
            throw new ExceptionInInitializerError(t);
        }

        var list = new JList<SimpleMason<T>>();
        var delete = new JButton("Delete");

        buttonsEnabled = b -> {
            for (var c : buttons.getComponents()) {
                c.setEnabled(b);
            }
        };

        buttons.add(delete, "sgx 1, sgy 1");
        buttons.setBackground(list.getBackground());

        add(list, "top, left, grow, push");
        add(buttons, "bottom, left, growx, pushx");

        delete.addActionListener(e -> {
            synchronized (model) {
                var ind = list.getSelectedIndex();
                if (ind != -1) {
                    var item = model.get(ind);
                    if (item != null) {
                        var d = item.getControls().get(Relations.DELETE.getValue());
                        if (d == null) {
                            return;
                        }
                        Client.getInstance().delete(d.getHref()).handleAsync((v, t) -> {
                            if (t != null) {
                                Main.handleException(t);
                            } else {
                                Main.getLogger()
                                    .fine("Deleted: "
                                          + item
                                          + " Of class: "
                                          + new TypeReference<T>() {}.getType().getTypeName());
                            }
                            reload(modelClazz);
                            return v;
                        });
                    } else {
                        model.removeElementAt(ind);
                    }
                }
            }
        });

        this.control = control;
        list.setCellRenderer(new Renderer());
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setModel(model);
        list.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && list.getSelectedIndex() != -1) {
                try {
                    panel.showObject(model.get(list.getSelectedIndex()));
                } catch (ArrayIndexOutOfBoundsException ignore) {

                }
            }
        });
        buttonsEnabled.accept(false);
        reloadData();
    }

    public void addUntilExit(MasonControl ctrl, T existing, MasonError error) {
        buttonsEnabled.accept(false);
        var opt = existing != null
                  ? EntityDialog.failed(existing, error)
                  : EntityDialog.show(null, modelClazz);
        opt.ifPresentOrElse(t -> Client.getInstance()
                                       .post(ctrl.getHref(), t)
                                       .thenAcceptAsync(
                                               triple -> {
                                                   if (triple.getC().isPresent()) {
                                                       addUntilExit(ctrl, t, triple.getC().get());
                                                   } else {
                                                       reload(modelClazz);
                                                   }
                                               }, SwingUtilities::invokeLater),
                            () -> reload(modelClazz)
        );
    }

    public Class<?> getContainerClass() {
        try {
            return Class.forName("pwp.client.model.containers." + modelClazz.getSimpleName() + "Container");
        } catch (Throwable t) {
            Main.handleException(t);
        }
        return Container.class;
    }

    @SuppressWarnings("unchecked")
    public void reloadData() {
        CompletableFuture.runAsync(
                () -> buttonsEnabled.accept(false),
                SwingUtilities::invokeLater
        ).thenComposeAsync(
                ignore -> Client.getInstance().get(
                        control.getHref(),
                        (om, v) -> (Container<T>) om.readValue(v, getContainerClass())
                ),
                ForkJoinPool.commonPool()
        ).thenAcceptAsync(opt -> opt.ifPresent(container -> {
            model.removeAllElements();
            if (buttons.getComponents().length == 1) {
                Optional.ofNullable(container.getControls().get(Relations.ADD.getValue())).ifPresent(ctrl -> {
                    var add = new JButton("Add");
                    add.setEnabled(false);
                    add.addActionListener(e -> addUntilExit(ctrl, null, null));
                    buttons.add(add, "sgx 1, sgy 1");
                    buttons.revalidate();
                });
            }
            for (var item : container.getItems()) {
                var wrapped = item.getWrapped();
                if (wrapped == null) {
                    continue;
                }
                try {
                    model.addElement(item);
                } catch (Exception e) {
                    Main.handleException(e);
                }
            }
        }), SwingUtilities::invokeLater).handleAsync((v, t) -> {
            if (t != null) {
                Main.handleException(t);
            }
            buttonsEnabled.accept(true);
            this.setEnabled(true);
            return v;
        }, SwingUtilities::invokeLater);
    }

    private class Renderer extends DefaultListCellRenderer {

        @SuppressWarnings("unchecked")
        @Override
        public Component getListCellRendererComponent(
                JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus
        )
        {
            var comp = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            comp.setText(handle.apply(((SimpleMason<T>) value).getWrapped()));
            return comp;
        }

    }

}
