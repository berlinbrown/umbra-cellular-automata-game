package com.berlin.cellular.basic;

public class ApplicationSimUIMain {

    public static void main(final String [] args) {
        System.out.println(">>> Running application");
        final CoreSimulationGraphicsRenderer automata = new CoreSimulationGraphicsRenderer();
        automata.invokeLater();
    }     
    
}

