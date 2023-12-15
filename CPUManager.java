import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

class Process implements Runnable {
    private final int id;
    private final CPU cpu;
    private final int memorySize;

    public Process(int id, CPU cpu, int memorySize) {
        this.id = id;
        this.cpu = cpu;
        this.memorySize = memorySize;
    }

    public int getId() {
        return id;
    }

    public int getMemorySize() {
        return memorySize;
    }

    @Override
    public void run() {
        try {
            System.out.println("Proceso " + id + " ha llegado.");

            // Simular el tiempo que tarda el proceso en ejecutarse
            TimeUnit.SECONDS.sleep(10);

            System.out.println("Proceso " + id + " ha terminado.");
            cpu.releaseMemory(this);
            cpu.AgregarProcesosFina(this);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

class CPU implements Runnable {
    private final List<Process> procesos;
    private final List<Process> cola;

    private  final List<Process> finalizado;
    private final int maxMemoria;

    private int usedMemoria;

    public CPU(int maxMemoria) {
        this.maxMemoria = maxMemoria;
        this.procesos = new ArrayList<>();
        this.cola = new ArrayList<>();
        this.finalizado = new ArrayList<>();
        this.usedMemoria = 0;
    }

    public void AgregarProcesos(Process process) {
        if (usedMemoria + process.getMemorySize() <= maxMemoria) {
            procesos.add(process);
            usedMemoria += process.getMemorySize();
            new Thread(process).start();
        } else {
            cola.add(process);
        }
    }
    public void AgregarProcesosFina(Process process) {
        finalizado.add(process);
    }


    public void SJN() {
        while (true) {
            if (!cola.isEmpty()) {
                Process smallestProcess = null;
                for (Process process : cola) {
                    if (usedMemoria + process.getMemorySize() <= maxMemoria) {
                        if (smallestProcess == null || process.getMemorySize() < smallestProcess.getMemorySize()) {
                            smallestProcess = process;
                        }
                    }
                }

                if (smallestProcess != null) {
                    cola.remove(smallestProcess);
                    procesos.add(smallestProcess);
                    usedMemoria += smallestProcess.getMemorySize();
                    new Thread(smallestProcess).start();
                }
            }
        }
    }

    public List<Process> getProcesos() {
        return procesos;
    }

    public List<Process> getCola() {
        return cola;
    }

    public List<Process> getFinalizado() {
        return finalizado;
    }
    public synchronized void releaseMemory(Process process) {
        usedMemoria -= process.getMemorySize(); // Liberar la memoria utilizada por el proceso
        procesos.remove(process); // Eliminar el proceso de la lista de procesos en ejecución

        // Verificar si hay procesos en la cola y espacio en memoria para ejecutarlos
        while (!cola.isEmpty() && usedMemoria < maxMemoria) {
            Process nextProcess = cola.remove(0);
            if (usedMemoria + nextProcess.getMemorySize() <= maxMemoria) {
                procesos.add(nextProcess);
                usedMemoria += nextProcess.getMemorySize();
                new Thread(nextProcess).start();
            } else {
                cola.add(nextProcess);
                break; // No hay suficiente memoria para agregar más procesos en este momento
            }
        }
    }



    @Override
    public void run() {
        SJN();


    }
}

class GUI implements Runnable {
    private final CPU cpu;
    private final JTextArea ejecutando;
    private final JTextArea esperando;
    private final JTextArea finalizando;

    public GUI(CPU cpu, JTextArea ejecutando, JTextArea esperando, JTextArea finalizando) {
        this.cpu = cpu;
        this.ejecutando = ejecutando;
        this.esperando = esperando;
        this.finalizando = finalizando;
    }

    @Override
    public void run() {
        JFrame frame = new JFrame("Estados de Procesos ");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);
        frame.setLayout(new GridLayout(1, 3));

        JScrollPane ejeScrollPane = new JScrollPane(ejecutando);
        JScrollPane espeScrollPane = new JScrollPane(esperando);
        JScrollPane finaScrollPane = new JScrollPane(finalizando);

        frame.add(ejeScrollPane);
        frame.add(espeScrollPane);
        frame.add(finaScrollPane);

        frame.setVisible(true);

        while (true) {
            ActualizarGUI();
            try {
                Thread.sleep(1000); // Actualiza la GUI cada segundo
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void ActualizarGUI() {
        ejecutando.setText("Procesos en ejecucion:\n");
        for (Process p : cpu.getProcesos()) {
            ejecutando.append("Proceso " + p.getId() + "\n");
        }

        esperando.setText("Procesos en espera:\n");
        for (Process p : cpu.getCola()) {
            esperando.append("Proceso " + p.getId() + "\n");
        }

        finalizando.setText("Procesos Finalizados:\n");
        for (Process p : cpu.getFinalizado()){
            finalizando.append("Proceso "+ p.getId()+" finalizado" + "\n");
        }
    }
}

public class CPUManager {
    public static void main(String[] args) {
        CPU cpu = new CPU(10);
        Thread cpuThread = new Thread(cpu);
        cpuThread.start();

        JTextArea ejecutando = new JTextArea();
        JTextArea esperando = new JTextArea();
        JTextArea finalizando = new JTextArea();

        GUI guiThread = new GUI(cpu, ejecutando, esperando, finalizando);
        Thread gui = new Thread(guiThread);
        gui.start();

        // Simulación de llegada de procesos cada 5 segundos
        int processId = 1;
        while (true) {
            int memorySize = (int) (Math.random() * 10) + 1;
            Process process = new Process(processId++,cpu,memorySize); // Id del proceso y tiempo de llegada
            cpu.AgregarProcesos(process);
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
