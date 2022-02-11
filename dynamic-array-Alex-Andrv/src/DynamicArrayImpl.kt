import kotlinx.atomicfu.*

class DynamicArrayImpl<E> : DynamicArray<E> {
    private val _size = atomic(0)
    private val core = atomic(Core<E>(INITIAL_CAPACITY))

    override fun get(index: Int): E {
        checkIndex(index)
        while (true) {
            val el = core.value.array[index].value
            if (el is Move) {
                helpCopy()
            } else if (el is Elem) {
                return el.el
            } else if (el == null) {
                throw IllegalStateException("Жопа")
            } else {
                throw IllegalStateException("Не может быть ")
            }
        }
    }

    private fun helpCopy() {
        val curCore = core.value
        if (curCore.size > _size.value) return
        // Если мы тут, значит:
        // 1) Идет фаза копирования из array -> next
        // 2) Фаза закончилась и в array лежат Move
        curCore.nextCore.compareAndSet(null, Core(curCore.size * 2))
        var index = -1
        while (++index < curCore.array.size) {
            var elFromNext = curCore.nextCore.value!!.array[index].value
            var el = curCore.array[index].value
            while (el is Elem) {
                //
                if (curCore.nextCore.value!!.array[index].compareAndSet(elFromNext, el))
                    if (curCore.array[index].compareAndSet(el, Move()))
                        break
                elFromNext = curCore.nextCore.value!!.array[index].value
                el = curCore.array[index].value
            }
        }
        core.compareAndSet(curCore, curCore.nextCore.value!!)
    }

    override fun put(index: Int, element: E) {
        checkIndex(index)
        while (true) {
            val el = core.value.array[index].value
            if (el is Move) {
                helpCopy()
            } else if (el is Elem) {
                if (core.value.array[index].compareAndSet(el, Elem(element)))
                    return
            } else if (el == null) {
                throw IllegalStateException("Жопа")
            } else {
                throw IllegalStateException("Не может быть ")
            }
        }
    }

    override fun pushBack(element: E) {
        while (true) {
            val curSize = _size.value
            val curCore = core.value
            if (curSize < curCore.size) {
                if (core.value.array[curSize].compareAndSet(null, Elem(element))) {
                    // interrupt и сразу не lock-free, надо форсить
                    _size.compareAndSet(curSize, curSize + 1)
                    return
                } else {
                    _size.compareAndSet(curSize, curSize + 1)
                }
            } else {
                helpCopy()
            }
        }
    }

    private fun checkIndex(index: Int) {
        if (index < 0 || index >= _size.value)
            throw IllegalArgumentException()
    }

    override val size: Int
        get() {
            return _size.value
        }

    private class Core<E>(
        val size: Int
    ) {
        val array: AtomicArray<State<E>?> = atomicArrayOfNulls(size)
        val nextCore: AtomicRef<Core<E>?> = atomic(null)
    }

    private interface State<E>

    private class Elem<E>(val el: E) : State<E>

    private class Move<E>() : State<E>
}

private const val INITIAL_CAPACITY = 1 // DO NOT CHANGE ME

