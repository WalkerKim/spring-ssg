package kim.figure.springssg;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The interface Enable ssg all get method without path variable.
 *
 * @author : "DoHyeong Walker Kim"
 * */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface EnableSsgAllGetMethodWithoutPathVariable {

}
