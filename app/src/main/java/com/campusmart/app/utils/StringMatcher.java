package com.campusmart.app.utils;

import java.util.regex.Pattern;

public class StringMatcher {

    public static boolean compare(String s1, String s2) {
        if (s1 == null || s2 == null) {
            return false;
        }
        String normalizedS1 = normalize(s1);
        String normalizedS2 = normalize(s2);
        return normalizedS1.equals(normalizedS2);
    }


    public static boolean compareWithLevenshtein(String s1, String s2, int maxDistance) {
        if (s1 == null || s2 == null) {
            return false;
        }
        String normalizedS1 = normalize(s1);
        String normalizedS2 = normalize(s2);

        int distance = calculateLevenshteinDistance(normalizedS1, normalizedS2);
        return distance <= maxDistance;
    }

    private static String normalize(String s) {
        // Trim leading/trailing whitespace, convert to lowercase, and replace multiple spaces with a single space.
        s = s.trim().toLowerCase();
        s = Pattern.compile("\\s+").matcher(s).replaceAll(" ");
        // Additionally, remove common non-alphanumeric characters that might be OCR errors
        s = s.replaceAll("[^a-z0-9 ]", "");
        return s;
    }

    private static int calculateLevenshteinDistance(String s1, String s2) {
        int[] costs = new int[s2.length() + 1];
        for (int i = 0; i <= s1.length(); i++) {
            int lastValue = i;
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) {
                    costs[j] = j;
                } else {
                    if (j > 0) {
                        int newValue = costs[j - 1];
                        if (s1.charAt(i - 1) != s2.charAt(j - 1)) {
                            newValue = Math.min(Math.min(newValue, lastValue), costs[j]) + 1;
                        }
                        costs[j - 1] = lastValue;
                        lastValue = newValue;
                    }
                }
            }
            if (i > 0) {
                costs[s2.length()] = lastValue;
            }
        }
        return costs[s2.length()];
    }
}