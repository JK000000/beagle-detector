package com.jk.beagledetector;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadManager {
    private static ThreadManager instance = new ThreadManager();

    private ThreadManager() {}

    public static ThreadManager getInstance()
    {
        return instance;
    }

    public ExecutorService cameraExecutor = Executors.newSingleThreadExecutor();
    public ExecutorService diskIoExecutor = Executors.newFixedThreadPool(3);
}
