/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.myaddressbook

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.util.Log
import android.view.*
import android.widget.TextView
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_contacts.*
import kotlinx.android.synthetic.main.contact_list_item.view.*
import kotlinx.android.synthetic.main.content_contacts.*
import org.json.JSONArray
import org.json.JSONException
import java.io.IOException
import java.util.*

class ContactsActivity : AppCompatActivity() {

    private lateinit var mContacts: ArrayList<Contact>
    private lateinit var mAdapter: ContactsAdapter

    private lateinit var mPrefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contacts)

        mPrefs = getPreferences(Context.MODE_PRIVATE)
        mContacts = loadContacts()
        mAdapter = ContactsAdapter(mContacts)

        setSupportActionBar(toolbar)
        setupRecyclerView()

        fab.setOnClickListener {
            fragmentManager.beginTransaction()
                    .add(InputContactDialogFragment.newInstance(), "TAG")
                    .commit()
        }
    }

    /**
     * Loads the contacts from SharedPreferences, and deserializes them into
     * a Contact data type using Gson.
     */
    private fun loadContacts(): ArrayList<Contact> {
        val contactSet = mPrefs.getStringSet(CONTACT_KEY, HashSet())
        return contactSet.mapTo(ArrayList()) { Gson().fromJson(it, Contact::class.java) }
    }

    /**
     * Saves the contacts to SharedPreferences by serializing them with Gson.
     */
    private fun saveContacts() {
        val editor = mPrefs.edit()
        editor.clear()
        val contactSet = mContacts.map { Gson().toJson(it) }.toSet()
        editor.putStringSet(CONTACT_KEY, contactSet)
        editor.apply()
    }

    /**
     * Sets up the RecyclerView: empty data set, item dividers, swipe to delete.
     */
    private fun setupRecyclerView() {
        contact_list.addItemDecoration(DividerItemDecoration(this,
                DividerItemDecoration.VERTICAL))
        contact_list.adapter = mAdapter

        // Implements swipe to delete
        val helper = ItemTouchHelper(
                object : ItemTouchHelper.SimpleCallback(0,
                        ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
                    override fun onMove(rV: RecyclerView,
                                        viewHolder: RecyclerView.ViewHolder,
                                        target: RecyclerView.ViewHolder): Boolean {
                        return false
                    }

                    override fun onSwiped(viewHolder: RecyclerView.ViewHolder,
                                          direction: Int) {
                        val position = viewHolder.adapterPosition
                        mContacts.removeAt(position)
                        mAdapter.notifyItemRemoved(position)
                        saveContacts()
                    }
                })

        helper.attachToRecyclerView(contact_list)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_contacts, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId

        when (id) {
            R.id.action_clear -> {
                clearContacts()
                return true
            }
            R.id.action_generate -> {
                generateContacts()
                return true
            }
            R.id.action_sort_first -> {
                mContacts.sortBy { it.firstName }
                mAdapter.notifyDataSetChanged()
                return true
            }
            R.id.action_sort_last -> {
                mContacts.sortBy { it.lastName }
                mAdapter.notifyDataSetChanged()
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }


    /**
     * Clears the contacts from SharedPreferences and the adapter, called from
     * the options menu.
     */
    private fun clearContacts() {
        mContacts.clear()
        saveContacts()
        mAdapter.notifyDataSetChanged()
    }

    /**
     * Generates mock contact data to populate the UI from a JSON file in the
     * assets directory, called from the options menu.
     */
    private fun generateContacts() {
        val contactsString = readContactJsonFile()
        try {
            val contactsJson = JSONArray(contactsString)
            for (i in 0 until contactsJson.length()) {
                val contactJson = contactsJson.getJSONObject(i)
                val contact = Contact(
                        contactJson.getString("first_name"),
                        contactJson.getString("last_name"),
                        contactJson.getString("email"))
                Log.d(TAG, "generateContacts: " + contact.toString())
                mContacts.add(contact)
            }

            mAdapter.notifyDataSetChanged()
            saveContacts()
        } catch (e: JSONException) {
            e.printStackTrace()
        }

    }

    /**
     * Reads a file from the assets directory and returns it as a string.
     *
     * @return The resulting string.
     */
    private fun readContactJsonFile(): String? {
        var contactsString: String? = null
        try {
            val inputStream = assets.open("mock_contacts.json")
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()

            contactsString = String(buffer)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return contactsString
    }

    private inner class ContactsAdapter internal constructor(
            private val mContacts: ArrayList<Contact>) :
            RecyclerView.Adapter<ContactsAdapter.ViewHolder>() {

        override fun onCreateViewHolder(
                parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.contact_list_item, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(
                holder: ViewHolder, position: Int) {
            val (firstName, lastName, email) = mContacts[position]
            val fullName = "$firstName $lastName"
            holder.nameLabel.text = fullName
            holder.emailLabel.text = email
        }

        override fun getItemCount(): Int {
            return mContacts.size
        }

        internal inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            var nameLabel: TextView = itemView.textview_name
            var emailLabel: TextView = itemView.textview_email

            init {
                itemView.setOnClickListener {
                    fragmentManager.beginTransaction()
                            .add(InputContactDialogFragment.newInstance(mContacts[adapterPosition], adapterPosition), "TAG")
                            .commit()
                }
            }
        }
    }

    fun updateContact(editedContact: Contact, contactPosition: Int) {
        mContacts[contactPosition] = editedContact
        mAdapter.notifyItemChanged(contactPosition)
        saveContacts()
    }

    fun addContact(newContact: Contact) {
        mContacts.add(newContact)
        mAdapter.notifyItemInserted(mContacts.size)
        saveContacts()
    }

    companion object {

        private val CONTACT_KEY = "contact_key"
        private val TAG = ContactsActivity::class.java.simpleName
    }
}
