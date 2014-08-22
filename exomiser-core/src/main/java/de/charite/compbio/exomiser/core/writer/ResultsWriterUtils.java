/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.charite.compbio.exomiser.core.writer;

import org.apache.commons.io.FilenameUtils;

/**
 *
 * @author Jules Jacobsen <jules.jacobsen@sanger.ac.uk>
 */
public class ResultsWriterUtils {
    
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
}