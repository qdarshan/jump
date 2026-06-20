package arc.astra.jump.model;

import arc.astra.jump.validation.annotation.ValidAlias;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateLinkRequest(

        @Pattern(regexp = "https?://.+\\..+")
        @NotBlank(message = "Please provide a valid destination URL to shorten.")
        String url,

        @NotBlank(message = "An email address is required to generate a short link.")
        @Email(message = "Please provide a properly formatted email address (e.g., user@example.com).")
        String email,

        @ValidAlias
        String alias
) {
}
