package arc.astra.jump.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ShortenRequest(

        @Pattern(regexp = "https?://.+\\..+")
        @NotBlank(message = "URL cannot be blank")
        String url
) {
}
