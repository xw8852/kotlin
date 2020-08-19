// FILE: KtAnnotated.java

public interface KtAnnotated extends KtElement

// FILE: callElement.kt

class KtVisitor<D>

interface KtElement {
    fun <D> acceptChildren(visitor: KtVisitor<D>, data: D)
}


interface KtCallElement : KtElement

interface KtDeclaration : KtAnnotated