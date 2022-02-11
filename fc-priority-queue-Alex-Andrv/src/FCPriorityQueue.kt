import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.atomicArrayOfNulls
import java.util.*
import java.util.concurrent.ThreadLocalRandom

class FCPriorityQueue<E : Comparable<E>> {
    private val q = PriorityQueue<E>()
    private val lock = atomic(false)
    private val CNT_THREDS = Runtime.getRuntime().availableProcessors()
    private val SIZE = CNT_THREDS * 20
    private val operations = atomicArrayOfNulls<Any?>(SIZE)

    class Operation<E>(val op: () -> E?) {
        fun eval(): Result<E> {
            return Result(op())
        }
    }

    class Result<E>(val res: E?)

    private fun apply(operation: Operation<E>): E?{
        var localId = 0

        while (true) {
            localId = nextInt()
            if (operations[localId].compareAndSet(null, operation))
                break
        }

        while (true) {
            if (lock.compareAndSet(expect = false, update = true)) {
                try {
                    var i = -1
                    while (++i < operations.size) {
                        val op = operations[i].value
                        if (op is Operation<*>) {
                            operations[i].getAndSet(op.eval())
                        }
                    }
                    val res = operations[localId].getAndSet(null) as Result<E>
                    return res.res
                } finally {
                    lock.getAndSet(false)
                }
            } else {
                val res = operations[localId].value
                if (res is Result<*>) {
                    operations[localId].getAndSet(null)
                    return res.res as E?
                }
            }
        }
    }

    private fun nextInt(): Int {
        return ThreadLocalRandom.current().nextInt(SIZE)
    }


    /**
     * Retrieves the element with the highest priority
     * and returns it as the result of this function;
     * returns `null` if the queue is empty.
     */
    fun poll(): E? {
        return apply(Operation { q.poll() })

    }

    /**
     * Returns the element with the highest priority
     * or `null` if the queue is empty.
     */
    fun peek(): E? {
        return apply(Operation { q.peek() })
    }

    /**
     * Adds the specified element to the queue.
     */
    fun add(element: E) {
        apply(Operation { q.add(element); null })
    }
}
