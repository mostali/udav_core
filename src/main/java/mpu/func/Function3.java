package mpu.func;

import java.util.Objects;
import java.util.function.Function;

@FunctionalInterface
public interface Function3<A, B, C, R> {

	R apply(A a, B b, C c);

	default <V> Function3<A, B, C, V> andThen(Function<? super R, ? extends V> after) {
		Objects.requireNonNull(after);
		return (A a, B b, C c) -> after.apply(apply(a, b, c));
	}
}