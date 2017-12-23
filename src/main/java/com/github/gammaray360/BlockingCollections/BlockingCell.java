package com.github.gammaray360.BlockingCollections;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class BlockingCell<E> {
    private int DEFAULT_MAX_QUEUE = Integer.MAX_VALUE;

    private static final int EMPTY = 0;
    private static final int FULL = 1;
    private static final int CANCELED = 2;

    private E value = null;
    private int status = EMPTY;
    private int queue = 0;
    private int maxQueue = DEFAULT_MAX_QUEUE;
    private boolean isQueueingAllowed = true;
    private boolean isWriteValueAllowed = true;
    private final Lock lock = new ReentrantLock();
    private final Condition valueReady  = lock.newCondition();
    private final Condition queueEmpty = lock.newCondition();
    private final Condition queueingAllowed = lock.newCondition();
    private final Condition writingValueAllowed = lock.newCondition();


    public E getValue() throws TimeoutException, CancellationException, InterruptedException {
        lock.lockInterruptibly();
        try{
            return waitAndGetValue();
        } finally{
            lock.unlock();
        }
    }

    private E waitAndGetValue() throws TimeoutException {
        try {
            enterQueue();
            while (status == EMPTY) {
                valueReady.await();
            }
            if (status == CANCELED) {
                throw new CancellationException();
            }
            return value;
        } catch (InterruptedException e) {
            throw new TimeoutException();
        } finally {
            leaveQueue();
        }
    }

    private void enterQueue() throws InterruptedException {
        if(queue >= maxQueue){
            throw new IllegalStateException("Too meany threads waiting for a value");
        }
        while(!isQueueingAllowed){
            queueingAllowed.await();
        }
        queue++;
    }

    public E getValue(long timeoutNano) throws TimeoutException, CancellationException, InterruptedException {
        lock.lockInterruptibly();
        try{
            return waitAndGetValue(timeoutNano);
        } finally{
            lock.unlock();
        }
    }

    private E waitAndGetValue(long timeoutNano) throws TimeoutException {
        long nanos = timeoutNano;
        try {
            nanos = enterQueue(nanos);
            while (status == EMPTY) {
                nanos = valueReady.awaitNanos(nanos);
                if(nanos <= 0)
                    throw new TimeoutException();
            }
            if (status == CANCELED) {
                throw new CancellationException();
            }
            return value;
        } catch (InterruptedException e) {
            throw new TimeoutException();
        } finally {
            leaveQueue();
        }
    }

    private long enterQueue(long timeoutNano) throws InterruptedException, TimeoutException {
        long nanos = timeoutNano;
        if(queue >= maxQueue){
            throw new IllegalStateException("Too meany threads waiting for a value");
        }
        while(!isQueueingAllowed){
            nanos = queueingAllowed.awaitNanos(nanos);
            if(nanos <= 0)
                throw new TimeoutException();
        }
        queue++;
        return nanos;
    }

    private void leaveQueue(){
        queue--;
        if(queue == 0){
            queueEmpty.signalAll();
        }
    }

    public void setValue(E val) throws InterruptedException {
        Lock lock = this.lock;
        lock.lockInterruptibly();
        try {
            setFull(val);
        } finally {
            lock.unlock();
        }
    }
    public void cancel() throws InterruptedException {
        Lock lock = this.lock;
        lock.lockInterruptibly();
        try {
            setCanceled();
        } finally {
            lock.unlock();
        }
    }
    public void init() throws InterruptedException {
        Lock lock = this.lock;
        lock.lockInterruptibly();
        try {
            setCanceled();
            setEmpty();
        } finally {
            lock.unlock();
        }

    }

    private void setFull(E val) throws InterruptedException {
        try{
            forbidWritingValue();
            value = val;
            status = FULL;
            waitSignOut();
        } finally {
            allowWritingValue();
        }

    }
    private void setEmpty() throws InterruptedException {
        try {
            forbidWritingValue();
            value = null;
            status = EMPTY;
            waitSignOut();
        } finally {
            allowWritingValue();
        }

    }
    private void setCanceled() throws InterruptedException {
        try {
            forbidWritingValue();
            value = null;
            status = CANCELED;
            waitSignOut();
        } finally {
            allowWritingValue();
        }


    }

    private void waitSignOut() throws InterruptedException {
        try {
            forbidQueueing();
            while (queue >0){
                valueReady.signalAll();
                queueEmpty.awaitNanos(200000);
            }
        } finally {
            allowQueueing();
        }

    }

    private void allowQueueing(){
        isQueueingAllowed = true;
        queueingAllowed.signalAll();
    }

    private void forbidQueueing(){
        isQueueingAllowed = false;
    }

    private void allowWritingValue(){
        isWriteValueAllowed = true;
        writingValueAllowed.signal();
    }

    private void forbidWritingValue() throws InterruptedException {
        while(!isWriteValueAllowed) {
            writingValueAllowed.await();
        }
        isWriteValueAllowed = false;
    }

}