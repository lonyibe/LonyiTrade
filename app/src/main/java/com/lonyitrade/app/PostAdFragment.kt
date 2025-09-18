package com.lonyitrade.app

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.Switch
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.lonyitrade.app.data.models.Ad
import com.lonyitrade.app.utils.SessionManager
import com.lonyitrade.app.viewmodels.SharedViewModel
import com.lonyitrade.app.api.ApiClient
import com.lonyitrade.app.data.models.AdRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts

class PostAdFragment : Fragment() {

    private lateinit var sessionManager: SessionManager
    private var selectedPhotoUri: Uri? = null

    private val sharedViewModel: SharedViewModel by activityViewModels()

    // View variables for the sell form
    private lateinit var sellFormLayout: View
    private lateinit var titleEditText: EditText
    private lateinit var descriptionEditText: EditText
    private lateinit var priceEditText: EditText
    private lateinit var sellConditionRadioGroup: RadioGroup
    private lateinit var sellDistrictEditText: EditText

    // View variables for the buy form
    private lateinit var buyFormLayout: View
    private lateinit var itemTitleEditText: EditText
    private lateinit var itemDescriptionEditText: EditText
    private lateinit var budgetEditText: EditText
    private lateinit var buyConditionRadioGroup: RadioGroup
    private lateinit var buyDistrictEditText: EditText
    private lateinit var buyContactPhoneEditText: EditText
    private lateinit var buyWhatsappSwitch: Switch

    // Re-added the ActivityResultLauncher to handle image selection
    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            // Take a persistent URI permission so the app can access the image later
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
            requireContext().contentResolver.takePersistableUriPermission(uri, takeFlags)

            selectedPhotoUri = uri
            view?.findViewById<ImageView>(R.id.photo1ImageView)?.setImageURI(uri)
            Toast.makeText(requireContext(), "Photo uploaded!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_post_ad, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())

        // Initialize view variables for both forms
        val tradeTypeRadioGroup = view.findViewById<RadioGroup>(R.id.tradeTypeRadioGroup)
        sellFormLayout = view.findViewById(R.id.sell_form_layout)
        buyFormLayout = view.findViewById(R.id.buy_form_layout)

        // Sell form fields
        titleEditText = view.findViewById(R.id.titleEditText)
        descriptionEditText = view.findViewById(R.id.descriptionEditText)
        priceEditText = view.findViewById(R.id.priceEditText)
        sellConditionRadioGroup = view.findViewById(R.id.conditionRadioGroup)
        sellDistrictEditText = view.findViewById(R.id.districtEditText)

        // Buy form fields
        itemTitleEditText = view.findViewById(R.id.itemTitleEditText)
        itemDescriptionEditText = view.findViewById(R.id.itemDescriptionEditText)
        budgetEditText = view.findViewById(R.id.budgetEditText)
        buyConditionRadioGroup = view.findViewById(R.id.buy_conditionRadioGroup)
        buyDistrictEditText = view.findViewById(R.id.buy_districtEditText)
        buyContactPhoneEditText = view.findViewById(R.id.buyContactPhoneEditText)
        buyWhatsappSwitch = view.findViewById(R.id.buyWhatsappSwitch)

        val postAdButton = view.findViewById<Button>(R.id.postAdButton)
        val photo1ImageView = view.findViewById<ImageView>(R.id.photo1ImageView)

        // Set the click listener to trigger the image picker
        photo1ImageView.setOnClickListener {
            pickImage.launch("image/*")
        }

        tradeTypeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.tradeTypeSell -> {
                    sellFormLayout.visibility = View.VISIBLE
                    buyFormLayout.visibility = View.GONE
                }
                R.id.tradeTypeBuy -> {
                    sellFormLayout.visibility = View.GONE
                    buyFormLayout.visibility = View.VISIBLE
                }
            }
        }

        postAdButton.setOnClickListener {
            val token = sessionManager.fetchAuthToken()
            if (token.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Please log in to post an ad", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (sellFormLayout.visibility == View.VISIBLE) {
                val title = titleEditText.text.toString()
                val description = descriptionEditText.text.toString()
                val price = priceEditText.text.toString().toDoubleOrNull()
                val district = sellDistrictEditText.text.toString()
                // Your backend expects a category, so we will use a hardcoded value for now.
                val category = "Unspecified"

                if (title.isEmpty() || description.isEmpty() || price == null || district.isEmpty()) {
                    Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val newAdRequest = AdRequest(title, description, category, "for_sale", price, district)

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val response = ApiClient.apiService.postAdvert("Bearer $token", newAdRequest)
                        withContext(Dispatchers.Main) {
                            if (response.isSuccessful) {
                                Toast.makeText(requireContext(), "Selling ad posted successfully!", Toast.LENGTH_SHORT).show()
                                // Optionally, refresh the home feed or navigate back
                                // requireActivity().supportFragmentManager.popBackStack()
                            } else if (response.code() == 401) {
                                // Handle unauthorized access by logging the user out
                                sessionManager.logoutUser()
                                Toast.makeText(requireContext(), "Your session has expired. Please log in again.", Toast.LENGTH_LONG).show()
                                // Finish the current activity (MainAppActivity)
                                requireActivity().finish()
                            } else {
                                Toast.makeText(requireContext(), "Failed to post ad: ${response.code()}", Toast.LENGTH_LONG).show()
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(requireContext(), "Network error: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }

            } else if (buyFormLayout.visibility == View.VISIBLE) {
                // The backend does not have a separate endpoint for "buy" requests,
                // so we will not post a "buy" ad to the server for now.
                Toast.makeText(requireContext(), "Buying requests are not yet supported on the server.", Toast.LENGTH_LONG).show()
            }
        }
    }
}