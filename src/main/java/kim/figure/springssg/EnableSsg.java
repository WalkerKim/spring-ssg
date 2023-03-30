package kim.figure.springssg;

import java.lang.annotation.*;

/**
 * The interface Enable ssg.
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EnableSsg {

    /**
     * Path variable param string [ ].
     *
     * @return the string [ ]
     */
    String[] pathVariableParam() default {};

    /**
     * Path variable bean repository class class.
     *
     * @return the class
     */
//TODO Have to make default interface
    Class<?> pathVariableBeanRepositoryClass() default PathRepository.class;

    /**
     * Gets path variable list method name.
     *
     * @return the path variable list method name
     */
    String getPathVariableListMethodName() default "";

    /**
     * Default param string string.
     *
     * @return the string
     */
    String defaultParamString() default "";


}
