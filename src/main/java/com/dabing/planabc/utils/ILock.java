package com.dabing.planabc.utils;

import java.util.concurrent.TimeUnit;

public interface ILock {
    boolean tryLock(long timeout, TimeUnit unit);
    void unLock();
}
