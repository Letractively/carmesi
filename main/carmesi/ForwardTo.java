package carmesi;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * This annotation indicates that when the execution of the controller is finished, a forward must be made to the the specified url.
 *
 * @author Victor Hugo Herrera Maldonado
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ForwardTo {
    /**
     *  return The url to forward.
     */
     String value();
}
