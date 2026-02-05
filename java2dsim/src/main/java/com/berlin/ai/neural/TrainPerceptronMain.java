package com.berlin.ai.neural;

import java.util.Random;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

/**
 * The neuron sums incoming signals.
 * Use in Perceptrons, simple, deterministic
 *
 * If the net electrical potential crosses a threshold (0):
 * <pre>
 *     neuron fires → +1
 *   Otherwise:
 *    neuron stays silent → -1
 *</pre>
 * Added bias - Allows threshold shift
 * Random init - Avoids symmetry
 * Shuffling - Prevents ordering bias
 * Error check - Stable convergence
 * Epochs - Repeated experience
 */
public class TrainPerceptronMain {

    // Weights (synapses)
    private double weightAge;
    private double weightEducation;

    // Bias (threshold)
    private double bias;

    private double learningRate;
    private Random random = new Random();

    public TrainPerceptronMain(final double learningRate) {
        this.learningRate = learningRate;

        // Small random initialization
        this.weightAge = randomWeight();
        this.weightEducation = randomWeight();
        this.bias = randomWeight();
    }

    private double randomWeight() {
        return (random.nextDouble() * 2) - 1; // [-1, 1]
    }

    // Sign activation function
    private int activate(double sum) {
        return sum >= 0 ? 1 : -1;
    }

    // Forward pass (summation + bias)
    public int predict(double age, double educationYears) {
        double sum =
                (age * weightAge) +
                        (educationYears * weightEducation) +
                        bias;

        return activate(sum);
    }

    // Training step (Perceptron Learning Rule)
    public void train(double age, double educationYears, int expected) {
        int prediction = predict(age, educationYears);
        int error = expected - prediction;

        if (error != 0) {
            weightAge += learningRate * error * age;
            weightEducation += learningRate * error * educationYears;
            bias += learningRate * error;
        }
    }

    public void printState() {
        System.out.printf(
                "Weights -> age: %.4f, education: %.4f, bias: %.4f%n",
                weightAge, weightEducation, bias
        );
    }

    // Training sample
    static class Sample {
        double age;
        double education;
        int label;

        Sample(double age, double education, int label) {
            this.age = age;
            this.education = education;
            this.label = label;
        }
    }

    public static void main(String[] args) {

        final TrainPerceptronMain perceptron = new TrainPerceptronMain(0.01);

        // Training data
        final List<Sample> trainingData = new ArrayList<>();
        trainingData.add(new Sample(22, 12, -1));
        trainingData.add(new Sample(25, 16, -1));
        trainingData.add(new Sample(28, 12, -1));
        trainingData.add(new Sample(30, 16,  1));
        trainingData.add(new Sample(35, 16,  1));
        trainingData.add(new Sample(45, 12,  1));

        int epochs = 100;

        for (int epoch = 1; epoch <= epochs; epoch++) {

            // Shuffle training data each epoch
            Collections.shuffle(trainingData);

            int errors = 0;

            for (Sample s : trainingData) {
                int prediction = perceptron.predict(s.age, s.education);
                if (prediction != s.label) {
                    errors++;
                }
                perceptron.train(s.age, s.education, s.label);
            }

            System.out.println("Epoch " + epoch +
                    " | errors = " + errors);
            perceptron.printState();

            // Early stopping if perfect
            if (errors == 0) {
                System.out.println("Converged early.");
                break;
            }
        }

        // Test after training
        System.out.println("\nTesting after training:");
        double testAge = 35;
        double testEducation = 16;

        int result = perceptron.predict(testAge, testEducation);

        System.out.println("Age: " + testAge +
                ", Education: " + testEducation +
                " => Output: " + result);
    }
}