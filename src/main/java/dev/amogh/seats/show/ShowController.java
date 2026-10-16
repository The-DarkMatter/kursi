package dev.amogh.seats.show;

import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import dev.amogh.seats.web.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;

@RestController
public class ShowController {

    private final ShowService shows;
    private final RateLimiter limiter;
    private final int createsPerWindow;
    private final Duration window;

    public ShowController(ShowService shows, RateLimiter limiter,
                          @Value("${app.demo.api-creates-per-window:20}") int createsPerWindow,
                          @Value("${app.demo.limit-window:10m}") Duration window) {
        this.shows = shows;
        this.limiter = limiter;
        this.createsPerWindow = createsPerWindow;
        this.window = window;
    }

    /**
     * Admin only (enforced in SecurityConfig). The demo admin secret is public so
     * anyone can run the burst against the live site, so creating shows is rate
     * limited per IP; one burst creates one show.
     */
    @PostMapping("/shows")
    @ResponseStatus(HttpStatus.CREATED)
    public ShowView create(@RequestBody CreateShowRequest request, HttpServletRequest http) {
        limiter.check("create-show", http.getRemoteAddr(), createsPerWindow, window);
        return shows.create(request);
    }

    /** Public listing; featured (the demo events) unless ?kind=api. */
    @GetMapping("/shows")
    public List<ShowSummary> list(@RequestParam(defaultValue = "featured") String kind,
                                  @RequestParam(defaultValue = "20") int limit) {
        return shows.list(kind, limit);
    }

    /** Compact live seat states for polling UIs (cached for 500 ms). */
    @GetMapping("/shows/{showId}/seatmap")
    public SeatMap seatMap(@PathVariable String showId) {
        return shows.seatMap(showId);
    }

    @GetMapping("/shows/{showId}")
    public ShowView get(@PathVariable String showId) {
        return shows.get(showId);
    }
}
