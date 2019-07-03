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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import ru.windcorp.jputil.SyntaxException;
import ru.windcorp.jputil.chars.StringUtil;
import ru.windcorp.jputil.chars.WordReader;
import ru.windcorp.jputil.cmd.AutoCommand;
import ru.windcorp.jputil.cmd.Command;
import ru.windcorp.jputil.cmd.CommandExceptions;
import ru.windcorp.jputil.cmd.CommandRegistry;
import ru.windcorp.jputil.cmd.CommandRunner;
import ru.windcorp.jputil.cmd.CommandSyntaxException;
import ru.windcorp.jputil.cmd.Complaint;
import ru.windcorp.jputil.cmd.Invocation;
import ru.windcorp.jputil.selectors.*;
import ru.windcorp.jputil.textui.TUITable;
import ru.windcorp.piwcs.acc.Accountant;

/**
 * @author Javapony
 *
 */
public class DatabaseCommand<T extends DatabaseEntry> extends CommandRegistry {
	
	private final Database<T> database;
	
	private final Map<CommandRunner, Collection<T>> selections = new WeakHashMap<>();
	private final Function<CommandRunner, Supplier<? extends CommandExceptions>> mustHaveSelection =
			runner -> getSelection(runner).isEmpty() ? () -> new Complaint(null, "Nothing selected") : null;
	
	private final SelectorSystem<T> selectorSystem;
	private final Object[] displayFields;

	/**
	 * @param names
	 * @param desc
	 * @param commands
	 */
	public DatabaseCommand(Database<T> database, String[] names, String desc, String[] displayFields, Command... commands) {
		super(names, desc, commands);
		
		this.database = database;
		
		this.displayFields = new Object[displayFields.length];
		System.arraycopy(displayFields, 0, this.displayFields, 0, displayFields.length);
		
		selectorSystem = new SelectorSystem<>();
		selectorSystem.setStackPrefix("stack");
		
		selectorSystem.add(new OperatorAnd("&&", "and"));
		selectorSystem.add(new OperatorExclude("\\", "exclude", "without"));
		selectorSystem.add(new OperatorNot("!", "not"));
		selectorSystem.add(new OperatorOr("||", "or"));
		selectorSystem.add(new OperatorXor("!=", "xor"));
		
		selectorSystem.add(new PredicateWrapper<>("all", entry -> true));
		selectorSystem.add(new PredicateWrapper<>("clear", entry -> false));
		selectorSystem.add(new FieldSelector(null, null));
		selectorSystem.add(new DatabaseIdSelector(null));
		
		add(new SelectCommand());
		add(new InfoCommand());
		add(new DeleteCommand());
		
		add(new ReloadCommand());
		add(new SaveCommand());
		
		add(new CommandRegistry(new String[] {"fields", "field", "f"}, "Operations on raw field values",
			AutoCommand.forMethod(this, "getValueCommand")// TODO make methods private after AutoCommand uses manual recursive getDeclaredMethod() here and in UserDatabaseCommand
					.name("get")
					.desc("Displays the value of field FIELD for all selected entries")
					.parser("<word FIELD>", null).setRunnerFilter(mustHaveSelection()),
			
			AutoCommand.forMethod(this, "setValueCommand")
					.name("set")
					.desc("Sets the value of field FIELD for all selected entries to VALUE")
					.parser("<word FIELD> <word VALUE>", null).setRunnerFilter(mustHaveSelection()),
		
			new ListFieldsCommand())
		);
	}
	
	public DatabaseCommand(Database<T> database, String name, String desc, String[] displayFields, Command... commands) {
		this(database, new String[] {name}, desc, displayFields, commands);
	}
	
	public Collection<T> getSelection(CommandRunner runner) {
		synchronized (selections) {
			Collection<T> selection = selections.get(runner);
			if (selection == null) {
				selection = new ArrayList<>();
				selections.put(runner, selection);
			}
			
			selection.removeIf(DatabaseEntry::isAbsent);
			return selection;
		}
	}
	
	public Function<CommandRunner, Supplier<? extends CommandExceptions>> mustHaveSelection() {
		return mustHaveSelection;
	}
	
	private class DatabaseIdSelector implements Selector<T> {
		
		private final String id;

		public DatabaseIdSelector(String id) {
			this.id = id;
		}

		/**
		 * @see java.util.function.Predicate#test(java.lang.Object)
		 */
		@Override
		public boolean test(T t) {
			return id.equalsIgnoreCase(t.getDatabaseId());
		}

		/**
		 * @see ru.windcorp.jputil.selectors.Selector#derive(java.lang.String)
		 */
		@Override
		public Selector<T> derive(String name) throws SyntaxException {
			return name.startsWith("$") ? null : new DatabaseIdSelector(name);
		}
		
	}
	
