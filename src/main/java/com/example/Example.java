package com.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

public class Example {

    public static final Logger logger = LoggerFactory.getLogger(Example.class);

    private final Set<LovePhantom> phantomReferences = ConcurrentHashMap.newKeySet();

    private final ReferenceQueue<Love> referenceQueue = new ReferenceQueue<>();

    public void run() throws InterruptedException {
        startFinalizerThread();

        while (true) {
            createInstance();
            Thread.sleep(100);
        }
    }

    public void startFinalizerThread() {
        Executors.newSingleThreadExecutor().execute(() -> {
            while (true) {
                try {
                    LovePhantom lovePhantom = (LovePhantom) referenceQueue.remove(1000);

                    if (lovePhantom != null) {
                        try {
                            logger.info("closed");
                            lovePhantom.closeLoveResource();

                            // Unlike soft and weak references, phantom references are not automatically cleared
                            // by the garbage collector as they are enqueued
                            lovePhantom.clear();
                        } finally {
                            phantomReferences.remove(lovePhantom);
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void createInstance() {
        Love love = new Love();
        LovePhantom lovePhantom = new LovePhantom(love, love.getLoveResource(), referenceQueue);
        phantomReferences.add(lovePhantom);
        logger.info("new");
    }

    public static void main(String[] args) throws InterruptedException {
        new Example().run();
    }

}

class LoveResource {

    private final BufferedInputStream buf;

    public LoveResource() {
        // Allocate a large amount of memory
        int bufSize = 65536 * 32;
        this.buf = new BufferedInputStream(new ByteArrayInputStream(new byte[]{}), bufSize);
    }

    public void close() {
        try {
            buf.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class Love {

    public static final Logger logger = LoggerFactory.getLogger(Love.class);

    private final LoveResource loveResource;

    public Love() {
        this.loveResource = new LoveResource();
    }

    public LoveResource getLoveResource() {
        return loveResource;
    }

}

class LovePhantom extends PhantomReference<Love> {

    private final LoveResource loveResource;

    public LovePhantom(Love love, LoveResource loveResource, ReferenceQueue<Love> q) {
        super(love, q);

        // Don’t do this.
        //
        // this.love = love;
        //
        // If then, the reference will not be enqueued.
        // That's why I keep LoveResource separately from Love.

        this.loveResource = loveResource;
    }

    public void closeLoveResource() {
        loveResource.close();
    }

}