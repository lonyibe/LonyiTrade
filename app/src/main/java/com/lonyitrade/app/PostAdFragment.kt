package com.lonyitrade.app

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.lonyitrade.app.api.ApiClient
import com.lonyitrade.app.data.models.AdRequest
import com.lonyitrade.app.data.models.RentalRequest
import com.lonyitrade.app.utils.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class PostAdFragment : Fragment() {

    private lateinit var sessionManager: SessionManager
    private var adPhotoUris = mutableListOf<Uri>()
    private var rentalPhotoUris = mutableListOf<Uri>()

    // UI Elements
    private lateinit var postAdButton: Button
    private lateinit var postAdProgressBar: ProgressBar
    private lateinit var sellBuyFormLayout: View
    private lateinit var rentalFormLayout: View
    private lateinit var listingTypeRadioGroup: RadioGroup

    // Sell/Buy Form Fields
    private lateinit var tradeTypeRadioGroup: RadioGroup
    private lateinit var titleEditText: EditText
    private lateinit var categorySpinner: Spinner
    private lateinit var descriptionEditText: EditText
    private lateinit var priceEditText: EditText
    private lateinit var priceTypeRadioGroup: RadioGroup
    private lateinit var sellConditionRadioGroup: RadioGroup
    private lateinit var districtEditText: EditText
    private lateinit var adImageContainer: LinearLayout
    private lateinit var addAdPhotoButton: ImageView

    // Rental Form Fields
    private lateinit var propertyTypeSpinner: Spinner
    private lateinit var rentalRoomsEditText: EditText
    private lateinit var rentalDescriptionEditText: EditText
    private lateinit var rentalRulesEditText: EditText
    private lateinit var rentalCityEditText: EditText
    private lateinit var rentalDistrictEditText: EditText
    private lateinit var rentalLocationDescEditText: EditText
    private lateinit var rentalRentEditText: EditText
    private lateinit var rentalPriceTypeRadioGroup: RadioGroup
    private lateinit var landlordTypeRadioGroup: RadioGroup
    private lateinit var landlordNameEditText: EditText
    private lateinit var landlordPhoneEditText: EditText
    private lateinit var landlordEmailEditText: EditText
    private lateinit var landlordWhatsappEditText: EditText
    private lateinit var rentalImageContainer: LinearLayout
    private lateinit var addRentalPhotoButton: ImageView


    private val pickAdImages = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri> ->
        if (adPhotoUris.size + uris.size > 5) {
            Toast.makeText(requireContext(), "You can select up to 5 photos only.", Toast.LENGTH_SHORT).show()
            return@registerForActivityResult
        }
        adPhotoUris.addAll(uris)
        displayAdImages()
    }

    private val pickRentalImages = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri> ->
        if (rentalPhotoUris.size + uris.size > 5) {
            Toast.makeText(requireContext(), "You can select up to 5 photos only.", Toast.LENGTH_SHORT).show()
            return@registerForActivityResult
        }
        rentalPhotoUris.addAll(uris)
        displayRentalImages()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_post_ad, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = SessionManager(requireContext())
        initializeViews(view)
        setupListeners()
        setupSpinners()
    }

    private fun initializeViews(view: View) {
        postAdButton = view.findViewById(R.id.postAdButton)
        postAdProgressBar = view.findViewById(R.id.postAdProgressBar)
        listingTypeRadioGroup = view.findViewById(R.id.listingTypeRadioGroup)
        sellBuyFormLayout = view.findViewById(R.id.sell_buy_form_layout)
        rentalFormLayout = view.findViewById(R.id.rental_form_layout)

        // Sell/Buy Form
        tradeTypeRadioGroup = view.findViewById(R.id.tradeTypeRadioGroup)
        titleEditText = view.findViewById(R.id.titleEditText)
        categorySpinner = view.findViewById(R.id.categorySpinner)
        descriptionEditText = view.findViewById(R.id.descriptionEditText)
        priceEditText = view.findViewById(R.id.priceEditText)
        priceTypeRadioGroup = view.findViewById(R.id.priceTypeRadioGroup)
        sellConditionRadioGroup = view.findViewById(R.id.conditionRadioGroup)
        districtEditText = view.findViewById(R.id.districtEditText)
        adImageContainer = view.findViewById(R.id.adImageContainer)
        addAdPhotoButton = view.findViewById(R.id.addAdPhotoButton)


        // Rental Form
        propertyTypeSpinner = view.findViewById(R.id.propertyTypeSpinner)
        rentalRoomsEditText = view.findViewById(R.id.rentalRoomsEditText)
        rentalDescriptionEditText = view.findViewById(R.id.rentalDescriptionEditText)
        rentalRulesEditText = view.findViewById(R.id.rentalRulesEditText)
        rentalCityEditText = view.findViewById(R.id.rentalCityEditText)
        rentalDistrictEditText = view.findViewById(R.id.rentalDistrictEditText)
        rentalLocationDescEditText = view.findViewById(R.id.rentalLocationDescEditText)
        rentalRentEditText = view.findViewById(R.id.rentalRentEditText)
        rentalPriceTypeRadioGroup = view.findViewById(R.id.rentalPriceTypeRadioGroup)
        landlordTypeRadioGroup = view.findViewById(R.id.landlordTypeRadioGroup)
        landlordNameEditText = view.findViewById(R.id.landlordNameEditText)
        landlordPhoneEditText = view.findViewById(R.id.landlordPhoneEditText)
        landlordEmailEditText = view.findViewById(R.id.landlordEmailEditText)
        landlordWhatsappEditText = view.findViewById(R.id.landlordWhatsappEditText)
        rentalImageContainer = view.findViewById(R.id.rentalImageContainer)
        addRentalPhotoButton = view.findViewById(R.id.addRentalPhotoButton)
    }

    private fun setupListeners() {
        listingTypeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.tradeTypeSell -> {
                    sellBuyFormLayout.visibility = View.VISIBLE
                    rentalFormLayout.visibility = View.GONE
                }
                R.id.rentalType -> {
                    sellBuyFormLayout.visibility = View.GONE
                    rentalFormLayout.visibility = View.VISIBLE
                }
            }
        }

        postAdButton.setOnClickListener { handlePostListing() }
        addAdPhotoButton.setOnClickListener { pickAdImages.launch("image/*") }
        addRentalPhotoButton.setOnClickListener { pickRentalImages.launch("image/*") }
    }

    private fun setupSpinners() {
        // Setup for Ad Category Spinner
        val categories = resources.getStringArray(R.array.categories)
        val categoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = categoryAdapter

        // Setup for Rental Property Type Spinner
        val propertyTypes = arrayOf("House", "Apartment", "Studio", "Condo", "Duplex", "Hostel", "Other")
        val propertyTypeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, propertyTypes)
        propertyTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        propertyTypeSpinner.adapter = propertyTypeAdapter
    }

    private fun handlePostListing() {
        val token = sessionManager.fetchAuthToken()
        if (token == null) {
            Toast.makeText(requireContext(), "You must be logged in to post a listing.", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)

        when (listingTypeRadioGroup.checkedRadioButtonId) {
            R.id.tradeTypeSell -> {
                val adRequest = createAdRequest()
                if (adRequest != null) {
                    postAd(token, adRequest)
                } else {
                    showLoading(false)
                }
            }
            R.id.rentalType -> {
                val rentalRequest = createRentalRequest()
                if (rentalRequest != null) {
                    postRental(token, rentalRequest)
                } else {
                    showLoading(false)
                }
            }
        }
    }

    private fun navigateToHome() {
        // Navigate back to the home fragment after posting
        activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation)?.selectedItemId = R.id.nav_home
    }

    // --- Ad Logic ---
    private fun createAdRequest(): AdRequest? {
        val title = titleEditText.text.toString().trim()
        val description = descriptionEditText.text.toString().trim()
        val price = priceEditText.text.toString().toDoubleOrNull()
        val district = districtEditText.text.toString().trim()
        val category = categorySpinner.selectedItem.toString()
        val priceType = view?.findViewById<RadioButton>(priceTypeRadioGroup.checkedRadioButtonId)?.text.toString()
        val condition = view?.findViewById<RadioButton>(sellConditionRadioGroup.checkedRadioButtonId)?.text.toString()
        val adType = if (view?.findViewById<RadioButton>(R.id.tradeTypeSellItem)?.isChecked == true) "for_sale" else "wanted"

        if (title.isEmpty() || description.isEmpty() || price == null || district.isEmpty() || category.isEmpty() || priceType.isNullOrEmpty() || condition.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Please fill all fields for the item", Toast.LENGTH_SHORT).show()
            return null
        }
        return AdRequest(title, description, category, adType, price, priceType, district, condition)
    }

    private fun postAd(token: String, adRequest: AdRequest) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.apiService.postAdvert("Bearer $token", adRequest)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val ad = response.body()
                        if (ad?.id != null && adPhotoUris.isNotEmpty()) {
                            uploadAdPhotos(token, ad.id, adPhotoUris)
                        } else {
                            showLoading(false)
                            Toast.makeText(requireContext(), "Ad posted successfully!", Toast.LENGTH_SHORT).show()
                            navigateToHome()
                        }
                    } else {
                        showLoading(false)
                        Toast.makeText(requireContext(), "Failed to post ad", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun uploadAdPhotos(token: String, adId: String, uris: List<Uri>) {
        val parts = uris.mapNotNull { uri ->
            getTempFileFromUri(uri)?.let { file ->
                val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                MultipartBody.Part.createFormData("photos", file.name, requestFile)
            }
        }

        if (parts.isNotEmpty()) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    ApiClient.apiService.uploadAdPhotos("Bearer $token", adId, parts)
                    withContext(Dispatchers.Main) {
                        showLoading(false)
                        Toast.makeText(requireContext(), "Ad and photos posted successfully!", Toast.LENGTH_SHORT).show()
                        navigateToHome()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        showLoading(false)
                        Toast.makeText(requireContext(), "Photo upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // --- Rental Logic ---
    private fun createRentalRequest(): RentalRequest? {
        val propertyType = propertyTypeSpinner.selectedItem.toString().trim()
        val city = rentalCityEditText.text.toString().trim()
        val district = rentalDistrictEditText.text.toString().trim()
        val rooms = rentalRoomsEditText.text.toString().toIntOrNull()
        val locationDesc = rentalLocationDescEditText.text.toString().trim()
        val rent = rentalRentEditText.text.toString().toDoubleOrNull()
        val priceType = view?.findViewById<RadioButton>(rentalPriceTypeRadioGroup.checkedRadioButtonId)?.text.toString().trim()
        val rules = rentalRulesEditText.text.toString().trim()
        val description = rentalDescriptionEditText.text.toString().trim()
        val landlordName = landlordNameEditText.text.toString().trim()
        val landlordPhone = landlordPhoneEditText.text.toString().trim()
        val landlordEmail = landlordEmailEditText.text.toString().trim()
        val landlordWhatsapp = landlordWhatsappEditText.text.toString().trim().ifEmpty { null }

        // --- Start of The Fix ---
        val landlordType = when (landlordTypeRadioGroup.checkedRadioButtonId) {
            R.id.landlordTypeLandlord -> "Landlord" // Assumes this ID exists in your layout
            R.id.landlordTypeAgent -> "Agent"       // Assumes this ID exists in your layout
            else -> {
                val selectedRadioButton = view?.findViewById<RadioButton>(landlordTypeRadioGroup.checkedRadioButtonId)
                if (selectedRadioButton?.text.toString().contains("Landlord", ignoreCase = true)) {
                    "Landlord"
                } else if (selectedRadioButton?.text.toString().contains("Agent", ignoreCase = true)) {
                    "Agent"
                } else {
                    ""
                }
            }
        }
        // --- End of The Fix ---

        if (propertyType.isEmpty() || city.isEmpty() || district.isEmpty() || rooms == null || rent == null || priceType.isEmpty() || rules.isEmpty() || description.isEmpty() || landlordName.isEmpty() || landlordPhone.isEmpty() || landlordType.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill in all required fields for the rental listing.", Toast.LENGTH_LONG).show()
            return null
        }

        return RentalRequest(
            property_type = propertyType,
            city = city,
            district = district,
            rooms = rooms,
            location_description = locationDesc,
            monthly_rent = rent,
            price_type = priceType,
            rules = rules,
            description = description,
            landlord_name = landlordName,
            landlord_phone = landlordPhone,
            landlord_email = landlordEmail,
            landlord_whatsapp = landlordWhatsapp,
            landlord_type = landlordType
        )
    }

    private fun postRental(token: String, rentalRequest: RentalRequest) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.apiService.postRental("Bearer $token", rentalRequest)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val rental = response.body()
                        if (rental?.id != null && rentalPhotoUris.isNotEmpty()) {
                            uploadRentalPhotos(token, rental.id, rentalPhotoUris)
                        } else {
                            showLoading(false)
                            Toast.makeText(requireContext(), "Rental posted successfully!", Toast.LENGTH_SHORT).show()
                            navigateToHome()
                        }
                    } else {
                        showLoading(false)
                        Toast.makeText(requireContext(), "Failed to post rental", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun uploadRentalPhotos(token: String, rentalId: String, uris: List<Uri>) {
        val parts = uris.mapNotNull { uri ->
            getTempFileFromUri(uri)?.let { file ->
                val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                MultipartBody.Part.createFormData("photos", file.name, requestFile)
            }
        }

        if (parts.isNotEmpty()) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    ApiClient.apiService.uploadRentalPhotos("Bearer $token", rentalId, parts)
                    withContext(Dispatchers.Main) {
                        showLoading(false)
                        Toast.makeText(requireContext(), "Rental and photos posted successfully!", Toast.LENGTH_SHORT).show()
                        navigateToHome()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        showLoading(false)
                        Toast.makeText(requireContext(), "Photo upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun displayAdImages() {
        if (adImageContainer.childCount > 1) {
            adImageContainer.removeViews(0, adImageContainer.childCount - 1)
        }

        adPhotoUris.forEach { uri ->
            val imageView = ImageView(requireContext()).apply {
                layoutParams = ViewGroup.LayoutParams(160, 160)
                setImageURI(uri)
                scaleType = ImageView.ScaleType.CENTER_CROP
                setPadding(8, 0, 8, 0)
            }
            adImageContainer.addView(imageView, 0)
        }
    }

    private fun displayRentalImages() {
        // Clear all but the "add photo" button
        if (rentalImageContainer.childCount > 1) {
            rentalImageContainer.removeViews(0, rentalImageContainer.childCount - 1)
        }

        rentalPhotoUris.forEach { uri ->
            val imageView = ImageView(requireContext()).apply {
                layoutParams = ViewGroup.LayoutParams(160, 160)
                setImageURI(uri)
                scaleType = ImageView.ScaleType.CENTER_CROP
                setPadding(8, 0, 8, 0)
            }
            rentalImageContainer.addView(imageView, 0)
        }
    }

    // --- Utility Functions ---
    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            postAdButton.text = ""
            postAdProgressBar.visibility = View.VISIBLE
            postAdButton.isEnabled = false
        } else {
            postAdButton.text = "Post Listing"
            postAdProgressBar.visibility = View.GONE
            postAdButton.isEnabled = true
        }
    }

    private fun getTempFileFromUri(uri: Uri): File? {
        return try {
            val inputStream: InputStream? = requireContext().contentResolver.openInputStream(uri)
            val file = File(requireContext().cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            file
        } catch (e: Exception) {
            null
        }
    }
}