	private class FieldSelector implements Selector<T> {
		
		private final String field;
		private final String template;
		
		/**
		 * @param separator
		 * @param names
		 */
		public FieldSelector(String field, String template) {
			this.field = field;
			this.template = template;
		}

		/**
		 * @see java.util.function.Predicate#test(java.lang.Object)
		 */
		@Override
		public boolean test(T t) {
			return template.equalsIgnoreCase(t.getFieldManager().getField(field).save());
		}
		
		/**
		 * @see ru.windcorp.jputil.selectors.Selector#derive(java.lang.String)
		 */
		@Override
		public Selector<T> derive(String name) throws SyntaxException {
			if (!name.startsWith("$")) return null;
			
			int index = name.indexOf('=');
			return index < 0 ? null : new FieldSelector(name.substring(0, index), name.substring(index + 1));
		}
		
	}
	
	private class SelectCommand extends Command {
		public SelectCommand() {
			super(new String[] {"sel", "select"}, "[SELECTORS...]", "Selects entries or displays selection");
		}
		
		@Override
		public void run(Invocation inv) throws CommandExceptions {
			String query = inv.getArgs().trim();
			Collection<T> selection = getSelection(inv.getRunner());
			
			try {
				if (!query.isEmpty()) select(query, inv, selection);
				displaySelection(inv.getRunner(), selection);
			} catch (SyntaxException e) {
				throw new CommandSyntaxException(inv, "Invalid selectors", e);
			}
		}

		/**
		 * @param query
		 * @param inv
		 * @param selection
		 * @throws SyntaxException 
		 */
		private void select(String query, Invocation inv, Collection<T> selection) throws SyntaxException {
			Predicate<? super T> test = selectorSystem.parse(new WordReader(query));
			selection.clear();
			database.stream().filter(test).forEach(selection::add);
		}

		/**
		 * @param runner
		 * @param selection 
		 */
		private void displaySelection(CommandRunner runner, Collection<T> selection) {
			if (selection.isEmpty()) {
				runner.respond("Nothing selected");
			} else if (selection.size() == 1) {
				runner.respond("Selected %s", selection.iterator().next());
			} else {
				
				TUITable response = new TUITable(displayFields);
				
				Object[] row;
				for (T entry : selection) {
					row = new Object[displayFields.length];
					FieldManager manager = entry.getFieldManager();
					
					for (int i = 0; i < row.length; ++i) {
						AbstractField field = manager.getField((String) displayFields[i]);
						row[i] = field.save();
					}
					
					response.addRow(row);
				}
				
				runner.respond("%d entries selected:", selection.size());
				runner.respond(response.toString());
				
			}
		}
	}

	private class InfoCommand extends Command {

		/**
		 * @param names
		 * @param syntax
		 * @param desc
		 */
		protected InfoCommand() {
			super(new String[] {"info", "details"}, "", "Displays all field values of selected entries");
			setRunnerFilter(mustHaveSelection());
		}

		/**
		 * @see ru.windcorp.jputil.cmd.Command#run(ru.windcorp.jputil.cmd.Invocation)
		 */
		@Override
		public void run(Invocation inv) throws CommandExceptions {
			Collection<T> selection = getSelection(inv.getRunner());
			
			Iterator<T> iterator = selection.iterator();
			T element = iterator.next();
			
			Object[] headers; {
				Collection<AbstractField> fields = element.getFieldManager().getFields();
				headers = new Object[fields.size()];
				int i = 0;
				for (AbstractField f : fields) headers[i++] = f.getName();
			}
			
			TUITable table = new TUITable(headers);
			
			while (true) {
				Object[] row = new Object[table.getColumns()];
				
				int i = 0;
				for (AbstractField field : element.getFieldManager().getFields())
					row[i++] = field.save();
				
				table.addRow(row);
				
				if (!iterator.hasNext()) break;
				element = iterator.next();
			}
			
			inv.getRunner().respond(table);
		}
		
	}
	
	private class DeleteCommand extends Command {

		protected DeleteCommand() {
			super(new String[] {"del", "delete", "rem", "remove"}, "[\"y\"]", "Deletes selected entries");
			setRunnerFilter(mustHaveSelection());
		}
		
		/**
		 * @see ru.windcorp.jputil.cmd.Command#run(ru.windcorp.jputil.cmd.Invocation)
		 */
		@Override
		public void run(Invocation inv) throws CommandExceptions {
			String arg = inv.getArgs().trim();
			if (arg.equalsIgnoreCase("y")) {
				Collection<T> selection = getSelection(inv.getRunner());
				selection.forEach(database::remove);
				inv.getRunner().respond("Deleted %d entries", selection.size());
				selection.clear();
			} else if (arg.isEmpty()) {
				inv.getRunner().respond("Are you sure you want to delete %d entries?", getSelection(inv.getRunner()).size());
				inv.getRunner().respond("Review with \"sel\", confirm with \"del y\"");
			} else {
				throw new CommandSyntaxException(inv, "Expected no arguments or \"y\"");
			}
		}
		
	}
	
