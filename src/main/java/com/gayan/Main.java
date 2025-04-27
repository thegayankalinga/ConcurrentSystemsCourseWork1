package com.gayan;

import com.gayan.utilz.SimulationManager;
import com.gayan.utilz.TerminalColorConstant;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {

        System.out.println(TerminalColorConstant.GREEN_BOLD +
                "\n===============================================" +
                "\n    Ticket System Concurrent Simulator" +
                "\n===============================================\n" +
                TerminalColorConstant.RESET);

        SimulationManager manager = new SimulationManager();
        manager.startSimulation();
    }
}