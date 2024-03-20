package br.com.helpc.app.controller;

import br.com.helpc.app.controller.dto.CreateTweetDto;
import br.com.helpc.app.controller.dto.FeedDto;
import br.com.helpc.app.controller.dto.FeedItemDto;
import br.com.helpc.app.entity.Tweet;
import br.com.helpc.app.entity.enums.RoleValues;
import br.com.helpc.app.repository.TweetRepository;
import br.com.helpc.app.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("api/v1/tweet")
public class TweetController {
    private final TweetRepository tweetRepository;
    private final UserRepository userRepository;

    public TweetController(TweetRepository tweetRepository,
                           UserRepository userRepository) {
        this.tweetRepository = tweetRepository;
        this.userRepository = userRepository;
    }
    @GetMapping("/feed")
    public ResponseEntity<FeedDto> feed(@RequestParam(value = "page", defaultValue = "0") int page,
                                        @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
                                        @RequestParam(value = "sort", defaultValue = "10") String sort) {

        var tweets = tweetRepository.findAll(
                        PageRequest.of(page, pageSize, Sort.Direction.fromString(sort), "creationTimestamp"))
                .map(tweet ->
                        new FeedItemDto(
                                tweet.getTweetId(),
                                tweet.getContent(),
                                tweet.getUser().getUsername())
                );

        return ResponseEntity.ok(new FeedDto(
                tweets.getContent(), page, pageSize, tweets.getTotalPages(), tweets.getTotalElements()));
    }

    @PostMapping("/tweets")
    public ResponseEntity<Void> createTweet(@RequestBody CreateTweetDto dto,
                                            JwtAuthenticationToken token) {
        userRepository.findById(UUID.fromString(token.getName()))
                .ifPresentOrElse(userPresent -> {
                    var tweet = new Tweet();
                    tweet.setUser(userPresent);
                    tweet.setContent(dto.content());
                    tweetRepository.save(tweet);
                }, () -> {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado");
                });

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/tweets/{id}")
    public ResponseEntity<Void> deleteTweet(@PathVariable("id") Long tweetId,
                                            JwtAuthenticationToken token) {
        userRepository.findById(UUID.fromString(token.getName()))
                .ifPresentOrElse(user -> {
                    var tweet = tweetRepository.findById(tweetId)
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

                    var isAdmin = user.getRoles()
                            .stream()
                            .anyMatch(role -> role.getName().equalsIgnoreCase(RoleValues.ADMIN.name()));

                    if (isAdmin || tweet.getUser().getUserId().equals(UUID.fromString(token.getName()))) {
                        tweetRepository.deleteById(tweetId);
                    } else {
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN);
                    }
                }, () -> {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado");
                });



        return ResponseEntity.ok().build();
    }
}
