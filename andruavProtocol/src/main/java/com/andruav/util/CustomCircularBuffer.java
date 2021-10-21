package com.andruav.util;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

/**
 * Created by mhefny on 8/26/16.
 * http://stackoverflow.com/questions/30612035/circular-buffer-implementation-in-android
 */
public class CustomCircularBuffer<T> {

    private int unitCount =0;
    private final T[] buffer;

    private int tail;

    private int head;

    private final Object SyncO = new Object();

    public CustomCircularBuffer(int n) {
        buffer = (T[]) new Object[n];
        tail = 0;
        head = 0;
    }


    public synchronized  Object[]  getAsList ()
    {
        return buffer.clone();
    }
    /***
     *
     * @param toAdd
     * @param forgetOld override old when buffer overflow. the buffer always keeps last fresh n only. no Buffer Over Flow.
     */
    public synchronized void add(final T toAdd,final boolean forgetOld) {
        if (forgetOld || (unitCount < buffer.length)) {
            buffer[head] = toAdd;
            head +=1;
            unitCount = (unitCount + 1) % buffer.length;
        } else {
            throw new BufferOverflowException();
        }
        head = head % buffer.length;
    }

    public synchronized  T get(final boolean ignoreUnderFlow) {
        T t = null;
        if (unitCount==0) return t;
        if (unitCount < buffer.length) {
            t = buffer[tail];
            tail +=1;
            tail = tail % buffer.length;
            unitCount = unitCount -1;
        } else {
            if (!ignoreUnderFlow) {
                throw new BufferUnderflowException();
            }
        }
        return t;
    }



    public synchronized void Flush ()
    {
        tail = 0;
        head = 0;
        unitCount = 0; // BUG FIXED
    }

    public synchronized String toString() {
        return "CustomCircularBuffer(size=" + buffer.length + ", head=" + head + ", tail=" + tail + ")";
    }


    public int getCount ()
    {
        return this.unitCount;
    }

    public boolean bufferFull() {

        return  (unitCount >= buffer.length);
    }
}
