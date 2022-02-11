package linked_list_set;


import kotlinx.atomicfu.AtomicRef;

public class SetImpl implements Set {
    private class Node {
        AtomicRef<Node> next; // Атомарная переменная, которая атомарно хранить флаг remove и next
        // (флаг относится к this)
        int x;

        Node(int x, Node next) {
            this.next = new AtomicRef<>(next);
            this.x = x;
        }

        public Node() {
            this.next = new AtomicRef<>(null);
        }
    }

    private class RemNode extends Node {
        Node node;

        RemNode(Node node) {
            this.node = node;
        }
    }

    private class Window {
        Node cur, next;
    }

    private final Node head = new Node(Integer.MIN_VALUE, new Node(Integer.MAX_VALUE, null));

    /**
     * Returns the {@link Window}, where cur.x < x <= next.x
     */
    private Window findWindow(int x) {
        while (true) {
            boolean reTry = false;
            Window w = new Window();
            w.cur = head;                   //инвариант cur и next, не instanceof RemNode
            w.next = w.cur.next.getValue();
            while (w.next.x < x) {
                Node node = w.next.next.getValue();
                if (node instanceof RemNode) {
                    //if (!(w.next instanceof RemNode)) {
                    w.cur.next.compareAndSet(w.next, ((RemNode) node).node);
                    reTry = true;
                    break;
                    //}
                } else {
                    w.cur = w.next;
                    w.next = w.cur.next.getValue();
                    if (w.next instanceof RemNode) {
                        reTry = true;
                        break;
                    }
                }
            }
            if (!reTry) {
                Node node = w.next.next.getValue();
                if (node instanceof RemNode) {
                    //if (!(w.next instanceof RemNode)) {
                    w.cur.next.compareAndSet(w.next, ((RemNode) node).node);
                    continue;
                    //}
                }
                return w;
            }
        }
    }

    @Override
    public boolean add(int x) {
        while (true) {
            Window w = findWindow(x);
            if (w.next.x == x) { // Если получилось добежать, значит есть или его параллельно удаляли.
                return false;
            } else {
                Node node = new Node(x, w.next);
                //if (!(w.next instanceof RemNode)) {
                    if (w.cur.next.compareAndSet(w.next, node)) {
                        return true;
                    }
                //}
            }
        }
    }

    @Override
    public boolean remove(int x) {
        while (true) {
            Window w = findWindow(x);
            if (w.next.x != x) {
                return false;
            } else {
                Node node = w.next.next.getValue();
                if (!(node instanceof RemNode)) {
                    if (w.next.next.compareAndSet(node, new RemNode(node))) {
                        //if (!(w.next instanceof RemNode)) {
                        w.cur.next.compareAndSet(w.next, node);
                        //}
                        return true;
                    }
                }

            }
        }
    }


    @Override
    public boolean contains(int x) { // Если получилось добежать, значит есть или его параллельно удаляли.
        Window w = findWindow(x);
        return w.next.x == x;
    }
}