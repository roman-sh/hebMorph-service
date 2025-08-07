package com.hagar.hebmorph;

import com.code972.hebmorph.HebrewToken;
import com.code972.hebmorph.Lemmatizer;
import com.code972.hebmorph.hspell.HSpellDictionaryLoader;
import io.javalin.Javalin;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Map;
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
                .map(Api::canonicalizeSentence)
                .collect(Collectors.toList());

            ctx.json(new LemmatizeResponse(results));
        });

        // Removed /lemmatize-all per spec

        // Canonical lemmatization: deterministically select one lemma per token with tie-break rules
        // Removed /lemmatize-canonical per spec

        // Raw endpoint: return HebMorph candidates per token for a single sentence (unfiltered)
        app.post("/lemmatize-raw", ctx -> {
            LemmatizeRawRequest request = ctx.bodyAsClass(LemmatizeRawRequest.class);
            String sentence = request.getSentence();
            if (sentence == null) sentence = "";

            List<List<Candidate>> results = Arrays.stream(sentence.split("\\s+"))
                .map(word -> lemmatizer.lemmatize(word).stream()
                    // no filtering/sorting; expose raw order as provided by HebMorph
                    .map(t -> new Candidate(
                        t.getLemma(),
                        t.getScore(),
                        t.getMask() != null ? t.getMask().name() : null,
                        (int) t.getPrefixLength()
                    ))
                    .collect(Collectors.toList())
                )
                .collect(Collectors.toList());

            ctx.json(new LemmatizeRawResponse(results));
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

    public static class LemmatizeRawResponse {
        private final List<List<Candidate>> results;
        public LemmatizeRawResponse(List<List<Candidate>> results) { this.results = results; }
        public List<List<Candidate>> getResults() { return results; }
    }

    public static class LemmatizeRawRequest {
        private String sentence;
        public String getSentence() { return sentence; }
        public void setSentence(String sentence) { this.sentence = sentence; }
    }

    public static class Candidate {
        public String lemma;
        public float score;
        public String mask;
        public int prefixLength;

        public Candidate(String lemma, float score, String mask, int prefixLength) {
            this.lemma = lemma;
            this.score = score;
            this.mask = mask;
            this.prefixLength = prefixLength;
        }
    }

    // Utilities
    private static String stripEdgePunct(String s) {
        if (s == null) return null;
        // Trim any leading/trailing chars that are not letters or digits (covers punctuation and symbols)
        return s.replaceAll("^[^\\p{L}\\p{N}]+|[^\\p{L}\\p{N}]+$", "");
    }

    private static String cleanTokenCore(String s) {
        String trimmed = stripEdgePunct(s);
        if (trimmed == null) return null;
        // Remove inner quotes/gershayim/geresh characters entirely to normalize units like ק"ג / ק״ג → קג
        return trimmed.replaceAll("[\\\"'׳״]", "");
    }

    private static int posPriorityGeneral(HebrewToken t) {
        if (t == null || t.getMask() == null) return 0;
        switch (t.getMask()) {
            case D_ADJ: return 3;
            case D_NOUN: return 2;
            case D_VERB: return 1;
            default: return 0;
        }
    }

    private static int posPriorityForWord(String normalized, HebrewToken t) {
        if (t == null || t.getMask() == null) return 0;
        boolean endsWithYod = normalized.endsWith("י") || normalized.endsWith("ית");
        boolean endsWithHeh = normalized.endsWith("ה");
        switch (t.getMask()) {
            case D_ADJ:
                return endsWithYod ? 3 : (endsWithHeh ? 2 : 3);
            case D_NOUN:
                return endsWithHeh ? 3 : (endsWithYod ? 2 : 2);
            case D_VERB:
                return 1;
            default:
                return 0;
        }
    }

    private static List<String> canonicalizeSentence(String sentence) {
        return Arrays.stream(sentence.split("\\s+"))
            .map(word -> {
                String normalized = cleanTokenCore(word);
                if (normalized == null || normalized.isEmpty()) return null; // drop pure punct
                List<HebrewToken> tokens = lemmatizer.lemmatize(normalized);
                if (tokens == null || tokens.isEmpty()) return normalized;
                HebrewToken best = tokens.stream()
                    .filter(t -> t.getLemma() != null && t.getLemma().length() > 1)
                    .sorted((a, b) -> {
                        int byScore = Float.compare(b.getScore(), a.getScore());
                        if (byScore != 0) return byScore;
                        int byPosSpecial = Integer.compare(posPriorityForWord(normalized, b), posPriorityForWord(normalized, a));
                        if (byPosSpecial != 0) return byPosSpecial;
                        int byPosGeneral = Integer.compare(posPriorityGeneral(b), posPriorityGeneral(a));
                        if (byPosGeneral != 0) return byPosGeneral;
                        int bySurfaceEq = Boolean.compare(
                            b.getLemma().equals(normalized),
                            a.getLemma().equals(normalized)
                        );
                        if (bySurfaceEq != 0) return bySurfaceEq;
                        return Integer.compare(a.getLemma().length(), b.getLemma().length());
                    })
                    .findFirst()
                    .orElse(null);
                String lemma = best != null ? best.getLemma() : normalized;
                if (lemma == null || lemma.length() <= 1) return null; // drop length 1 lemmas
                return lemma;
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
} 