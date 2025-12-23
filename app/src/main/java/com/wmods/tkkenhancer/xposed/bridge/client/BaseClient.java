package com.wmods.tkkenhancer.xposed.bridge.client;

import com.wmods.tkkenhancer.xposed.bridge.TkeIIFace;

import java.util.concurrent.CompletableFuture;

public abstract class BaseClient {

    public abstract TkeIIFace getService();

    public abstract CompletableFuture<Boolean> connect();

    public abstract void tryReconnect();

}
