import org.jspecify.annotations.*;

@DefaultNotNull
public class Simple {
    // PSometimesNull(_)
    @Nullable public String field = null;

    // PSometimesNull(_), PNonNull(x), PUnknownNull(y)
    @Nullable
    public String foo(String x, @NullnessUnknown CharSequence y) {
        return "";
    }

    // PNonNull(_)
    @NotNull
    public String bar() {
        return "";
    }
}
