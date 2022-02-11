import kotlinx.atomicfu.*

class AtomicArray<E>(size: Int, initialValue: E) {
    val a = atomicArrayOfNulls<Any>(size)

    init {
        for (i in 0 until size) a[i].value = initialValue
    }

    fun get(index: Int): E {
        while (true) {
            val el = a[index].value
            if (el is Description<*>) {
                el.complete()
            } else {
                return el as E
            }
        }
    }

    fun set(index: Int, value: E) {
        while (true) {
            val el = a[index].value
            if (el is Description<*>) {
                el.complete()
            } else if (a[index].compareAndSet(el, value)) {
                return
            }
        }
    }


    fun cas(index: Int, expected: E, update: E): Boolean {
        while (true) {
            val el = a[index].value
            if (el is Description<*>) {
                el.complete()
            } else if (el == expected) {
                if (a[index].compareAndSet(expected, update)) {
                    return true
                }
            } else{
                return false
            }
        }
    }

    fun cas2(
        index1: Int, expected1: E, update1: E,
        index2: Int, expected2: E, update2: E
    ): Boolean {

        if (index1 == index2) {
            return if (expected1 == expected2) {
                cas(index1, expected1, update2)
            } else {
                false
            }
        }

        val description = if (index1 < index2)
            DescriptionCASN(
                index1, expected1, update1,
                index2, expected2, update2
            ) else DescriptionCASN(
            index2, expected2, update2,
            index1, expected1, update1
        )

        while (true) {
            val first = a[description.index1].value
            if (first is Description<*>) {
                first.complete() // complite должен валидно работать, даже на отработанном дескрипторе
                continue
            } else {
                val expectedFirst = description.expected1
                if (first == expectedFirst) {
                    if (a[description.index1].compareAndSet(expectedFirst, description)) {
                        // операция началась
                        description.complete()
                        return description.outcome.value == Outcome.SUCCESS
                    }
                    // в a[_index1] могли запихнуть description или другой элемент. Поэтому повторяем.
                } else {
                    /// откатываем
//                    description.outcome.compareAndSet(Outcome.UNDECIDED, Outcome.FAIL)
//                    description.complete()
//                    return description.outcome.value == Outcome.SUCCESS
                    return false
                }
            }
        }
    }

    enum class Outcome { UNDECIDED, FAIL, SUCCESS }

    abstract class Description<E> {
        abstract fun complete()
    }

    inner class DescriptionCASN<E>(
        val index1: Int, val expected1: E, val update1: E,
        val index2: Int, val expected2: E, val update2: E,
        val outcome: AtomicRef<Outcome> = atomic(Outcome.UNDECIDED)
    ) : Description<E>() {


        // Где-то после второго состояния
        override fun complete() {
            while (true) {
                if (outcome.value == Outcome.FAIL) { // 8 | 9
                    rollback()
                    break;
                } else if (outcome.value == Outcome.SUCCESS) { // 4 | 5 | 6
                    force()
                    break;
                } else { // 2 | 3
                    val second = a[index2].value

                    if (second == this) { // 3
                        outcome.compareAndSet(Outcome.UNDECIDED, Outcome.SUCCESS)
                        continue
                    }
                    // во 2
                    if (second is Description<*>) {
                        second.complete()
                    } else {
                        if (second == expected2) {
                            val descriptionDCSS = DescriptionDCSS(index2, expected2, this)
                            if (a[index2].compareAndSet(expected2, descriptionDCSS))
                                descriptionDCSS.complete()
                        } else {
                            outcome.compareAndSet(Outcome.UNDECIDED, Outcome.FAIL)
                            continue
                        }
                    }
                }
            }
        }

        private fun rollback() {
            a[index1].compareAndSet(this, expected1)
            a[index2].compareAndSet(this, expected2)
        }

        private fun force() {
            if (a[index1].compareAndSet(this, update1));
                //System.out.println("1" + " " +  update1)
            if (a[index2].compareAndSet(this, update2));
                //System.out.println("2")
        }

    }

    inner class DescriptionDCSS<E>(
        val index: Int, val expected: E, val description: DescriptionCASN<E>
    ) : Description<E>() {

        override fun complete() {
            val update = if (description.outcome.value == Outcome.UNDECIDED) description else expected
            a[index].compareAndSet(this, update)
        }
    }
}
