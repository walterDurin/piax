package org.piax.trans.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class PeriodicRunner extends TimerTask {
    long period;
    Timer timer;
    List<Runnable> tasks;
    public PeriodicRunner(long period) {
        this.period = period;
        this.timer = new Timer();
        this.timer.schedule(this, 0, period);
        this.tasks = new ArrayList<Runnable>();
    }

    public void addTask(Runnable task) {
        synchronized(tasks) {
            tasks.add(task);
        }
    }

    public void run() {
        //        System.out.println("PeriodicRunner: task number=" + tasks.size());
        for (Runnable r : tasks) {
            r.run();
        }
    }
    
    public void stop() {
        timer.cancel();
    }
}