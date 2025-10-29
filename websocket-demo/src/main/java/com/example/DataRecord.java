package com.example;

public record DataRecord(Long time, Double value) {

    @Override
    public String toString() {
        return "DataRecord{" + "time=" + time + ", value=" + value + '}';
    }
}
