/*
 * Copyright (C) 2017 Wiktor Nizio
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * If you like this program, consider donating bitcoin: bc1qncxh5xs6erq6w4qz3a7xl7f50agrgn3w58dsfp
 */

package pl.org.seva.navigator.main.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import pl.org.seva.navigator.contact.Contact
import pl.org.seva.navigator.main.init.instance

infix fun ContactDao.insert(contact: Contact) = insert(contact.toEntity()) //#infix_function,extension_function

infix fun ContactDao.delete(contact: Contact) = delete(contact.toEntity()) //#infix_function,extension_function

val contactDao by instance<ContactDao>() //#inference,property_delegation

@Dao
interface ContactDao {

    @Query("SELECT * FROM ${ContactsDatabase.TABLE_NAME}") //#string_template
    fun getAll(): List<Contact.Entity>

    @Insert
    fun insert(contact: Contact.Entity)

    @Delete
    fun delete(contact: Contact.Entity)

    @Query("DELETE FROM ${ContactsDatabase.TABLE_NAME}") //#string_template
    fun deleteAll(): Int
}
