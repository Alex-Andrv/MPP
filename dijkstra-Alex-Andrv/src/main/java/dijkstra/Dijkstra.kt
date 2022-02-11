package dijkstra

import java.lang.IllegalStateException
import java.util.*
import java.util.concurrent.Phaser
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.Comparator
import kotlin.concurrent.thread
import kotlin.random.Random.Default.nextInt

private val NODE_DISTANCE_COMPARATOR = Comparator<Node> { o1, o2 -> Integer.compare(o1!!.distance, o2!!.distance) }

private val COEFFICIENT = 2

// Returns `Integer.MAX_VALUE` if a path has not been found.
fun shortestPathParallel(start: Node) {
    val workers = Runtime.getRuntime().availableProcessors()
    // The distance to the start node is `0`
    start.distance = 0
    // Create a priority (by distance) queue and add the start node into it
    //val q = PriorityQueue(workers, NODE_DISTANCE_COMPARATOR) // TODO replace me with a multi-queue based PQ!
    val q = MultiQueue(workers, NODE_DISTANCE_COMPARATOR, COEFFICIENT)
    q.add(start)
    // Run worker threads and wait until the total work is done
    val onFinish = Phaser(workers + 1) // `arrive()` should be invoked at the end by each worker
    val activeNodes = AtomicInteger();
    activeNodes.incrementAndGet();
    repeat(workers) {
        thread {
            while (activeNodes.get() > 0) {
                // TODO Write the required algorithm here,
                // TODO break from this loop when there is no more node to process.
                // TODO Be careful, "empty queue" != "all nodes are processed".
                val cur: Node = //synchronized(q) {
                    q.poll() ?: continue
                //} ?: continue
                for (e in cur.outgoingEdges) {
                    var curDist = e.to.distance
                    var newDist = cur.distance + e.weight
                    while (curDist > newDist) {
                        if (e.to.casDistance(curDist, newDist)) {
                            activeNodes.incrementAndGet()
                            //synchronized(q) {
                            q.add(e.to)
                            //}
                            break;
                        }
                        curDist = e.to.distance
                        newDist = cur.distance + e.weight
                    }
                }
                activeNodes.decrementAndGet();
            }
            onFinish.arrive()
        }
    }
    onFinish.arriveAndAwaitAdvance()
}


class MultiQueue<T>(workers: Int, private val comparator: Comparator<T>, t: Int) {
    private val cntQueue = workers * t;
    private val queues = Array(cntQueue) { PriorityQueue(comparator) }
    private val locks = Array(cntQueue) { ReentrantLock() }

    fun add(element: T) {
        while (true) {
            var index = nextInd()
            val curQueue = queues[index]
            val curLock = locks[index]
            if (curLock.tryLock()) {
                try {
                    curQueue.add(element)
                    return
                } finally {
                    curLock.unlock()
                }
            }
        }
    }


    fun poll(): T? {
        while (true) {
            val firstInd = nextInd()
            val secondInd = nextInd()
            if (firstInd == secondInd)
                continue
            val firstQueue = queues[firstInd]
            val firstLock = locks[firstInd]
            val secondQueue = queues[secondInd]
            val secondLock = locks[secondInd]
            if (firstLock.tryLock()) {
                try {
                    if (secondLock.tryLock()) {
                        val firstPeek = firstQueue.peek()
                        val secondPeek = secondQueue.peek()
                        try {
                            return when {
                                    firstPeek == null && secondPeek == null -> null
                                    firstPeek != null && secondPeek == null -> firstQueue.poll()
                                    firstPeek == null && secondPeek != null -> secondQueue.poll()
                                    else -> if (comparator.compare(firstPeek, secondPeek) > 0) {
                                        secondQueue.poll()
                                    } else {
                                        firstQueue.poll()
                                    }
                                }
                        } finally {
                            secondLock.unlock()
                        }
                    }
                } finally {
                    firstLock.unlock()
                }
            }
        }
    }

//    fun poll(): T? {
//        while (true) {
//            var firstInd = nextInd()
//            val firstQueue = queues[firstInd]
//            val firstLock = locks[firstInd]
//            if (firstLock.tryLock()) {
//                try {
//                    var secondInd = nextInd()
//                    val secondQueue = queues[secondInd]
//                    val secondLock = locks[secondInd]
//                    if (secondInd != firstInd && secondLock.tryLock()) {
//                        try {
//                            val firstPeek = firstQueue.peek()
//                            val secondPeek = secondQueue.peek()
//                            return when {
//                                firstPeek == null && secondPeek == null -> null
//                                firstPeek != null && secondPeek == null -> firstQueue.poll()
//                                firstPeek == null && secondPeek != null -> secondQueue.poll()
//                                else -> if (comparator.compare(firstPeek, secondPeek) > 0) {
//                                    secondQueue.poll()
//                                } else {
//                                    firstQueue.poll()
//                                }
//                            }
//                        } finally {
//                            secondLock.unlock()
//                        }
//                    } else {
//                        return firstQueue.poll()
//                    }
//                } finally {
//                    firstLock.unlock()
//                }
//            }
//        }
//    }

    private fun nextInd(): Int {
        return ThreadLocalRandom.current().nextInt(0, cntQueue);
    }


}
