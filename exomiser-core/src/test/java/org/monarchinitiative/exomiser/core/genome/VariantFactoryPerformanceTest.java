/*
 * The Exomiser - A tool to annotate and prioritize genomic variants
 *
 * Copyright (c) 2016-2019 Queen Mary University of London.
 * Copyright (c) 2012-2016 Charité Universitätsmedizin Berlin and Genome Research Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.monarchinitiative.exomiser.core.genome;

import de.charite.compbio.jannovar.data.JannovarData;
import org.h2.mvstore.MVStore;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.exomiser.core.genome.dao.AllelePropertiesDao;
import org.monarchinitiative.exomiser.core.genome.dao.AllelePropertiesDaoMvStore;
import org.monarchinitiative.exomiser.core.genome.jannovar.JannovarDataProtoSerialiser;
import org.monarchinitiative.exomiser.core.model.*;
import org.monarchinitiative.exomiser.core.proto.AlleleProto;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/**
 * @author Jules Jacobsen <j.jacobsen@qmul.ac.uk>
 */
public class VariantFactoryPerformanceTest {
    /**
     * Comparative performance test for loading a full genome. Ignored by default as this takes a few minutes.
     */
    @Disabled("Performance test - won't run on CI server")
    @Test
    public void testGenome() {

        VariantAnnotator stubVariantAnnotator = new StubVariantAnnotator();
        VariantFactory stubAnnotationVariantFactory = new VariantFactoryImpl(stubVariantAnnotator);

        //warm-up
        for (int i = 0; i < 1000; i++) {
            countVariants(stubAnnotationVariantFactory, Paths.get("src/test/resources/multiSampleWithProbandHomRef.vcf"), new StubAllelePropertiesDao());
        }

        Path vcfPath = Paths.get("C:/Users/hhx640/Documents/exomiser-cli-dev/examples/NA19722_601952_AUTOSOMAL_RECESSIVE_POMP_13_29233225_5UTR_38.vcf.gz");
        System.out.println("Read variants with stub annotations, stub data - baseline file reading and VariantEvaluation creation");
        for (int i = 0; i < 4; i++) {
            Instant start = Instant.now();

            long numVariants = countVariants(stubAnnotationVariantFactory, vcfPath, new StubAllelePropertiesDao());

            Duration duration = Duration.between(start, Instant.now());
            long ms = duration.toMillis();
            System.out.printf("Read %d in in %dm %ds %dms (%d ms)%n", numVariants, (ms / 1000) / 60 % 60, ms / 1000 % 60, ms % 1000, ms);
        }

        VariantAnnotator jannovarVariantAnnotator = new JannovarVariantAnnotator(GenomeAssembly.HG19, loadJannovarData(), ChromosomalRegionIndex
                .empty());
        VariantFactory jannovarVariantFactory = new VariantFactoryImpl(jannovarVariantAnnotator);

        System.out.println("Read variants with real annotations, stub data");
        for (int i = 0; i < 4; i++) {
            Instant start = Instant.now();

            long numVariants = countVariants(jannovarVariantFactory, vcfPath, new StubAllelePropertiesDao());

            Duration duration = Duration.between(start, Instant.now());
            long ms = duration.toMillis();
            System.out.printf("Read %d in in %dm %ds %dms (%d ms)%n", numVariants, (ms / 1000) / 60 % 60, ms / 1000 % 60, ms % 1000, ms);
        }

//        System.out.println("Read variants with real annotations, stub data");
//        countVariants(jannovarVariantFactory, vcfPath, new StubAllelePropertiesDao());

//        System.out.println("Read variants with stub annotations, real data");
//        countVariants(stubAnnotationVariantFactory, vcfPath, allelePropertiesDao());
//
//        System.out.println("Read variants with real annotations, real data");
//        countVariants(jannovarVariantFactory, vcfPath, allelePropertiesDao());

    }

    private long countVariants(VariantFactory variantFactory, Path vcfPath, AllelePropertiesDao allelePropertiesDao) {
        return variantFactory.createVariantEvaluations(vcfPath)
                .map(annotateVariant(allelePropertiesDao))
                .count();
    }

    private Function<VariantEvaluation, VariantEvaluation> annotateVariant(AllelePropertiesDao allelePropertiesDao) {
        AtomicLong totalSeekTime = new AtomicLong(0);
        AtomicInteger count = new AtomicInteger(0);
        return variantEvaluation -> {
            Instant start = Instant.now();
            AlleleProto.AlleleProperties alleleProperties = allelePropertiesDao.getAlleleProperties(variantEvaluation);
            Instant end = Instant.now();
            totalSeekTime.getAndAdd(Duration.between(start, end).toMillis());
            int current = count.incrementAndGet();
            if (current % 100_000 == 0) {
                double aveSeekTime = (double) totalSeekTime.get() / (double) current;
//                System.out.printf("%s variants in %d ms - ave seek time %3f ms/variant%n", current, totalSeekTime.get(), aveSeekTime);
            }
            variantEvaluation.setFrequencyData(AlleleProtoAdaptor.toFrequencyData(alleleProperties));
            variantEvaluation.setPathogenicityData(AlleleProtoAdaptor.toPathogenicityData(alleleProperties));
            return variantEvaluation;
        };
    }

    private class StubVariantAnnotator implements VariantAnnotator {

        @Override
        public VariantAnnotation annotate(String chr, int pos, String ref, String alt) {
            return VariantAnnotation.builder()
                    .chromosomeName(chr)
                    .chromosome(toChromosomeNumber(chr))
                    .position(pos)
                    .ref(ref)
                    .alt(alt)
                    .geneSymbol("GENE")
                    .build();
        }

        private int toChromosomeNumber(String chr) {
            switch (chr) {
                case "X":
                    return 23;
                case "Y":
                    return 24;
                case "M":
                case "MT":
                    return 25;
                default:
                    return Integer.parseInt(chr);
            }
        }
    }

    private JannovarData loadJannovarData() {
        Path transcriptFilePath = Paths.get("C:/Users/hhx640/Documents/exomiser-data/1811_hg19/1811_hg19_transcripts_ucsc.ser");
        return JannovarDataProtoSerialiser.load(transcriptFilePath);
    }

    private class StubAllelePropertiesDao implements AllelePropertiesDao {
        @Override
        public AlleleProto.AlleleProperties getAlleleProperties(AlleleProto.AlleleKey alleleKey, GenomeAssembly genomeAssembly) {
            return AlleleProto.AlleleProperties.getDefaultInstance();
        }

        @Override
        public AlleleProto.AlleleProperties getAlleleProperties(Variant variant) {
            return AlleleProto.AlleleProperties.getDefaultInstance();
        }
    }

    private AllelePropertiesDao allelePropertiesDao() {
        Path mvStorePath = Paths.get("C:/Users/hhx640/Documents/exomiser-data/1811_hg19/1811_hg19_variants.mv.db").toAbsolutePath();
        MVStore mvStore = new MVStore.Builder()
                .fileName(mvStorePath.toString())
                .cacheSize(32)
                .readOnly()
                .open();;
        return new AllelePropertiesDaoMvStore(mvStore);
    }
}
