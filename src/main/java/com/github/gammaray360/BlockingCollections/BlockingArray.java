package com.github.gammaray360.BlockingCollections;

import java.util.ArrayList;
import java.util.concurrent.TimeoutException;

/**
 * Created by junk_ on 11/11/2017.
 */
public class BlockingArray<E> {
    // region private members {...}
    private int mLength;
    private final ArrayList<BlockingCell<E>> mArray;
    // endregion

    // region constructors {...}
    public BlockingArray(int arrayLength){
        mLength = arrayLength;
        mArray = new ArrayList<>(mLength);
        for(int i=0; i<mLength; i++){
            mArray.add(new BlockingCell<>());
        }
    }

    public E get(int index) throws TimeoutException, InterruptedException {
        BlockingCell<E> cell = mArray.get(index);
        return cell.getValue();
    }

    public E get(int index, long timeoutNano) throws TimeoutException, InterruptedException {
        BlockingCell<E> cell = mArray.get(index);
        return cell.getValue(timeoutNano);
    }

    public void set(int index, E value) throws InterruptedException {
        BlockingCell<E> cell = mArray.get(index);
        cell.setValue(value);
    }

    public void cancel(int index) throws InterruptedException {
        BlockingCell<E> cell = mArray.get(index);
        cell.cancel();
    }
    public void init(int index) throws InterruptedException {
        BlockingCell<E> cell = mArray.get(index);
        cell.init();
    }
}
