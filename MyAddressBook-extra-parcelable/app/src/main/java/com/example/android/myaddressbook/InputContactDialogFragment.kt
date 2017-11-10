/*
 * Copyright 2017 Google Inc. / René de Groot
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

import android.app.DialogFragment
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import kotlinx.android.synthetic.main.input_contact_dialog.*
import kotlinx.android.synthetic.main.input_contact_dialog.view.*

class InputContactDialogFragment : DialogFragment(), TextWatcher {

    private var mFirstNameEdit: EditText? = null
    private var mLastNameEdit: EditText? = null
    private var mEmailEdit: EditText? = null
    private var mEntryValid = false
    private var mContact: Contact? = null
    private var mContactPosition: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments?.containsKey(ARG_CONTACT) == true) {
            mContact = arguments.getParcelable(ARG_CONTACT)
            mContactPosition = arguments.getInt(ARG_POSITION, -1)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            inflater.inflate(R.layout.input_contact_dialog, container, false)

    override fun onViewCreated(dialogView: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mFirstNameEdit = dialogView.edittext_firstname
        mLastNameEdit = dialogView.edittext_lastname
        mEmailEdit = dialogView.edittext_email

        // Listens to text changes to validate after each key press
        mFirstNameEdit?.addTextChangedListener(this)
        mLastNameEdit?.addTextChangedListener(this)
        mEmailEdit?.addTextChangedListener(this)

        // Checks if the user is editing an existing contact
        val editing = mContact != null

        val dialogTitle = if (editing)
            getString(R.string.edit_contact)
        else
            getString(R.string.new_contact)
        text_title.text = dialogTitle

        // If the contact is being edited, populates the EditText with the old
        // information
        if (editing) {
            val (firstName, lastName, email) = mContact!!
            mFirstNameEdit?.setText(firstName)
            mFirstNameEdit?.isEnabled = false
            mLastNameEdit?.setText(lastName)
            mLastNameEdit?.isEnabled = false
            mEmailEdit?.setText(email)
        }
        // Overrides the "Save" button press and check for valid input
        button_ok.setOnClickListener {
            // If input is valid, creates and saves the new contact,
            // or replaces it if the contact is being edited
            if (mEntryValid) {
                if (editing) {
                    val editedContact = mContact
                    editedContact?.email = mEmailEdit?.text.toString()
                    (activity as ContactsActivity).updateContact(editedContact!!, mContactPosition)
                } else {
                    val newContact = Contact(
                            mFirstNameEdit?.text.toString(),
                            mLastNameEdit?.text.toString(),
                            mEmailEdit?.text.toString()
                    )
                    (activity as ContactsActivity).addContact(newContact)
                }
                dismiss()
            } else {
                // Otherwise, shows an error Toast
                Toast.makeText(activity,
                        R.string.contact_not_valid,
                        Toast.LENGTH_SHORT).show()
            }
        }

        button_cancel.setOnClickListener { dismiss() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mFirstNameEdit = null
        mLastNameEdit = null
        mEmailEdit = null
    }

    /**
     * Override methods for the TextWatcher interface, used to validate user
     * input.
     */
    override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
    }

    override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
    }

    /**
     * Validates the user input when adding a new contact each time the test
     * is changed.
     *
     * @param editable The text that was changed. It is not used as you get the
     * text from member variables.
     */
    override fun afterTextChanged(editable: Editable) {
        val notEmpty: TextView.() -> Boolean = { text.isNotEmpty() }
        val isEmail: TextView.() -> Boolean = { Patterns.EMAIL_ADDRESS.matcher(text).matches() }

        mEntryValid = mFirstNameEdit!!.validateWith(validator = notEmpty) and
                mLastNameEdit!!.validateWith(validator = notEmpty) and
                mEmailEdit!!.validateWith(validator = isEmail)
    }

    companion object {
        const val ARG_CONTACT = "ARG_CONTACT"
        const val ARG_POSITION = "ARG_POSITION"

        @JvmStatic
        fun newInstance(contact: Contact, position: Int): InputContactDialogFragment {
            val fragment = InputContactDialogFragment()
            val bundle = Bundle()
            bundle.putParcelable(ARG_CONTACT, contact)
            bundle.putInt(ARG_POSITION, position)
            fragment.arguments = bundle
            return fragment
        }

        @JvmStatic
        fun newInstance(): InputContactDialogFragment = InputContactDialogFragment()
    }
}
