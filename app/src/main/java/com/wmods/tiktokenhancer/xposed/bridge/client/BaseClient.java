package com.wmods.tiktokenhancer.xposed.bridge.client;

import com.wmods.tiktokenhancer.xposed.bridge.WaeIIFace;

import java.util.concurrent.CompletableFuture;

public abstract class BaseClient {

    public abstract WaeIIFace getService();

    public abstract CompletableFuture<Boolean> connect();

    public abstract void tryReconnect();

}
