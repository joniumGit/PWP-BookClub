package pwp.client.ui;

import com.fasterxml.jackson.core.type.TypeReference;
import dev.jonium.mason.MasonControl;
import dev.jonium.mason.MasonError;
import net.miginfocom.swing.MigLayout;
import pwp.client.Main;
import pwp.client.http.Client;
import pwp.client.model.containers.Container;
import pwp.client.path.Relations;
import pwp.client.utils.Tuple;
import pwp.client.utils.reflection.ReflectionUtils;

import javax.swing.*;
import java.awt.*;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

public class ListPanel<T> extends JPanel {

    private final MasonControl control;
    private final DefaultListModel<Tuple<T, MasonControl>> model;
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

        var list = new JList<Tuple<T, MasonControl>>();
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
                    if (item.getB() != null) {
                        Client.getInstance().delete(item.getB().getHref()).handleAsync((v, t) -> {
                            if (t != null) {
                                Main.handleException(t);
                            } else {
                                Main.getLogger()
                                    .fine("Deleted: "
                                          + item.getA()
                                          + " Of class: "
                                          + new TypeReference<T>() {}.getType().getTypeName());
                            }
                            reloadData();
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
            if (!e.getValueIsAdjusting() && e.getFirstIndex() != -1) {
                try {
                    panel.showObject(model.get(e.getFirstIndex()));
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
                                                       reloadData().thenAcceptAsync(v -> buttonsEnabled.accept(true));
                                                   }
                                               }, SwingUtilities::invokeLater),
                            () -> reloadData().thenAcceptAsync(v -> buttonsEnabled.accept(true))
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
    public CompletableFuture<Void> reloadData() {
        return Client.getInstance().get(
                control.getHref(),
                (om, v) -> (Container<T>) om.readValue(v, getContainerClass())
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
                    model.addElement(
                            Tuple.of(
                                    wrapped,
                                    item.getControls().getOrDefault(Relations.SELF.getValue(), null)
                            )
                    );
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
            comp.setText(handle.apply(((Tuple<T, ?>) value).getA()));
            return comp;
        }
    }

}
