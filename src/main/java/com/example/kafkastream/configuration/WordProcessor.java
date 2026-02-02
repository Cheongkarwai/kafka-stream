package com.example.kafkastream.configuration;

import com.example.kafkastream.dto.Word;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Date;
import java.util.function.Consumer;
import java.util.function.Function;

@Configuration
@Slf4j
public class WordProcessor {

    @Bean
    public Function<KStream<String, String>, KStream<String, Word>> countWord() {
        return kstream-> kstream
                .map((key, value) -> new KeyValue<>(value, value))
                .groupByKey(Grouped.with(Serdes.String(), Serdes.String()))
                .windowedBy(TimeWindows.of(Duration.ofMillis(10000)))
                .count(Materialized.as("WordCounts-1"))
                .toStream()
                .map((key, value) -> new KeyValue<>(null, new Word(key.key(), value, new Date(key.window().start()), new Date(key.window().end()))));
    }

    @Bean
    public Consumer<KStream<String, Word>> processWord() {
        return kStream -> kStream.foreach((key, word) -> log.info("Received message. Word: {} Count: {}", word.getWord(), word.getCount()));
    }
}
