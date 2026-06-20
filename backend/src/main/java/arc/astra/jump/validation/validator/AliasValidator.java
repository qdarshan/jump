package arc.astra.jump.validation.validator;

import arc.astra.jump.validation.annotation.ValidAlias;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Set;
import java.util.regex.Pattern;

public class AliasValidator implements ConstraintValidator<ValidAlias, String> {

    private static final Pattern ALIAS_PATTERN =
            Pattern.compile("^[a-z0-9_-]{3,50}$");

    private static final Set<String> RESERVED =
            Set.of("api", "admin", "login", "logout");

    @Override
    public boolean isValid(String alias, ConstraintValidatorContext context) {

        if (alias == null || alias.isBlank()) {
            return true;
        }

        alias = alias.toLowerCase();
        // Normalization happens in the service; the lowercasing here is purely for validation

        if (!ALIAS_PATTERN.matcher(alias).matches()) {
            return false;
        }

        return !RESERVED.contains(alias);
    }
}
