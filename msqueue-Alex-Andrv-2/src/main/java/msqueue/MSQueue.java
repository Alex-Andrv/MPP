package msqueue;

import kotlinx.atomicfu.AtomicRef;

public class MSQueue implements Queue {
    private AtomicRef<Node> head;
    private AtomicRef<Node> tail;

    public MSQueue() {
        Node dummy = new Node(0);
        this.head = new AtomicRef<>(dummy);
        this.tail = new AtomicRef<>(dummy);
    }

    @Override
    public void enqueue(int x) {
        Node newTail = new Node(x);
        while (true) {
            Node curTail = tail.getValue();
            // interrupt: drop Node, add Node, partial add Node
            if (tail.getValue().next.compareAndSet(null, newTail)) {
                //try force
                tail.compareAndSet(curTail, newTail);
                return; // mean we add Node
            } else {
                Node realTail = curTail.next.getValue(); // Ok
                // help other
                tail.compareAndSet(curTail, realTail);
            }
        }
    }

    @Override
    public int dequeue() {
        while (true) {
            Node curHead = head.getValue();
            Node curTail = tail.getValue();
            Node headNext = curHead.next.getValue();
            if (curHead == curTail) {
                if (headNext == null)
                    return Integer.MIN_VALUE;
                //try force
                tail.compareAndSet(curTail, headNext);
            } else {
                if (head.compareAndSet(curHead, headNext)) {
                    return headNext.x;
                }
            }
        }
    }

    @Override
    public int peek() { // peek lazy, don't force tail
        Node curHead = head.getValue();
        Node next = curHead.next.getValue();
        if (next == null) {
            return Integer.MIN_VALUE;
        }
        return next.x;
    }


    private class Node {
        final int x;
        AtomicRef<Node> next;

        Node(int x) {
            this.x = x;
            this.next = new AtomicRef<>(null);
        }
    }
}