	private class ReloadCommand extends Command {

		protected ReloadCommand() {
			super(new String[] {"reload"}, "[\"y\"]", "Reloads database discarding unsaved changes");
		}
		
		/**
		 * @see ru.windcorp.jputil.cmd.Command#run(ru.windcorp.jputil.cmd.Invocation)
		 */
		@Override
		public void run(Invocation inv) throws CommandExceptions {
			String arg = inv.getArgs().trim();
			if (arg.equalsIgnoreCase("y")) {
				inv.getRunner().respond("Reloading database...");
				try {
					database.load();
					inv.getRunner().respond("Database reloaded %d entries", database.size());
				} catch (IOException e) {
					inv.getRunner().respond("Could not load database. Database not reloaded");
				}
			} else if (arg.isEmpty()) {
				inv.getRunner().respond("Are you sure you want to reload the database? Changes will be lost");
				inv.getRunner().respond("Confirm with \"reload y\"");
			} else {
				throw new CommandSyntaxException(inv, "Expected no arguments or \"y\"");
			}
		}
		
	}
	
	private class SaveCommand extends Command {
		
		public SaveCommand() {
			super(new String[] {"save"}, "", "Saves database");
		}
		
		/**
		 * @see ru.windcorp.jputil.cmd.Command#run(ru.windcorp.jputil.cmd.Invocation)
		 */
		@Override
		public void run(Invocation inv) throws CommandExceptions {
			CommandRunner runner = inv.getRunner();
			
			try {
				runner.respond("Saving database");
				database.save(runner);
				runner.respond("Database saved %d entries", database.size());
			} catch (IOException e) {
				runner.respond("Could not save database due to an IO problem");
				runner.reportException(e);
				Accountant.reportException(e, "Could not save database");
			}
		}
		
	}

	public void getValueCommand(Invocation inv, String fieldName) throws Complaint {
		CommandRunner runner = inv.getRunner();
		Collection<T> selection = getSelection(runner);
		
		Iterator<T> iterator = selection.iterator();
		T element = iterator.next();
		AbstractField templateField = element.getFieldManager().getField(fieldName);
		
		if (templateField == null) {
			throw new Complaint(inv, "No field with name " + fieldName + " exists");
		}
		
		if (iterator.hasNext()) {
			TUITable table = new TUITable("ID", templateField.getType() + " " + fieldName);
			
			while (true) {
				table.addRow(element.getDatabaseId(), element.getFieldManager().getField(fieldName).save());
				if (!iterator.hasNext()) break;
				element = iterator.next();
			}
			
			runner.respond(table.toString());
		} else {
			runner.respond(templateField.getType() + " " + element.getDatabaseId() + fieldName + " = " + templateField.save());
		}
	}
	
	public void setValueCommand(Invocation inv, String fieldName, String value) throws Complaint {
		CommandRunner runner = inv.getRunner();
		Collection<T> selection = getSelection(runner);
		
		Iterator<T> iterator = selection.iterator();
		T element = iterator.next();
		AbstractField templateField = element.getFieldManager().getField(fieldName);
		
		if (templateField == null) {
			throw new Complaint(inv, "No field with name " + fieldName + " exists");
		}
		
		try {
			if (iterator.hasNext()) {
				
				int entries = 0;
				while (true) {
					element.getFieldManager().getField(fieldName).load(value);
					entries++;
					if (!iterator.hasNext()) break;
					element = iterator.next();
				}
				
				runner.respond("Changed value of field %s to \"%s\" for %d entries", fieldName, value, entries);
			} else {
				templateField.load(value);
				runner.respond(templateField.getType() + " " + element.getDatabaseId() + fieldName + " := " + value);
			}
		} catch (IOException e) {
			runner.complain("Could not set value: %s", e.toString());
		}
	}
	
	private class ListFieldsCommand extends Command {
		/**
		 * 
		 */
		public ListFieldsCommand() {
			super(new String[] {"list"}, "", "Lists fields of entries of this database");
			setRunnerFilter(mustHaveSelection());
		}
		
		/**
		 * @see ru.windcorp.jputil.cmd.Command#run(ru.windcorp.jputil.cmd.Invocation)
		 */
		@Override
		public void run(Invocation inv) throws CommandExceptions {
			inv.getRunner().respond(
					StringUtil.iteratorToString(
							getSelection(inv.getRunner())
							.iterator()
							.next()
							.getFieldManager()
							.getFields()
							.stream()
							.map(AbstractField::toString)
							.iterator()
					)
			);
		}
	}
	
}
