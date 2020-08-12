import kotlin.contracts.*

infix fun <E, R> ((E) -> R).returnsForEachOf(collection: Collection<E>)

inline fun <E, R> Collection<E>.forEach(block: (E) -> R) {
    contract {
        block returnsForEachOf this@forEach
    }
    for (element in this) block(element)
}

interface MutableNumberList<T : Number> : MutableList<T>
interface NumberList<out T : Number> : List<T>

fun <E> addTo(list: MutableList<E>, element: E){}
fun <E> getFrom(list: MutableList<E>) : E = list[0]
fun <E> getFrom(list: List<E>) : E = list[0]

fun testInv(list: MutableList<Number>) {
    addTo(list, 1)
    getFrom(list).toByte()

    fun correct() {
        list as MutableList<Int>

        getFrom(list).dec()

        addTo(list, 1)
        getFrom(list).toByte()
    }

    fun test() {
        list.forEach { it as Int }

        getFrom(list).dec()

        addTo(list, 1)
        getFrom(list).toByte()
    }
}



fun testCov(list: MutableList<out Number>) {
    <!INAPPLICABLE_CANDIDATE!>addTo<!>(list, 1)
    getFrom(list).toByte()

    fun correct() {
        list as MutableList<out Int>

        getFrom(list).dec()

        <!INAPPLICABLE_CANDIDATE!>addTo<!>(list, 1)
        getFrom(list).toByte()
    }

    fun test() {
        list.forEach { it as Int }

        getFrom(list).dec()

        <!INAPPLICABLE_CANDIDATE!>addTo<!>(list, 1)
        getFrom(list).toByte()
    }
}

fun testContr(list: MutableList<in Number>) {
    addTo(list, 1)
    getFrom(list).<!UNRESOLVED_REFERENCE!>toByte<!>()

    fun correct() {
        list as MutableList<in Int>

        addTo(list, 1)
        getFrom(list).<!UNRESOLVED_REFERENCE!>toByte<!>()
    }

    fun test() {
        list.forEach { it as Int }

        addTo(list, 1)
        getFrom(list).<!UNRESOLVED_REFERENCE!>toByte<!>()
    }
}

fun testStarInv(list: MutableNumberList<*>) {
    <!INAPPLICABLE_CANDIDATE!>addTo<!>(list, 1)
    getFrom(list).toByte()

    fun correct() {
        list as MutableNumberList<out Int>

        getFrom(list).dec()

        <!INAPPLICABLE_CANDIDATE!>addTo<!>(list, 1)
        getFrom(list).toByte()
    }

    fun test() {
        list.forEach { it as Int }

        getFrom(list).dec()

        <!INAPPLICABLE_CANDIDATE!>addTo<!>(list, 1)
        getFrom(list).toByte()
    }
}

fun testStarOut(list: NumberList<*>) {
    getFrom(list).toByte()

    fun correct() {
        list as NumberList<out Int>

        getFrom(list).dec()

        getFrom(list).toByte()
    }

    fun test() {
        list.forEach { it as Int }

        getFrom(list).dec()

        getFrom(list).toByte()
    }
}
