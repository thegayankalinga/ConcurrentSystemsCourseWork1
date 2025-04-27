package com.gayan;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {

        System.out.println(utilz.TerminalColorConstant.GREEN_BOLD +
                "\n===============================================" +
                "\n    Ticket System Concurrent Simulator" +
                "\n===============================================\n" +
                utilz.TerminalColorConstant.RESET);

        utilz.SimulationManager manager = new utilz.SimulationManager();
        manager.startSimulation();
    }
}