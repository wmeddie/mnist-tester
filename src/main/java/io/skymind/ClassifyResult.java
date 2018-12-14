package io.skymind;

import java.util.List;

public class ClassifyResult {
    private final List<Integer> results;
    private final List<Float> probabilities;

    public ClassifyResult(List<Integer> results, List<Float> probabilities) {
        this.results = results;
        this.probabilities = probabilities;
    }

    public List<Float> getProbabilities() {
        return probabilities;
    }

    public List<Integer> getResults() {
        return results;
    }
}
