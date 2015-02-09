/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.charite.compbio.exomiser.core.writers;

import de.charite.compbio.exomiser.core.filters.FilterFactory;
import de.charite.compbio.exomiser.core.filters.FilterReport;
import de.charite.compbio.exomiser.core.filters.FilterReportFactory;
import de.charite.compbio.exomiser.core.filters.FilterType;
import de.charite.compbio.exomiser.core.ExomiserSettings;
import de.charite.compbio.exomiser.core.model.SampleData;
import de.charite.compbio.exomiser.core.model.VariantEvaluation;
import de.charite.compbio.jannovar.annotation.VariantEffect;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 *
 * @author Jules Jacobsen <jules.jacobsen@sanger.ac.uk>
 */
public class ResultsWriterUtils {
    
    private static final FilterReportFactory filterReportFactory = new FilterReportFactory();
    
    /**
     * Determines the correct file extension for a file given what was specified in the {@link de.charite.compbio.exomiser.core.ExomiserSettings}.
     * @param outFileName
     * @param outputFormat
     * @return 
     */
    public static String determineFileExtension(String outFileName, OutputFormat outputFormat) {

        String specifiedFileExtension = outputFormat.getFileExtension();
        String outFileExtension = FilenameUtils.getExtension(outFileName);
        if (outFileExtension.isEmpty() || outFileName.endsWith("-results")) {
            //default filename will end in the build number and "-results"
            outFileName = String.format("%s.%s",outFileName, specifiedFileExtension);
        } else {
            outFileName = outFileName.replace(outFileExtension, specifiedFileExtension);
        }
        return outFileName;
    }
    
    /**
     * Make a {@code VariantTypeCounter} object from the
     * list of {@code VariantEvaluation}.
     * We use this to print out a table of variant class distribution.
     * @param variantEvaluations
     * @return 
     */
    public static List<VariantTypeCount> makeVariantTypeCounters(List<VariantEvaluation> variantEvaluations) {
        VariantEffectCounter variantTypeCounter = makeVariantTypeCounter(variantEvaluations);

        final ImmutableList<ImmutableMap<VariantEffect, Integer>> freqMaps = variantTypeCounter.getFrequencyMap();

        int numIndividuals = 0;
        if (!variantEvaluations.isEmpty())
            numIndividuals = variantEvaluations.get(0).getNumberOfIndividuals();

        List<VariantTypeCount> result = new ArrayList<VariantTypeCount>();
        Set<VariantEffect> effects = new HashSet<VariantEffect>();
        for (int sampleIdx = 0; sampleIdx < numIndividuals; ++sampleIdx)
            for (VariantEffect effect : freqMaps.get(sampleIdx).keySet())
                effects.add(effect);
        
        for (VariantEffect effect : effects) {
            List<Integer> typeSpecificCounts = new ArrayList<Integer>();
            for (int sampleIdx = 0; sampleIdx < numIndividuals; ++sampleIdx)
                typeSpecificCounts.add(freqMaps.get(sampleIdx).get(effect));
            result.add(new VariantTypeCount(effect, typeSpecificCounts));
        }
        
        return result;
    }
    
    protected static VariantEffectCounter makeVariantTypeCounter(List<VariantEvaluation> variantEvaluations) {
        if (variantEvaluations.isEmpty())
            return new VariantEffectCounter(0);
        
        int numIndividuals = variantEvaluations.get(0).getNumberOfIndividuals();
        VariantEffectCounter effectCounter = new VariantEffectCounter(numIndividuals);

        for (VariantEvaluation variantEvaluation : variantEvaluations)
            effectCounter.put(variantEvaluation.getVariant());
        return effectCounter;
    }

    public static List<FilterReport> makeFilterReports(ExomiserSettings settings, SampleData sampleData) {
        //TODO: ExomiserSettings is really sticking it's nose into everything might be a good idea to scale
        //this back so that it's only really needed in to cli package as it is tightly coupled with that anyway.
        //For instance here it would be somewhat simpler to just supply the list of filters applied as they all
        //know what their required parameters were. Sure this will violate the 'Tell Don't Ask' principle but
        //the alternatives are worse
        List<FilterType> filtersApplied = FilterFactory.determineFilterTypesToRun(settings);
        return filterReportFactory.makeFilterReports(filtersApplied, settings, sampleData);
        
    }
    
}
