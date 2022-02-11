/**
 * @author : Alexandr Andreev
 */
public class Solution implements AtomicCounter {
    final Node root = new Node(0);
    final ThreadLocal<Node> last = ThreadLocal.withInitial(() -> root);


    public int getAndAdd(int x) {
        while(true) {
            Node curLast = last.get();
            int cur_x = curLast.x;
            int upd_x = cur_x + x;
            Node new_node = new Node(upd_x);
            last.set(curLast.next.decide(new_node));
            if (last.get() == new_node) {
                return cur_x;
            }
        }
    }

    // вам наверняка потребуется дополнительный класс
    private static class Node {
        final Consensus<Node> next;
        final int x;

        public Node(int x) {
            this.x = x;
            this.next = new Consensus<>();
        }
    }
}
