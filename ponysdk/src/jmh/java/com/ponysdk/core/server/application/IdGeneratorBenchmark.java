package com.ponysdk.core.server.application;

import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(3)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 10, time = 1)
@State(Scope.Benchmark)
public class IdGeneratorBenchmark {

    public IdGenerator tested = new IdGenerator();
    public int counterID = 0;


    @Benchmark
    @Fork(jvmArgsAppend = "-Xint") //prevent compilation
    public boolean initializeGeneratorClass(){
        // emulate the computations done at class loading to see if it is worth to use magic numbers instead.
        return IdGenerator.computeOrderFactors()[0] == IdGenerator.findPrimitiveRoot();
    }

    @Benchmark
    public Object initializeGenerator() {
        return new IdGenerator();
    }

    @Benchmark
    public int nextId() {
        return tested.nextID();
    }

    @Benchmark
    public int incrementalNextID() {
        return ++counterID;
    }

}
