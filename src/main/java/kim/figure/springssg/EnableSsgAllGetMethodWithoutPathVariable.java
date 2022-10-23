package kim.figure.springssg;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @Project : figure.kim
 * @Date : 2022-01-13
 * @Author : "DoHyeong Walker Kim"
 * @ChangeHistory :
 * @Note : If set this annotation on Controller if it's all get method will be export as static html file
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface EnableSsgAllGetMethodWithoutPathVariable {

}
