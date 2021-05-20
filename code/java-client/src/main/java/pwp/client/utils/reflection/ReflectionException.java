package pwp.client.utils.reflection;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class ReflectionException extends Exception {
    public ReflectionException(Throwable cause) {
        super(cause);
    }
}