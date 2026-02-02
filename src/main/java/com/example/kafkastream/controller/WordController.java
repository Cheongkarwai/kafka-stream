package com.example.kafkastream.controller;

import org.apache.kafka.streams.state.ReadOnlyWindowStore;
import org.apache.kafka.streams.state.WindowStoreIterator;
import org.springframework.cloud.stream.binder.kafka.streams.InteractiveQueryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/words")
public class WordController {

    private final KafkaTemplate<String, String> kafkaTemplate;

    private final InteractiveQueryService interactiveQueryService;

    public WordController(KafkaTemplate<String, String> kafkaTemplate,
                          InteractiveQueryService interactiveQueryService) {
        this.kafkaTemplate = kafkaTemplate;
        this.interactiveQueryService = interactiveQueryService;
    }

    @PostMapping
    public Mono<Void> send(@RequestBody String word) {
        return Mono.fromFuture(kafkaTemplate.send("word", word))
                .then();
    }

    @GetMapping("/{word}")
    public Mono<Map<String, Object>> countForWindow(
            @PathVariable String word,
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam("to")   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
    ) {
        return Mono.fromSupplier(() -> {
            ReadOnlyWindowStore<String, Long> store =
                    interactiveQueryService.getQueryableStore("WordCounts-1", org.apache.kafka.streams.state.QueryableStoreTypes.windowStore());

            Map<String, Long> results = new LinkedHashMap<>();
            try (WindowStoreIterator<Long> it = store.fetch(word, from, to)) {
                while (it.hasNext()) {
                    var next = it.next();
                    results.put(next.key.toString(), next.value); // key is window-start timestamp
                }
            }

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("word", word);
            response.put("from", from.toString());
            response.put("to", to.toString());
            response.put("countsByWindowStart", results);
            return response;
        });
    }


}
