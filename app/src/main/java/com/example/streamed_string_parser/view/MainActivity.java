package com.example.streamed_string_parser.view;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.streamed_string_parser.BuildConfig;
import com.example.streamed_string_parser.databinding.ActivityMainBinding;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import timber.log.Timber;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private volatile ExecutorService netWorkerThreadPool;
    private volatile ExecutorService logPool;
    private volatile ScheduledExecutorService schedulerThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        netWorkerThreadPool = Executors.newFixedThreadPool(5);
        schedulerThread = Executors.newSingleThreadScheduledExecutor();
        logPool = Executors.newFixedThreadPool(2);
        setContentView(binding.getRoot());
    }

    public synchronized ExecutorService getNetWorkerThreadPool() {
        return netWorkerThreadPool;
    }

    public synchronized ExecutorService getLogPool() {
        return logPool;
    }

    public synchronized ScheduledExecutorService getSchedulerThread() {
        return schedulerThread;
    }

    public void writeLog(String new_result) {
        final String path = getFilesDir().getAbsolutePath().replace("files", "results.log");
        try {
            final File resultsLogFile = new File(path);
            if (!resultsLogFile.exists()) {
                resultsLogFile.createNewFile();
                resultsLogFile.setReadable(true);
                resultsLogFile.setWritable(true);
            }
            try (FileWriter fileWriter = new FileWriter(resultsLogFile, true);
                 BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
                 PrintWriter printWriter = new PrintWriter(bufferedWriter)) {
                printWriter.println(new_result);
                Timber.i("Successfully written to logs");
            } catch (IOException e) {
                Toast.makeText(this, "Could not write to history", Toast.LENGTH_SHORT).show();
            }

        } catch (IOException f) {
            Toast.makeText(this, "Could not write to history", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        getNetWorkerThreadPool().shutdown();
        getLogPool().shutdown();
        getSchedulerThread().shutdown();
        super.onDestroy();
    }
}