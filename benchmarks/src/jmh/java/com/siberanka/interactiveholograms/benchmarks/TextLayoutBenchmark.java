package com.siberanka.interactiveholograms.benchmarks;

import com.siberanka.interactiveholograms.display.TextLayoutScanner;

public class TextLayoutBenchmark {

    public static void main(String[] args) {
        String testString = "&aHello &bWorld &#FF0000Hex <bold>MiniMessage</bold> %player_name%";
        long start = System.nanoTime();
        int iterations = 100_000;
        int totalLen = 0;
        for (int i = 0; i < iterations; i++) {
            totalLen += TextLayoutScanner.visibleLength(testString);
        }
        long elapsed = System.nanoTime() - start;
        System.out.printf("Processed %d iterations in %.2f ms (%.2f ns/op), checksum: %d%n",
                iterations, elapsed / 1_000_000.0, elapsed / (double) iterations, totalLen);
    }
}
