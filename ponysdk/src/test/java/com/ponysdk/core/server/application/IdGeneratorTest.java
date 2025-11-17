package com.ponysdk.core.server.application;

import org.junit.Assert;
import org.junit.Test;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.IntStream;

public class IdGeneratorTest {

    private final IdGenerator tested = new IdGenerator();

    @Test
    public void cycleLengthTest() {
        long count = 0L;
        int first = tested.nextID();
        while (count++ < Integer.MAX_VALUE + 64L) {
            if (tested.nextID() == first) {
                Assert.assertEquals("order: " + count, (1L + Integer.MAX_VALUE) / 64, count / 64);
                return;
            }
        }
        Assert.fail("no cycle");
    }

    @Test
    public void nextBitTest() {
        for (int i = 0; i < 100; i++) {
            long used = 0L;
            while (used != -1L) {
                int bit = IdGenerator.nextBit(used);
                Assert.assertEquals(Long.toHexString(used) + ',' + bit, 0, ((1L << bit) & used));
                used |= 1L << bit;
            }
        }
    }

    @Test
    public void visualStatisticsTest() throws IOException {
        final int SAMPLE_SIZE = 100_000_000;
        final int TIMESERIES_SAMPLE = 100_000; // Échantillonner pour série temporelle
        IdGenerator generator = new IdGenerator();
        
        System.out.println("Génération de " + SAMPLE_SIZE + " IDs...");
        
        // Variables pour statistiques
        Set<Integer> uniqueIds = new HashSet<>();
        int[] bitCounts = new int[31]; // Exclure le bit de signe
        
        // Ouvrir les fichiers CSV en streaming
        try (FileWriter timeseriesWriter = new FileWriter("id_timeseries.csv");
             FileWriter uniquenessWriter = new FileWriter("uniqueness.csv")) {
            
            timeseriesWriter.write("sequence,id,id_projection\n");
            uniquenessWriter.write("total_generated,unique_count,uniqueness_ratio\n");
            
            // Génération et analyse en streaming
            for (int i = 0; i < SAMPLE_SIZE; i++) {
                int id = generator.nextID();
                uniqueIds.add(id);
                
                // Analyse des bits
                for (int bit = 0; bit < 31; bit++) {
                    if ((id & (1 << bit)) != 0) {
                        bitCounts[bit]++;
                    }
                }
                
                // Échantillonner pour série temporelle (1 sur 1000)
                if (i % (SAMPLE_SIZE / TIMESERIES_SAMPLE) == 0) {
                    int projection = (id >>> 24) & 0xFF;
                    timeseriesWriter.write(i + "," + id + "," + projection + "\n");
                }
                
                // Écrire unicité tous les 100k
                if (i % 100_000 == 0 || i == SAMPLE_SIZE - 1) {
                    double ratio = (double) uniqueIds.size() / (i + 1);
                    uniquenessWriter.write((i + 1) + "," + uniqueIds.size() + "," + ratio + "\n");
                }
                
                // Progress
                if (i % 10_000_000 == 0) {
                    System.out.println("Progression: " + (i / 1_000_000) + "M IDs générés");
                }
            }
        }
        
        // Génération du fichier de distribution des bits
        generateBitDistributionCSV(bitCounts, SAMPLE_SIZE);
        
        // Statistiques finales
        double uniquenessRatio = (double) uniqueIds.size() / SAMPLE_SIZE;
        System.out.println("\n=== RÉSULTATS FINAUX ===");
        System.out.println("Taux d'unicité: " + String.format("%.6f", uniquenessRatio * 100) + "%");
        System.out.println("IDs uniques: " + uniqueIds.size() + "/" + SAMPLE_SIZE);
        System.out.println("Collisions: " + (SAMPLE_SIZE - uniqueIds.size()));
        
        // Vérifications
        Assert.assertTrue("Taux d'unicité trop faible: " + uniquenessRatio, uniquenessRatio > 0.99);
        
        System.out.println("\n=== DISTRIBUTION DES BITS ===");
        boolean allBitsOk = true;
        for (int i = 0; i < 31; i++) {
            double bitRatio = (double) bitCounts[i] / SAMPLE_SIZE;
            System.out.println(String.format("Bit %2d: %6.3f%%", i, bitRatio * 100));
            if (Math.abs(bitRatio - 0.5) >= 0.01) { // Tolérance 1% pour 100M échantillons
                System.out.println("⚠️  Bit " + i + " hors tolérance!");
                allBitsOk = false;
            }
        }
        
        Assert.assertTrue("Distribution des bits non uniforme", allBitsOk);
        
        System.out.println("\n=== FICHIERS GÉNÉRÉS ===");
        System.out.println("✓ bit_distribution.csv (distribution des 31 bits)");
        System.out.println("✓ id_timeseries.csv (" + TIMESERIES_SAMPLE + " échantillons)");
        System.out.println("✓ uniqueness.csv (courbe d'unicité)");
    }
    
    private void generateBitDistributionCSV(int[] bitCounts, int sampleSize) throws IOException {
        try (FileWriter writer = new FileWriter("bit_distribution.csv")) {
            writer.write("bit_position,count,percentage\n");
            for (int i = 0; i < bitCounts.length; i++) {
                double percentage = (double) bitCounts[i] / sampleSize * 100;
                writer.write(i + "," + bitCounts[i] + "," + percentage + "\n");
            }
        }
    }
    


}
