package stack;

import kotlinx.atomicfu.AtomicArray;
import kotlinx.atomicfu.AtomicRef;

import java.util.Random;

public class StackImpl implements Stack {

    private static final Random random = new Random(0);
    private static final int ELIMINATION_SIZE = 32;
    private static final int WINDOW_SIZE = 4;
    private static final int CNT_NOP = 100;

    private static class Node {
        final AtomicRef<Node> next;
        final int x;

        Node(int x, Node next) {
            this.next = new AtomicRef<>(next);
            this.x = x;
        }
    }

    // head pointer
    private final AtomicRef<Node> head = new AtomicRef<>(null);
    private final AtomicArray<Integer> eliminationArray = new AtomicArray<>(ELIMINATION_SIZE);


    @Override
    public void push(int x) {
        if (!tryPushWithElimination(x)) {
            while (true) {
                Node curHead = head.getValue();
                Node newHead = new Node(x, curHead);
                if (head.compareAndSet(curHead, newHead))
                    return;
            }
        }
    }

    private boolean tryPushWithElimination(int x) {
        Integer el = x; //  used autoboxing. Call Integer.valueOf(x), but it OK!!!
        int ind = random.nextInt(ELIMINATION_SIZE);
        for (int del = 0; del < WINDOW_SIZE; del++) {
            int n_ind = (ind + del) % ELIMINATION_SIZE;
            if (eliminationArray.get(n_ind).compareAndSet(null, el)) {
                int cnt = CNT_NOP;
                while (cnt-- > 0) {
                    assert true;      // Надеюсь компилятор это не убьет.
                }
                if (eliminationArray.get(n_ind).compareAndSet(el, null)) {
                    return false;
                } else {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public int pop() {
        Integer el = tryPopWithElimination();
        if (el != null) {
            return el;
        }
        while (true) {
            Node curHead = head.getValue();
            if (curHead == null) return Integer.MIN_VALUE;
            if (head.compareAndSet(curHead, curHead.next.getValue()))
                return curHead.x;
        }
    }

    public Integer tryPopWithElimination() {
        int ind = random.nextInt(ELIMINATION_SIZE);
        for (int del = 0; del < WINDOW_SIZE; del++) {
            int n_ind = (ind + del) % ELIMINATION_SIZE;
            Integer val = eliminationArray.get(n_ind).getValue();
            if (val != null && eliminationArray.get(n_ind).compareAndSet(val, null)) {
                return val;
            }
        }
        return null;
    }
}
