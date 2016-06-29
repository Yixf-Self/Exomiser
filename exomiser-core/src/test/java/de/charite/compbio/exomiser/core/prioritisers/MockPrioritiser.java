/*
 * The Exomiser - A tool to annotate and prioritize variants
 *
 * Copyright (C) 2012 - 2016  Charite Universitätsmedizin Berlin and Genome Research Ltd.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.charite.compbio.exomiser.core.prioritisers;

import de.charite.compbio.exomiser.core.model.Gene;

import java.util.List;
import java.util.Map;

/**
 * Class for mocking a Prioritiser of the given PriorityType. Will score genes with scores specified for the geneIds
 * supplied in the constructor with the type.
 *
 * @author Jules Jacobsen <jules.jacobsen@sanger.ac.uk>
 */
public class MockPrioritiser implements Prioritiser {

    private final PriorityType priorityType;
    private final Map<String, Float> expectedScores;

    public MockPrioritiser(PriorityType priorityType, Map<String, Float> geneSymbolPrioritiserScores) {
        this.priorityType = priorityType;
        expectedScores = geneSymbolPrioritiserScores;
    }

    @Override
    public void prioritizeGenes(List<Gene> genes) {
        for (Gene gene : genes) {
            String geneSymbol = gene.getGeneSymbol();
            Float score = expectedScores.getOrDefault(geneSymbol, 0f);
            int geneId = gene.getEntrezGeneID();
            gene.addPriorityResult(new MockPriorityResult(priorityType, geneId, geneSymbol, score));
        }
    }

    @Override
    public PriorityType getPriorityType() {
        return priorityType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MockPrioritiser that = (MockPrioritiser) o;

        if (priorityType != that.priorityType) return false;
        return expectedScores.equals(that.expectedScores);

    }

    @Override
    public int hashCode() {
        int result = priorityType.hashCode();
        result = 31 * result + expectedScores.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "MockPrioritiser{" +
                "priorityType=" + priorityType +
                ", expectedScores=" + expectedScores +
                '}';
    }
}
