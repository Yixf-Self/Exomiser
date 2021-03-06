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

import de.charite.compbio.jannovar.pedigree.Genotype;
import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.GenotypeBuilder;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.variantcontext.VariantContextBuilder;
import org.monarchinitiative.exomiser.core.model.Variant;
import org.monarchinitiative.exomiser.core.model.VariantEvaluation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * Helper class for constructing {@link Variant} objects for tests.
 *
 * The construction of {@link Variant} objects is quite complex but for tests,
 * we would ideally have them for testing our data sets. This class helps us
 * with the construction.
 */
public class TestVariantFactory {

    private static final Logger logger = LoggerFactory.getLogger(TestVariantFactory.class);

    private final VariantFactoryImpl variantFactory = (VariantFactoryImpl) TestFactory.buildDefaultVariantFactory();

    /**
     * Construct a new {@link Variant} object with the given values. n.b. this follows the VCF standard of being 1-based.
     *
     * @param chrom numeric chromosome id
     * @param pos one-based position of the variant
     * @param ref reference string
     * @param alt alt string
     * @param gt the Genotype to use
     * @param readDepth depth the read depth to use
     * @param altAlleleID alternative allele ID
     * @param qual phred-scale quality
     * @return {@link Variant} with the setting
     */
    public VariantEvaluation buildVariant(int chrom, int pos, String ref, String alt, Genotype gt, int readDepth, int altAlleleID, double qual) {
        VariantContext variantContext = buildVariantContext(chrom, pos, ref, alt, gt, readDepth, qual);
        Allele altAllele = variantContext.getAlternateAllele(altAlleleID);
        return variantFactory.buildVariantEvaluation(variantContext, altAlleleID, altAllele);
    }

    private VariantContext buildVariantContext(int chrom, int pos, String ref, String alt, Genotype genotype, int readDepth, double qual) {
        Allele refAllele = Allele.create(ref, true);
        Allele altAllele = Allele.create(alt);

        // build Genotype
        GenotypeBuilder genotypeBuilder = buildGenotype(genotype, readDepth, refAllele, altAllele);

        // build VariantContext
        VariantContextBuilder vcBuilder = new VariantContextBuilder();
        vcBuilder.loc("chr" + chrom, pos, pos - 1L + ref.length());
//        vcBuilder.loc(Integer.toString(chrom), pos, pos - 1L + ref.length());
        vcBuilder.alleles(Arrays.asList(refAllele, altAllele));
        vcBuilder.genotypes(genotypeBuilder.make());
        vcBuilder.attribute("RD", readDepth);
        vcBuilder.log10PError(-0.1 * qual);

        return vcBuilder.make();
    }

    private GenotypeBuilder buildGenotype(Genotype gt, int readDepth, Allele refAllele, Allele altAllele) {
        GenotypeBuilder gtBuilder = new GenotypeBuilder("sample");
        setGenotype(gtBuilder, refAllele, altAllele, gt);
        gtBuilder.attribute("RD", readDepth);
        return gtBuilder;
    }

    private void setGenotype(GenotypeBuilder genotypeBuilder, Allele refAllele, Allele altAllele, Genotype genotype) {
        switch (genotype) {
            case HOMOZYGOUS_ALT:
                genotypeBuilder.alleles(Arrays.asList(altAllele, altAllele));
                break;
            case HOMOZYGOUS_REF:
                genotypeBuilder.alleles(Arrays.asList(refAllele, refAllele));
                break;
            case HETEROZYGOUS:
                genotypeBuilder.alleles(Arrays.asList(refAllele, altAllele));
                break;
            default:
                break;
        }
    }

}
