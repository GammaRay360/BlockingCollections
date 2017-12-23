package com.github.gammaray360.BlockingCollections;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.*;

public class BlockingCellTest {
    private final static int NUM_THREADS = 10; // how many threads of each kind
    private final static int MAX_SLEEP_TIME = 400; // nano seconds
    private final static int TIMEOUT_TIME = 400; // nano seconds

    @Test
    public void testForDeadLock(){
        BlockingCell<Integer> cell = new BlockingCell<>();
        for(int i=0; i<NUM_THREADS; i++){
            new GetterThread("Getter " + i, cell).start();
            new SetterThread("Setter " + i, cell).start();
            new CancelerThread("Canceler " + i, cell).start();
            new InitterThread("Initter " + i ,cell).start();
        }
        // TODO: test for deadlock
    }

    public static class CancelerThread extends Thread {
        BlockingCell<Integer> cell;
        Random rand = new Random();

        CancelerThread(String name, BlockingCell<Integer> cell) {
            setName(name);
            this.cell = cell;
        }
        @Override
        public void run() {

            while (true) {
                try {
                    sleep(rand.nextInt(MAX_SLEEP_TIME));
                    cell.cancel();
                    System.out.println(getName() + ": canceling");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }
    }
    public static class GetterThread extends Thread{
        BlockingCell<Integer> cell;
        Random rand = new Random();
        GetterThread(String name, BlockingCell<Integer> cell){
            setName(name);
            this.cell = cell;
        }
        @Override
        public void run() {
            while (true) {
                try {
                    sleep(rand.nextInt(MAX_SLEEP_TIME));
                    int val = cell.getValue(TIMEOUT_TIME);
                    System.out.println(getName() + ": got " + val);
                } catch (CancellationException e) {
                    System.out.println(getName() + ": got canceled");
                } catch (TimeoutException e) {
                    System.out.println(getName() + ": got timeout");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }
    }
    public static class SetterThread extends Thread{
        BlockingCell<Integer> cell;
        Random rand = new Random();
        SetterThread(String name, BlockingCell<Integer> cell){
            setName(name);
            this.cell = cell;
        }
        @Override
        public void run() {
            while(true){
                try {
                    sleep(rand.nextInt(MAX_SLEEP_TIME));
                    int val = rand.nextInt(200);
                    cell.setValue(val);
                    System.out.println(getName() + ": set " + val);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }
    }
    public static class InitterThread extends Thread{
        BlockingCell<Integer> cell;
        Random rand = new Random();
        InitterThread(String name, BlockingCell<Integer> cell){
            setName(name);
            this.cell = cell;
        }
        @Override
        public void run() {
            while(true){
                try {
                    sleep(rand.nextInt(MAX_SLEEP_TIME));
                    cell.init();
                    System.out.println(getName() + ": initing");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }
    }
}