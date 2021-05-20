package pwp.client.ui;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.jonium.mason.MasonError;
import dev.jonium.mason.impl.SimpleMasonError;
import jakarta.validation.constraints.NotNull;
import net.miginfocom.swing.MigLayout;
import pwp.client.Main;
import pwp.client.utils.Tuple;
import pwp.client.utils.reflection.ReflectedField;
import pwp.client.utils.reflection.ReflectionUtils;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EntityDialog {

    private EntityDialog() {
    }


    public static <T> Optional<T> show(T existing, Class<T> clazz) {
        return show(existing, clazz, false, "");
    }

    @SuppressWarnings("unchecked")
    public static <T> Optional<T> failed(@NotNull T model, MasonError message) {
        return show(model, (Class<T>) model.getClass(), true, message.getMessage());
    }

    public static <T> Optional<T> show(T existing, Class<T> clazz, boolean failed, String message) {
        var pne = new JPanel(new MigLayout("fill, ins 5, wrap 1", "[40%][60%]"));
        if (failed) {
            var text = new JTextArea("Entity was rejected by the server");
            text.setText(text.getText() + "\n" + message);
            pne.add(text, "spanx 2, center, top");
        }
        List<Tuple<ReflectedField, JTextField>> fields = new ArrayList<>();
        try {
            T model;
            if (existing == null) {
                var constructor = clazz.getConstructor();
                model = constructor.newInstance();
            } else {
                model = existing;
            }

            for (var field : ReflectionUtils.getAllFields(model)) {
                var type = field.getType();
                if (!String.class.isAssignableFrom(type)
                    && !Integer.class.isAssignableFrom(type))
                {
                    continue;
                }
                var textField = new JTextField();
                field.get(model).map(String::valueOf).ifPresent(textField::setText);
                var label = new JLabel();
                Optional.ofNullable(field.getAnnotation(JsonProperty.class)).ifPresentOrElse(
                        jp -> label.setText(jp.value()),
                        () -> label.setText(field.getName())
                );
                if (Integer.class.isAssignableFrom(type)) {
                    textField.setInputVerifier(new InputVerifier() {
                        @Override
                        public boolean verify(JComponent input) {
                            try {
                                Integer.valueOf(((JTextField) input).getText());
                            } catch (Exception e) {
                                return false;
                            }
                            return true;
                        }
                    });
                }
                fields.add(Tuple.of(field, textField));
                pne.add(label, "left, center, grow, push");
                pne.add(textField, "left, top, grow, push");
            }
            var option = JOptionPane.showConfirmDialog(
                    null,
                    pne,
                    "Create " + clazz.getSimpleName(),
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE,
                    null
            );
            if (option == JOptionPane.OK_OPTION) {
                boolean fail = false;
                List<String> messages = new ArrayList<>();

                for (var t : fields) {
                    var text = Optional.ofNullable(t.getB().getText()).map(String::trim).orElse("");
                    if (!text.isEmpty()) {
                        if (Integer.class.isAssignableFrom(t.getA().getType())) {
                            try {
                                t.getA().set(model, Integer.valueOf(text));
                            } catch (NumberFormatException e) {
                                fail = true;
                                messages.add(t.getA().getName());
                            }
                        } else {
                            t.getA().set(model, text);
                        }
                    }
                }

                if (fail) {
                    return failed(
                            model,
                            SimpleMasonError.builder().message(
                                    "Failed to Parse a number from:\n"
                                    + String.join("\n´-", messages)
                            ).build()
                    );
                }

                return Optional.of(model);
            }
        } catch (Throwable t) {
            Main.handleException(t);
        }
        return Optional.empty();
    }

}
