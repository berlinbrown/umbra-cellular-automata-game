package com.berlin.ai.neural;

public class SimpleNeuralNetwork {

    // weights and bias
    private double w1;
    private double w2;
    private double bias;

    // learning rate
    private double learningRate = 0.1;

    public SimpleNeuralNetwork() {
        // initialize weights randomly
        w1 = Math.random() - 0.5;
        w2 = Math.random() - 0.5;
        bias = Math.random() - 0.5;
    }

    // Sigmoid activation function
    private double sigmoid(double x) {
        return 1.0 / (1.0 + Math.exp(-x));
    }

    // Derivative of sigmoid
    private double sigmoidDerivative(double output) {
        return output * (1 - output);
    }

    // Forward pass
    public double predict(double temp, double cloudy) {
        double sum = temp * w1 + cloudy * w2 + bias;
        return sigmoid(sum);
    }

    // Training using gradient descent
    public void train(double[][] inputs, double[] targets, int epochs) {

        // train the neural network
        for (int epoch = 0; epoch < epochs; epoch++) {
            double totalError = 0;

            for (int i = 0; i < inputs.length; i++) {
                double temp = inputs[i][0];
                double cloudy = inputs[i][1];

                double output = predict(temp, cloudy);
                double error = targets[i] - output;
                totalError += Math.abs(error);

                // Backpropagation (for single neuron)
                double delta = error * sigmoidDerivative(output);

                w1 += learningRate * delta * temp;
                w2 += learningRate * delta * cloudy;
                bias += learningRate * delta;
            }

            if (epoch % 1000 == 0) {
                System.out.println("Epoch " + epoch + " Error: " + totalError);
            }
        }
    }

    public static void main(String[] args) {

        SimpleNeuralNetwork nn = new SimpleNeuralNetwork();

        double[][] inputs = {
                { 0.1, 1.0 },
                { 0.2, 0.8 },
                { 0.8, 0.2 },
                { 0.9, 0.0 }
        };

        double[] targets = {
                1,
                1,
                0,
                0
        };

        nn.train(inputs, targets, 10000);

        System.out.println("\nPredictions:");
        System.out.println("Cold & Cloudy: " + nn.predict(0.1, 1.0));
        System.out.println("Hot & Clear:   " + nn.predict(0.9, 0.0));
    }
}
