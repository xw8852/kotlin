// !LANGUAGE: -AdditionalBuiltInsMembers
// SKIP_JDK6
// WITH_RUNTIME
// FULL_JDK

class A(val x: List<String>) : List<String> by x

fun box(): String {
    return A(listOf("OK"))[0]
}
