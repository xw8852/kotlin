interface A {
    fun foo()
}

class B : A {
    override fun foo() {}
}

class C(val b: B) : A by b

fun bar() {
    val b = B()
    val c = C(b)
    c.foo()
}

interface AA<T> {
    fun foo(x: T)
}

class BB<T>(val y: T) : AA<T> {
    override fun foo(x: T) {}
}

class CC(val b: BB<String>) : AA<String> by b

fun baz() {
    val b = BB("")
    val c = CC(b)
    c.foo("")
}