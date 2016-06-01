package com.ponysdk.core.writer;

@FunctionalInterface
public interface ModelWriterCallback {
    public void doWrite(ModelWriter writer);

}
