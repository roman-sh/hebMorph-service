package com.hagar.hebmorph;

import com.code972.hebmorph.HebrewToken;
import com.code972.hebmorph.Lemmatizer;
import com.code972.hebmorph.hspell.HSpellDictionaryLoader;
import io.javalin.Javalin;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Api {

    private static Lemmatizer lemmatizer;

    public static void main(String[] args) {
        try {
            System.out.println("Loading HSpell dictionary from classpath...");
            lemmatizer = new Lemmatizer(new HSpellDictionaryLoader().loadDictionaryFromDefaultPath());
            System.out.println("Dictionary loaded successfully.");
        } catch (IOException e) {
            System.err.println("Failed to load HSpell dictionary from classpath. See error below:");
            e.printStackTrace();
            System.exit(1);
        }

        Javalin app = Javalin.create().start(5001);

        app.post("/lemmatize", ctx -> {
            LemmatizeRequest request = ctx.bodyAsClass(LemmatizeRequest.class);

            List<List<String>> results = request.getSentences().stream()
                .map(sentence -> 
                    Arrays.stream(sentence.split("\\s+"))
                        .map(word -> {
                            return lemmatizer.lemmatize(word).stream()
                                .map(HebrewToken::getLemma)
                                .filter(Objects::nonNull)
                                .filter(lemma -> lemma.length() > 1)
                                .findFirst()
                                .orElse(word);
                        })
                        .collect(Collectors.toList())
                )
                .collect(Collectors.toList());

            ctx.json(new LemmatizeResponse(results));
        });

        System.out.println("HebMorph service started on port 5001.");
    }

    public static class LemmatizeRequest {
        private List<String> sentences;
        public List<String> getSentences() { return sentences; }
        public void setSentences(List<String> sentences) { this.sentences = sentences; }
    }

    public static class LemmatizeResponse {
        private final List<List<String>> results;
        public LemmatizeResponse(List<List<String>> results) { this.results = results; }
        public List<List<String>> getResults() { return results; }
    }
} 