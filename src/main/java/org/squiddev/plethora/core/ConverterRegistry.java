package org.squiddev.plethora.core;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.squiddev.plethora.api.converter.IConverter;
import org.squiddev.plethora.api.converter.IConverterRegistry;
import org.squiddev.plethora.api.reference.IReference;
import org.squiddev.plethora.core.collections.ClassIteratorIterable;

import javax.annotation.Nonnull;
import java.util.*;

public class ConverterRegistry implements IConverterRegistry {
	public static final ConverterRegistry instance = new ConverterRegistry();

	private final List<TargetedRegisteredValue<? extends IConverter<?, ?>>> all = new ArrayList<>();
	private final Multimap<Class<?>, IConverter<?, ?>> converters = MultimapBuilder.hashKeys().hashSetValues().build();

	void registerConverter(@Nonnull TargetedRegisteredValue<? extends IConverter<?, ?>> converter) {
		Objects.requireNonNull(converter, "converter cannot be null");
		all.add(converter);
	}

	void build() {
		TargetedRegisteredValue.buildCache(all, converters);
	}

	@Nonnull
	@Override
	@SuppressWarnings("unchecked")
	public Iterable<Object> convertAll(@Nonnull final Object input) {
		Objects.requireNonNull(input, "input cannot be null");

		return new Iterable<Object>() {
			@Nonnull
			@Override
			public Iterator<Object> iterator() {
				return new ConverterIterator(input);
			}
		};
	}

	@SuppressWarnings("unchecked")
	public void extendConverted(@Nonnull List<String> keys, @Nonnull List<Object> values, @Nonnull List<Object> references, int startPoint) {
		Objects.requireNonNull(keys, "keys cannot be null");
		Objects.requireNonNull(values, "values cannot be null");
		Objects.requireNonNull(references, "references in cannot be null");

		if (keys.size() != values.size()) throw new IllegalStateException("lists must be of the same size");
		if (keys.size() != references.size()) throw new IllegalStateException("lists must be of the same size");

		Object2IntMap<Object> positions = new Object2IntOpenHashMap<>();
		positions.defaultReturnValue(-1);
		for (int i = 0; i < values.size(); i++) positions.put(values.get(i), i);

		for (int i = startPoint; i < values.size(); i++) {
			Object target = values.get(i);

			Class<?> initial = target.getClass();
			for (Class<?> klass : new ClassIteratorIterable(initial)) {
				for (IConverter<?, ?> converter : converters.get(klass)) {
					Object converted = ((IConverter<Object, Object>) converter).convert(target);
					if (converted == null) continue;

					int existing = positions.getInt(converted);
					if (existing == positions.defaultReturnValue()) {
						positions.put(converted, keys.size());
						keys.add(keys.get(i));
						values.add(converted);

						boolean isConstant = converter.isConstant();
						if (isConstant) {
							Object reference = references.get(i);
							if (reference instanceof ConverterReference) {
								isConstant = false;
							} else if (reference instanceof IReference) {
								isConstant = ((IReference) reference).isConstant();
							}
						}

						references.add(isConstant ? converted : new ConverterReference(i, klass, converter));
					} else if (requiresInsertion(keys, values, existing, keys.get(i), converted)) {
						positions.put(converted, keys.size());
						keys.add(keys.get(i));
						values.add(converted);

						Object reference = references.get(existing);
						references.add(reference instanceof ConverterReference && ((ConverterReference) reference).isIdentity()
							? reference : ConverterReference.identity(existing));
					}
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	public void extendConverted(@Nonnull List<String> keys, @Nonnull List<Object> values, int startPoint) {
		Objects.requireNonNull(keys, "keys cannot be null");
		Objects.requireNonNull(values, "values cannot be null");

		if (keys.size() != values.size()) throw new IllegalStateException("lists must be of the same size");

		Object2IntMap<Object> positions = new Object2IntOpenHashMap<>();
		positions.defaultReturnValue(-1);
		for (int i = 0; i < values.size(); i++) positions.put(values.get(i), i);

		for (int i = startPoint; i < values.size(); i++) {
			Object target = values.get(i);

			Class<?> initial = target.getClass();
			for (Class<?> klass : new ClassIteratorIterable(initial)) {
				for (IConverter<?, ?> converter : converters.get(klass)) {
					Object converted = ((IConverter<Object, Object>) converter).convert(target);
					if (converted == null) continue;

					int existing = positions.getInt(converted);
					if (existing == positions.defaultReturnValue() || requiresInsertion(keys, values, existing, keys.get(i), converted)) {
						positions.put(converted, keys.size());
						keys.add(keys.get(i));
						values.add(converted);
					}
				}
			}
		}
	}

	private static boolean requiresInsertion(List<String> keys, List<Object> values, int existing, String key, Object value) {
		for (int i = existing; i >= 0; i--) {
			if (values.get(i).equals(value) && keys.get(i).equals(key)) {
				return false;
			}
		}
		return true;
	}

	private class ConverterIterator implements Iterator<Object> {
		private final Set<Object> allConverted = new HashSet<>();
		private final Queue<Object> queue = new ArrayDeque<>();

		ConverterIterator(Object input) {
			allConverted.add(input);
			queue.offer(input);
		}

		@Override
		public boolean hasNext() {
			return !queue.isEmpty();
		}

		@Override
		@SuppressWarnings("unchecked")
		public Object next() {
			Object next = queue.remove();

			Class<?> initial = next.getClass();
			for (Class<?> klass : new ClassIteratorIterable(initial)) {
				for (IConverter<?, ?> converter : converters.get(klass)) {
					Object converted = ((IConverter<Object, Object>) converter).convert(next);
					if (converted != null && allConverted.add(converted)) {
						queue.offer(converted);
					}
				}
			}

			return next;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException("remove");
		}
	}
}
