package com.lonyitrade.app

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
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
    private var itemPhotoUri: Uri? = null
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
    private lateinit var descriptionEditText: EditText
    private lateinit var priceEditText: EditText
    private lateinit var priceTypeRadioGroup: RadioGroup
    private lateinit var sellConditionRadioGroup: RadioGroup
    private lateinit var districtEditText: EditText

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


    private val pickItemImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            itemPhotoUri = it
            view?.findViewById<ImageView>(R.id.photo1ImageView)?.setImageURI(it)
        }
    }

    private val pickRentalImages = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri> ->
        if (rentalPhotoUris.size + uris.size > 10) {
            Toast.makeText(requireContext(), "You can select up to 10 photos only.", Toast.LENGTH_SHORT).show()
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
        setupPropertyTypeSpinner()
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
        descriptionEditText = view.findViewById(R.id.descriptionEditText)
        priceEditText = view.findViewById(R.id.priceEditText)
        priceTypeRadioGroup = view.findViewById(R.id.priceTypeRadioGroup)
        sellConditionRadioGroup = view.findViewById(R.id.conditionRadioGroup)
        districtEditText = view.findViewById(R.id.districtEditText)

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
        view?.findViewById<ImageView>(R.id.photo1ImageView)?.setOnClickListener { pickItemImage.launch("image/*") }
        addRentalPhotoButton.setOnClickListener { pickRentalImages.launch("image/*") }
    }

    private fun setupPropertyTypeSpinner() {
        val propertyTypes = arrayOf("House", "Apartment", "Studio", "Condo", "Duplex", "Hostel", "Other")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, propertyTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        propertyTypeSpinner.adapter = adapter
    }

    private fun handlePostListing() {
        val token = sessionManager.fetchAuthToken() ?: return
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

    // --- Ad Logic ---
    private fun createAdRequest(): AdRequest? {
        val title = titleEditText.text.toString()
        val description = descriptionEditText.text.toString()
        val price = priceEditText.text.toString().toDoubleOrNull()
        val district = districtEditText.text.toString()
        val priceType = view?.findViewById<RadioButton>(priceTypeRadioGroup.checkedRadioButtonId)?.text.toString()
        val condition = view?.findViewById<RadioButton>(sellConditionRadioGroup.checkedRadioButtonId)?.text.toString()
        val adType = if (view?.findViewById<RadioButton>(R.id.tradeTypeSellItem)?.isChecked == true) "for_sale" else "wanted"

        if (title.isEmpty() || description.isEmpty() || price == null || district.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill all fields for the item", Toast.LENGTH_SHORT).show()
            return null
        }
        return AdRequest(title, description, "Unspecified", adType, price, priceType, district, condition)
    }

    private fun postAd(token: String, adRequest: AdRequest) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.apiService.postAdvert("Bearer $token", adRequest)
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    if (response.isSuccessful) {
                        Toast.makeText(requireContext(), "Ad posted successfully!", Toast.LENGTH_SHORT).show()
                        // Optionally clear form or navigate away
                    } else {
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

    // --- Rental Logic ---
    private fun createRentalRequest(): RentalRequest? {
        val rooms = rentalRoomsEditText.text.toString().toIntOrNull()
        val rent = rentalRentEditText.text.toString().toDoubleOrNull()

        if(rooms == null || rent == null) {
            Toast.makeText(requireContext(), "Please enter valid numbers for rooms and rent", Toast.LENGTH_SHORT).show()
            return null
        }

        return RentalRequest(
            propertyType = propertyTypeSpinner.selectedItem.toString(),
            city = rentalCityEditText.text.toString(),
            district = rentalDistrictEditText.text.toString(),
            rooms = rooms,
            locationDescription = rentalLocationDescEditText.text.toString(),
            monthlyRent = rent,
            priceType = view?.findViewById<RadioButton>(rentalPriceTypeRadioGroup.checkedRadioButtonId)?.text.toString(),
            rules = rentalRulesEditText.text.toString(),
            description = rentalDescriptionEditText.text.toString(),
            landlordName = landlordNameEditText.text.toString(),
            landlordPhone = landlordPhoneEditText.text.toString(),
            landlordEmail = landlordEmailEditText.text.toString(),
            landlordWhatsapp = landlordWhatsappEditText.text.toString().ifEmpty { null },
            landlordType = view?.findViewById<RadioButton>(landlordTypeRadioGroup.checkedRadioButtonId)?.text.toString()
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

    private fun displayRentalImages() {
        rentalImageContainer.removeViews(0, rentalImageContainer.childCount - 1) // Clear previous images, keep button
        rentalPhotoUris.forEach { uri ->
            val imageView = ImageView(requireContext()).apply {
                layoutParams = ViewGroup.LayoutParams(160, 160) // size of image preview
                setImageURI(uri)
                scaleType = ImageView.ScaleType.CENTER_CROP
                setPadding(8, 0, 8, 0)
            }
            rentalImageContainer.addView(imageView, 0) // Add new images at the beginning
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