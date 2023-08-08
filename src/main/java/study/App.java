package study;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;

import java.util.Arrays;
import java.util.Properties;

public class App {

    private static String kafkaHost = "localhost";
    private static String kafkaPort = "9092";
    private static String consumerGroupId = "word-count-consumer-group";

    public static void main(String[] args) {
        StreamsBuilder builder = new StreamsBuilder();

        KStream<String, String> textLines = builder.stream("TextLinesTopic");

        // TextLInesTopic의 데이터를 출력하는 코드
        textLines.foreach((key, value) ->
                System.out.println("Key: [" + key + "]  Value: [" + value + "]"));

        // 단어를 카운트하는 코드
        KTable<String, Long> wordCounts = textLines
                .flatMapValues(textLine -> Arrays.asList(textLine.toLowerCase().split("\\W+")))
                .groupBy((key, word) -> word)
                .count(Materialized.as("counts-store"));

        // 단어 카운트 결과를 출력하는 코드
        wordCounts.toStream().foreach((word, count) -> {
            System.out.println("Word: [" + word + "]  Count: [" + count + "]");
        });

        // 단어 카운트 결과를 Kafka WordsWithCountsTopic Topic에 전송하는 코드
        wordCounts.toStream().to("WordsWithCountsTopic", Produced.with(Serdes.String(), Serdes.Long()));

        KafkaStreams streams = new KafkaStreams(builder.build(), new StreamsConfig(getProperties()));
        streams.start();
    }

    private static Properties getProperties() {
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, consumerGroupId);
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaHost + ":" + kafkaPort);
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.consumerPrefix(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG), "earliest");

        return props;
    }
}
