package com.lonyitrade.app

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment

class MessagesFragment : Fragment(R.layout.fragment_messages) {

    // Define a companion object to create an instance with arguments
    companion object {
        private const val ARG_PHONE_NUMBER = "phone_number"

        fun newInstance(phoneNumber: String): MessagesFragment {
            val fragment = MessagesFragment()
            val args = Bundle()
            args.putString(ARG_PHONE_NUMBER, phoneNumber)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val messageTextView = view.findViewById<TextView>(R.id.messageTextView)

        // Retrieve the phone number from arguments
        val sellerPhoneNumber = arguments?.getString(ARG_PHONE_NUMBER)

        // Update the TextView to display a custom message or a default one
        if (sellerPhoneNumber != null) {
            messageTextView.text = "You are now chatting with the seller: $sellerPhoneNumber"
        } else {
            messageTextView.text = "Messages Page"
        }
    }
}