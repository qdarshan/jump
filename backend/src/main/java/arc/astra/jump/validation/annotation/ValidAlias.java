package arc.astra.jump.validation.annotation;

import arc.astra.jump.validation.validator.AliasValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = AliasValidator.class)
public @interface ValidAlias {
    String message() default "Alias must be 3-50 characters (a-z, 0-9, -, _) and not a reserved word";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
