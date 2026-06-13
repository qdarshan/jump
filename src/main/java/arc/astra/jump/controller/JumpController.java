package arc.astra.jump.controller;

import arc.astra.jump.model.ShortenRequest;
import arc.astra.jump.model.ShortenResponse;
import arc.astra.jump.service.JumpService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
public class JumpController {

    private final JumpService jumpService;

    public JumpController(JumpService jumpService) {
        this.jumpService = jumpService;
    }

    @PostMapping("/shorten")
    public ResponseEntity<ShortenResponse> shorten(
            @RequestBody @Valid ShortenRequest shortenRequest,
            HttpServletRequest request) {
        String clientIp = request.getRemoteAddr();
        if (jumpService.isRateLimited(clientIp)){
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .build();
        }

        String code = jumpService.shortenUrl(shortenRequest.url());

        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/{code}")
                .buildAndExpand(code)
                .toUri();

        ShortenResponse response = new ShortenResponse(code, location.toString());

        return ResponseEntity.created(location)
                .body(response);
    }

    @GetMapping("/{code}")
    public ResponseEntity<Void> redirect(@PathVariable String code) {
        String url = jumpService.resolveUrl(code);

        if (url == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(url))
                .build();
    }


}
