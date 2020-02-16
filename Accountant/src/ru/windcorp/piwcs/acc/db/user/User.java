/**
 * Copyright (C) 2019 OLEGSHA
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
package ru.windcorp.piwcs.acc.db.user;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import ru.windcorp.piwcs.acc.Accountant;
import ru.windcorp.piwcs.acc.Agent;
import ru.windcorp.piwcs.acc.Agent.AccessLevel;
import ru.windcorp.piwcs.acc.db.DatabaseEntry;
import ru.windcorp.piwcs.acc.db.Field;
import ru.windcorp.piwcs.acc.db.FieldManager;
import ru.windcorp.piwcs.acc.db.settlement.Settlement;

public class User extends DatabaseEntry {
	
	private final FieldManager manager = new FieldManager();

	private final Field<String> username =
			manager.newField(String.class).initial(null).name("username");
	
	private final Field<String> nickname =
			manager.newField(String.class).optionalNull().name("nickname");
	private final Field<String> realName =
			manager.newField(String.class).optionalNull().name("real-name");
	
	private final Field<ZonedDateTime> registered =
			manager.newField(ZonedDateTime.class).initial(null).name("registered");
	private final Field<ZonedDateTime> joined =
			manager.newField(ZonedDateTime.class).optionalNull().name("joined");
	private final Field<ZonedDateTime> lastSeen =
			manager.newField(ZonedDateTime.class).optionalNull().name("last-seen");
	
	private final Field<String> invitor =
			manager.newField(String.class).optionalNull().name("invitor");
	
	private final Field<AccessLevel> accessLevel =
			manager.newField(AccessLevel.class)
			.optional(AccessLevel.PLAYER)
			.ioReader(Agent.AccessLevel::valueOf, Agent.AccessLevel::name)
			.name("access-level");
	
	private final Field<String> settlement =
			manager.newField(String.class).optionalNull().name("settlement");
	
	private final Field<ContactRecordSet> contacts =
			manager.newField(ContactRecordSet.class).initial(new ContactRecordSet()).name("contacts");
	
	private final Field<String> comment =
			manager.newField(String.class).optionalNull().name("comments");
	
	protected User() {
		
	}
	
	@Override
	public String getDatabaseId() {
		return getUsername();
	}
	
	@Override
	public String getFileName() {
		return getUsername();
	}
	
	/**
	 * @see ru.windcorp.piwcs.acc.db.DatabaseEntry#getFieldManager()
	 */
	@Override
	public FieldManager getFieldManager() {
		return manager;
	}
	
	public static User load(Path file) throws IOException {
		try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			User user = new User();
			user.manager.load(reader, file.toString());
			return user;
		}
	}
	
	public void save(Path file) throws IOException {
		try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
			this.manager.save(writer);
		}
	}
	
	static User create(String username, ZonedDateTime registerDate) {
		User user = new User();
		user.username.set(username);
		user.registered.set(registerDate);
		return user;
	}
	
	public String getUsername() {
		return this.username.get();
	}
	
	public String getNickname() {
		return this.nickname.get();
	}
	
	public void setNickname(String nickname) {
		this.nickname.set("".equals(nickname) ? null : nickname);
	}
	
	public String getDisplayName() {
		String nickname = getNickname();
		
		if (nickname == null) {
			return getUsername();
		} else {
			return nickname;
		}
	}
	
	public String getRealName() {
		return this.realName.get();
	}
	
	public void setRealName(String realName) {
		this.realName.set(realName);
	}
	
	public ZonedDateTime getRegisterDate() {
		return this.registered.get();
	}
	
	public ZonedDateTime getJoinDate() {
		return this.joined.get();
	}
	
	public void setJoinDate(ZonedDateTime joinDate) {
		this.joined.set(joinDate);
	}
	
	public ZonedDateTime getLastSeenDate() {
		return this.lastSeen.get();
	}
	
	public void setLastSeenDate(ZonedDateTime lastSeenDate) {
		this.lastSeen.set(lastSeenDate);
	}
	
	public AccessLevel getAccessLevel() {
		return this.accessLevel.get();
	}
	
	public void setAccessLevel(AccessLevel lvl) {
		this.accessLevel.set(lvl);
	}
	
	public String getInvitorUsername() {
		return this.invitor.get();
	}
	
	public User getInvitor() {
		return Accountant.getUsers().get(getInvitorUsername());
	}

	public void setInvitor(User invitor) {
		this.invitor.set(invitor != null ? invitor.getDatabaseId() : null);
	}
	
	public String getSettlementId() {
		return this.settlement.get();
	}
	
	public Settlement getSettlement() {
		return Accountant.getSettlements().get(getSettlementId());
	}

	public void setSettlement(Settlement settlement) {
		this.settlement.set(settlement != null ? settlement.getDatabaseId() : null);
	}
	
	public ContactRecordSet getContacts() {
		return this.contacts.get();
	}
	
	public String getComment() {
		return this.comment.get();
	}
	
	public void setComment(String comment) {
		this.comment.set(comment);
	}
	
	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return getAccessLevel() + " " + getUsername();
	}
	
}
