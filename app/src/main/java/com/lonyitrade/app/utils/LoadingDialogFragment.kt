package com.lonyitrade.app.utils

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.lonyitrade.app.R

class LoadingDialogFragment : DialogFragment() {

    companion object {
        const val TAG = "LoadingDialog"
        private const val ARG_MESSAGE = "message"

        fun newInstance(message: String = "Processing..."): LoadingDialogFragment {
            val fragment = LoadingDialogFragment()
            val args = Bundle().apply {
                putString(ARG_MESSAGE, message)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_loading, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val message = arguments?.getString(ARG_MESSAGE) ?: "Processing..."
        view.findViewById<TextView>(R.id.loadingMessageTextView).text = message
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        isCancelable = false
    }
}