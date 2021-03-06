package com.github.alexcojocaru.mojo.elasticsearch;

import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Starts a local instance of ElasticSearch indefinitely.
 * In order to kill it a CTRL+C has to be made.
 *
 * @author gfernandes
 * @goal run
 * @execute phase="compile"
 * @requiresDependencyResolution runtime
 */
public class RunElasticsearchNodeMojo extends AbstractStartElasticsearchNodeMojo {
    private static final long SHUTDOWN_TIMEOUT = 300;
    /**
     * @parameter
     */
    private File scriptFile;

    final private CountDownLatch waitES = new CountDownLatch(1);

    public void execute() throws MojoExecutionException {
        super.execute();
        if (scriptFile != null) {
            getLog().info("RunElasticsearchNodeMojo loading data");
            LoadElasticsearchUtility.load(scriptFile, getLog());
        }

        //Adding shutdown hook to stop ES
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                ElasticsearchNode.stop();
                waitES.countDown();
            }
        });

        waitIndefinitely();

        getLog().info("RunElasticsearchNodeMojo waiting for ES to be stopped");
        try {
            waitES.await(SHUTDOWN_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            getLog().warn("RunElasticsearchNodeMojo interrupted, ES instance has not stopped after " +
                    SHUTDOWN_TIMEOUT + "ms");
        }
    }

    /**
     * Causes the current thread to wait indefinitely. This method does not return.
     */
    private void waitIndefinitely() {
        Object lock = new Object();

        synchronized (lock) {
            try {
                lock.wait();
            } catch (InterruptedException exception) {
                getLog().warn("RunElasticsearchNodeMojo interrupted");
            }
        }
    }

}
