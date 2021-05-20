package pwp.client.utils.reflection;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import pwp.client.Main;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ReflectionUtils {

    public static ReflectedField getIdField(Object o) throws ReflectionException {
        return streamAllFields(o).filter(ReflectedField::isId)
                                 .findFirst()
                                 .orElseThrow(ReflectionException::new);
    }

    public static Collection<ReflectedField> getUnmodifiableFields(Object o) throws ReflectionException {
        return streamAllFields(o).filter(ReflectedField::isImmutable)
                                 .collect(Collectors.toUnmodifiableSet());
    }

    public static Collection<ReflectedField> getModifiableFields(Object o) throws ReflectionException {
        return streamAllFields(o).filter(f -> !f.isId())
                                 .collect(Collectors.toUnmodifiableList());
    }

    public static Collection<ReflectedField> getAllFields(Object o) throws ReflectionException {
        return streamAllFields(o).collect(Collectors.toUnmodifiableList());
    }

    public static Stream<ReflectedField> streamAllFields(Object o) throws ReflectionException {
        try {
            var fields = o instanceof Class
                         ? Stream.of(((Class<?>) o).getDeclaredFields())
                         : Stream.of(o.getClass().getDeclaredFields());
            var superclass = o.getClass().getSuperclass();
            while (superclass != null) {
                if (superclass == Object.class) {
                    break;
                }
                fields = Stream.concat(fields, Arrays.stream(superclass.getDeclaredFields()));
                superclass = superclass.getSuperclass();
            }
            return fields.parallel().map(ReflectedField::new);
        } catch (Throwable t) {
            throw new ReflectionException(t);
        }
    }

    public static void set(Field f, Object o, Object value) throws ReflectionException {
        try {
            try {
                o.getClass().getMethod(
                        "set"
                        + f.getName().substring(0, 1).toUpperCase()
                        + f.getName().substring(1),
                        value.getClass()
                ).invoke(o, value);
            } catch (NoSuchMethodException e) {
                f.setAccessible(true);
                try {
                    f.set(o, value);
                } finally {
                    f.setAccessible(false);
                }
            }
        } catch (Throwable t) {
            throw new ReflectionException(t);
        }
    }

    public static void setSilent(Field f, Object o, Object value) {
        try {
            set(f, o, value);
        } catch (ReflectionException e) {
            Main.handleException(e);
        }
    }

}
