/*
 * PIWCS Accountant Plugin
 * Copyright (C) 2019  Javapony/OLEGSHA
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package ru.windcorp.piwcs.acc.db;

import java.util.AbstractList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

import ru.windcorp.jputil.iterators.FunctionIterator;

/**
 * @author Javapony
 *
 */
public class FieldListWrapper<T, E> extends AbstractList<E> {
	
	private final FieldList<Field<T>> src;
	private final Function<T, E> reader;
	private final Function<E, T> writer;

	protected FieldListWrapper(FieldList<Field<T>> src, Function<T, E> reader, Function<E, T> writer) {
		this.src = src;
		this.reader = reader;
		this.writer = writer;
	}
	
	public static <T, E> List<E> wrap(FieldList<Field<T>> src, Function<T, E> reader, Function<E, T> writer) {
		return Collections.synchronizedList(new FieldListWrapper<T, E>(src, reader, writer));
	}
	
	private static final Function<Object, Object> IDENTITY = element -> element;
	@SuppressWarnings("unchecked")
	public static <E> List<E> wrap(FieldList<Field<E>> src) {
		return wrap(src, (Function<E, E>) IDENTITY, (Function<E, E>) IDENTITY);
	}

	/**
	 * @see java.util.AbstractCollection#iterator()
	 */
	@Override
	public Iterator<E> iterator() {
		return new FunctionIterator<>(src.getFields().iterator(), reader.compose(Field::get));
	}

	/**
	 * @see java.util.AbstractCollection#size()
	 */
	@Override
	public int size() {
		return src.getFields().size();
	}

	/**
	 * @see java.util.AbstractList#get(int)
	 */
	@Override
	public E get(int index) {
		return reader.apply(src.getFields().get(index).get());
	}
	
	/**
	 * @see java.util.AbstractList#set(int, java.lang.Object)
	 */
	@Override
	public E set(int index, E element) {
		Field<T> field = src.getFields().get(index);
		E prev = reader.apply(field.get());
		field.set(writer.apply(element));
		return prev;
	}
	
	/**
	 * @see java.util.AbstractList#add(int, java.lang.Object)
	 */
	@Override
	public void add(int index, E element) {
		Field<T> field = src.newEntry();
		field.set(writer.apply(element));
		src.getFields().add(index, field);
	}

}
