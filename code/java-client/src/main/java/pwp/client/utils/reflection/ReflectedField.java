package pwp.client.utils.reflection;

import lombok.Getter;
import pwp.client.Main;
import pwp.client.utils.ID;
import pwp.client.utils.Immutable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Optional;

@Getter
public final class ReflectedField implements Member {
    private final Method setter;
    private final Method getter;
    private final Field field;

    public ReflectedField(Field field) {
        this.field = field;
        Method set;
        Method get;
        try {
            set = field.getDeclaringClass().getMethod(setterName(field), field.getType());
            get = field.getDeclaringClass().getMethod(getterName(field));
        } catch (Throwable t) {
            get = set = null;
        }
        setter = set;
        getter = get;
    }

    public boolean isId() {
        return field.getAnnotation(ID.class) != null;
    }

    public boolean isImmutable() {
        return field.getAnnotation(Immutable.class) != null;
    }

    public void set(Object object, Object value) {
        if (object == null) {
            return;
        }
        try {
            if (setter != null) {
                setter.invoke(object, value);
            } else {
                if (field.canAccess(this)) {
                    field.set(object, value);
                } else {
                    field.setAccessible(true);
                    try {
                        field.set(object, value);
                    } finally {
                        field.setAccessible(false);
                    }
                }
            }
        } catch (Throwable t) {
            Main.handleException(t);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(Object object) {
        if (object == null) {
            return Optional.empty();
        }
        try {
            if (getter != null) {
                return (Optional<T>) Optional.ofNullable(getter.invoke(object));
            } else {
                if (field.canAccess(object)) {
                    return Optional.ofNullable((T) field.get(object));
                } else {
                    field.setAccessible(true);
                    try {
                        return Optional.ofNullable((T) field.get(object));
                    } finally {
                        field.setAccessible(false);
                    }
                }
            }
        } catch (Throwable t) {
            Main.handleException(t);
        }
        return Optional.empty();
    }

    private static String setterName(Field f) {
        return "set"
               + f.getName().substring(0, 1).toUpperCase()
               + f.getName().substring(1);
    }

    private static String getterName(Field f) {
        return "get"
               + f.getName().substring(0, 1).toUpperCase()
               + f.getName().substring(1);
    }

    @Override
    public Class<?> getDeclaringClass() {
        return field.getDeclaringClass();
    }

    @Override
    public String getName() {
        return field.getName();
    }

    @Override
    public int getModifiers() {
        return field.getModifiers();
    }

    @Override
    public boolean isSynthetic() {
        return field.isSynthetic();
    }

    public Class<?> getType() {
        return field.getType();
    }

    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        return field.getAnnotation(annotationClass);
    }

}
