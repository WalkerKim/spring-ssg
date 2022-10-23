package kim.figure.springssg;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EnableSsg {

    String[] pathVariableParam() default {};

    //TODO Have to make default interface
    Class<?> pathVariableBeanRepositoryClass() default PathRepository.class;

    String getPathVariableListMethodName() default "";

    String defaultParamString() default "";


}
