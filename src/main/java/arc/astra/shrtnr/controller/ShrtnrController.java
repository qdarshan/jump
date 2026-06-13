package arc.astra.shrtnr.controller;

import arc.astra.shrtnr.model.ShortenRequest;
import arc.astra.shrtnr.model.ShortenResponse;
import arc.astra.shrtnr.service.ShrtnrService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
public class ShrtnrController {

    private final ShrtnrService shrtnrService;

    public ShrtnrController(ShrtnrService shrtnrService) {
        this.shrtnrService = shrtnrService;
    }

    @PostMapping("/shorten")
    public ResponseEntity<ShortenResponse> shorten(@RequestBody ShortenRequest shortenRequest) {
        String code = shrtnrService.shortenURL(shortenRequest.url());

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
        String url = shrtnrService.redirectURL(code);

        if (url == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(url))
                .build();
    }


}
