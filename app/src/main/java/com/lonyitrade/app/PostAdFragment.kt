package com.lonyitrade.app

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.lonyitrade.app.api.ApiClient
import com.lonyitrade.app.data.models.AdRequest
import com.lonyitrade.app.utils.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class PostAdFragment : Fragment() {

    private lateinit var sessionManager: SessionManager
    private var selectedPhotoUri: Uri? = null

    // UI elements
    private lateinit var postAdButton: Button
    private lateinit var postAdProgressBar: ProgressBar
    private lateinit var sellFormLayout: View
    private lateinit var buyFormLayout: View
    private lateinit var tradeTypeRadioGroup: RadioGroup

    // Sell form fields
    private lateinit var titleEditText: EditText
    private lateinit var descriptionEditText: EditText
    private lateinit var priceEditText: EditText
    private lateinit var priceTypeRadioGroup: RadioGroup
    private lateinit var sellConditionRadioGroup: RadioGroup
    private lateinit var sellDistrictEditText: EditText

    // Buy form fields
    private lateinit var itemTitleEditText: EditText
    private lateinit var itemDescriptionEditText: EditText
    private lateinit var budgetEditText: EditText
    private lateinit var buyConditionRadioGroup: RadioGroup
    private lateinit var buyDistrictEditText: EditText

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
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
        initializeViews(view)

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
            handlePostAd()
        }

        view.findViewById<ImageView>(R.id.photo1ImageView).setOnClickListener {
            pickImage.launch("image/*")
        }
    }

    private fun initializeViews(view: View) {
        postAdButton = view.findViewById(R.id.postAdButton)
        postAdProgressBar = view.findViewById(R.id.postAdProgressBar)
        tradeTypeRadioGroup = view.findViewById(R.id.tradeTypeRadioGroup)
        sellFormLayout = view.findViewById(R.id.sell_form_layout)
        buyFormLayout = view.findViewById(R.id.buy_form_layout)

        // Sell form
        titleEditText = view.findViewById(R.id.titleEditText)
        descriptionEditText = view.findViewById(R.id.descriptionEditText)
        priceEditText = view.findViewById(R.id.priceEditText)
        priceTypeRadioGroup = view.findViewById(R.id.priceTypeRadioGroup)
        sellConditionRadioGroup = view.findViewById(R.id.conditionRadioGroup)
        sellDistrictEditText = view.findViewById(R.id.districtEditText)

        // Buy form
        itemTitleEditText = view.findViewById(R.id.itemTitleEditText)
        itemDescriptionEditText = view.findViewById(R.id.itemDescriptionEditText)
        budgetEditText = view.findViewById(R.id.budgetEditText)
        buyConditionRadioGroup = view.findViewById(R.id.buy_conditionRadioGroup)
        buyDistrictEditText = view.findViewById(R.id.buy_districtEditText)
    }

    private fun handlePostAd() {
        val token = sessionManager.fetchAuthToken()
        if (token.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Please log in to post an ad", Toast.LENGTH_SHORT).show()
            return
        }

        val adRequest: AdRequest? = when (tradeTypeRadioGroup.checkedRadioButtonId) {
            R.id.tradeTypeSell -> createSellAdRequest()
            R.id.tradeTypeBuy -> createBuyAdRequest()
            else -> null
        }

        if (adRequest != null) {
            showLoading(true)
            postAd(token, adRequest)
        }
    }

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            postAdButton.text = ""
            postAdProgressBar.visibility = View.VISIBLE
            postAdButton.isEnabled = false
        } else {
            postAdButton.text = "Post Ad"
            postAdProgressBar.visibility = View.GONE
            postAdButton.isEnabled = true
        }
    }

    private fun createSellAdRequest(): AdRequest? {
        val title = titleEditText.text.toString()
        val description = descriptionEditText.text.toString()
        val price = priceEditText.text.toString().toDoubleOrNull()
        val district = sellDistrictEditText.text.toString()
        val priceType = view?.findViewById<RadioButton>(priceTypeRadioGroup.checkedRadioButtonId)?.text.toString()
        val condition = view?.findViewById<RadioButton>(sellConditionRadioGroup.checkedRadioButtonId)?.text.toString()

        if (title.isEmpty() || description.isEmpty() || price == null || district.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill in all sell form fields", Toast.LENGTH_SHORT).show()
            return null
        }
        return AdRequest(title, description, "Unspecified", "for_sale", price, priceType, district, condition)
    }

    private fun createBuyAdRequest(): AdRequest? {
        val title = itemTitleEditText.text.toString()
        val description = itemDescriptionEditText.text.toString()
        val budget = budgetEditText.text.toString().toDoubleOrNull()
        val district = buyDistrictEditText.text.toString()
        val condition = view?.findViewById<RadioButton>(buyConditionRadioGroup.checkedRadioButtonId)?.text.toString()

        if (title.isEmpty() || description.isEmpty() || budget == null || district.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill in all buy form fields", Toast.LENGTH_SHORT).show()
            return null
        }
        return AdRequest(title, description, "Unspecified", "wanted", budget, "Budget", district, condition)
    }

    private fun postAd(token: String, adRequest: AdRequest) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.apiService.postAdvert("Bearer $token", adRequest)
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    if (response.isSuccessful) {
                        val postedAd = response.body()
                        if (postedAd?.id != null && selectedPhotoUri != null) {
                            uploadPhotoForAd(token, postedAd.id, selectedPhotoUri!!)
                        } else {
                            Toast.makeText(requireContext(), "Ad posted successfully!", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(requireContext(), "Failed to post ad: ${response.code()}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Toast.makeText(requireContext(), "Network error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun uploadPhotoForAd(token: String, adId: String, photoUri: Uri) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val file = getTempFileFromUri(photoUri)
                if (file == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Failed to create temporary file for upload.", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }
                val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                val photoPart = MultipartBody.Part.createFormData("photo", file.name, requestFile)

                val response = ApiClient.apiService.uploadAdPhoto("Bearer $token", adId, photoPart)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(requireContext(), "Ad posted and photo uploaded successfully!", Toast.LENGTH_SHORT).show()
                        file.delete()
                    } else {
                        Toast.makeText(requireContext(), "Failed to upload photo: ${response.code()}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Photo upload network error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun getTempFileFromUri(uri: Uri): File? {
        return try {
            val inputStream: InputStream? = requireContext().contentResolver.openInputStream(uri)
            val file = File(requireContext().cacheDir, "temp_image_for_upload.jpg")
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            file
        } catch (e: Exception) {
            null
        }
    }
}