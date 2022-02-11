import java.util.concurrent.atomic.AtomicReference;

public class Solution implements Lock<Solution.Node> {
    private final Environment env;
    private final AtomicReference<Node> tail = new AtomicReference<>(null);

    // todo: необходимые поля (final, используем AtomicReference)

    public Solution(Environment env) {
        this.env = env;
    }

    @Override
    public Node lock() {
        Node my = new Node(); // сделали узел
        Node oldTail = tail.getAndSet(my);
        if (oldTail != null) {
            oldTail.next.getAndSet(my);
            while (!my.lock.get()) env.park();
        }
        return my; // вернули узел
    }

    @Override
    public void unlock(Node node) {
        if (node.next.get() == null) {
            if (tail.compareAndSet(node, null)) {
                return;
            } else {
                while (node.next.get() == null);
            }
        }
        node.next.get().lock.getAndSet(true);
        env.unpark(node.next.get().thread);
    }

    static class Node {
        final Thread thread = Thread.currentThread(); // запоминаем поток, которые создал узел
        final AtomicReference<Node> next = new AtomicReference<>(null);
        final AtomicReference<Boolean> lock = new AtomicReference<>(false);
        // todo: необходимые поля (final, используем AtomicReference)
    }

}
