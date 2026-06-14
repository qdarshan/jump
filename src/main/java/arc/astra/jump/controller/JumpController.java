package arc.astra.jump.controller;

import arc.astra.jump.model.LeaderboardEntry;
import arc.astra.jump.model.Analytics;
import arc.astra.jump.model.CreateLinkRequest;
import arc.astra.jump.model.LinkResponse;
import arc.astra.jump.service.JumpService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
public class JumpController {

    private final JumpService jumpService;

    public JumpController(JumpService jumpService) {
        this.jumpService = jumpService;
    }

    @PostMapping("/shorten")
    public ResponseEntity<LinkResponse> shorten(
            @RequestBody @Valid CreateLinkRequest createLinkRequest) {
        LinkResponse response = jumpService.shortenUrl(createLinkRequest.url(), createLinkRequest.email());

        return ResponseEntity.created(response.shortUrl())
                .body(response);
    }

    @GetMapping("/{code}")
    public ResponseEntity<Void> redirect(@PathVariable String code, HttpServletRequest request) {
        String url = jumpService.resolveUrl(code, request.getRemoteAddr());

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(url))
                .build();
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<List<LeaderboardEntry>> getLeaderboard() {
        return ResponseEntity.ok(jumpService.getLeaderboard());
    }

    @GetMapping("/stats/{code}")
    public ResponseEntity<Analytics> metadata(@PathVariable String code) {
        return ResponseEntity.ok(jumpService.getStats(code));
    }

}
