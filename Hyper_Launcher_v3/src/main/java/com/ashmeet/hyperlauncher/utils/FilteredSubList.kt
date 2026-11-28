package com.ashmeet.hyperlauncher.utils

class FilteredSubList<E>(motherList: Array<E>, filter: BasicPredicate<E>) : AbstractMutableList<E>() {

    private val mArrayList: ArrayList<E> = ArrayList()

    init {
        refresh(motherList, filter)
    }

    fun refresh(motherArray: Array<E>, filter: BasicPredicate<E>) {
        if (mArrayList.isNotEmpty()) mArrayList.clear()

        for (item in motherArray) {
            if (filter.test(item)) {
                mArrayList.add(item)
            }
        }

        mArrayList.trimToSize()
    }

    override val size: Int
        get() = mArrayList.size

    override fun iterator(): MutableIterator<E> {
        return mArrayList.iterator()
    }

    override fun remove(element: E): Boolean {
        return mArrayList.remove(element)
    }

    override fun removeAll(elements: Collection<E>): Boolean {
        return mArrayList.removeAll(elements.toSet())
    }

    override fun retainAll(elements: Collection<E>): Boolean {
        return mArrayList.retainAll(elements.toSet())
    }

    override fun clear() {
        mArrayList.clear()
    }

    override fun get(index: Int): E {
        return mArrayList[index]
    }

    override fun removeAt(index: Int): E {
        return mArrayList.removeAt(index)
    }

    override fun set(index: Int, element: E): E {
        return mArrayList.set(index, element)
    }

    override fun add(index: Int, element: E) {
        mArrayList.add(index, element)
    }

    override fun listIterator(): MutableListIterator<E> {
        return mArrayList.listIterator()
    }

    override fun listIterator(index: Int): MutableListIterator<E> {
        return mArrayList.listIterator(index)
    }

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<E> {
        return mArrayList.subList(fromIndex, toIndex)
    }

    fun interface BasicPredicate<E> {
        fun test(item: E): Boolean
    }
}
