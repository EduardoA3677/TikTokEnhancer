package com.wmods.tiktokenhancer.xposed.utils;

public class HKDFv3 extends HKDF {
    @Override
    protected int getIterationStartOffset() {
        return 1;
    }
}
