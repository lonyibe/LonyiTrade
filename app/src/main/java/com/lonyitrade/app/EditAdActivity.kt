package com.lonyitrade.app

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.lonyitrade.app.api.ApiClient
import com.lonyitrade.app.data.models.Ad
import com.lonyitrade.app.data.models.AdRequest
import com.lonyitrade.app.utils.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditAdActivity : AppCompatActivity() {

    private lateinit var ad: Ad
    private lateinit var sessionManager: SessionManager

    // UI Elements
    private lateinit var titleEditText: EditText
    private lateinit var descriptionEditText: EditText
    private lateinit var priceEditText: EditText
    private lateinit var districtEditText: EditText
    private lateinit var priceTypeRadioGroup: RadioGroup
    private lateinit var conditionRadioGroup: RadioGroup
    private lateinit var categorySpinner: Spinner
    private lateinit var updateButton: Button
    private lateinit var updateProgressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_ad)

        sessionManager = SessionManager(this)
        ad = intent.getParcelableExtra("AD_EXTRA") ?: run {
            Toast.makeText(this, "Error loading ad data", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initializeViews()
        setupCategorySpinner()
        populateFields()

        updateButton.setOnClickListener {
            handleUpdate()
        }
    }

    private fun initializeViews() {
        titleEditText = findViewById(R.id.editTitleEditText)
        descriptionEditText = findViewById(R.id.editDescriptionEditText)
        priceEditText = findViewById(R.id.editPriceEditText)
        districtEditText = findViewById(R.id.editDistrictEditText)
        priceTypeRadioGroup = findViewById(R.id.editPriceTypeRadioGroup)
        conditionRadioGroup = findViewById(R.id.editConditionRadioGroup)
        categorySpinner = findViewById(R.id.editCategorySpinner)
        updateButton = findViewById(R.id.updateAdButton)
        updateProgressBar = findViewById(R.id.updateAdProgressBar)
    }

    private fun setupCategorySpinner() {
        val categories = resources.getStringArray(R.array.categories)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = adapter
    }

    private fun populateFields() {
        titleEditText.setText(ad.title)
        descriptionEditText.setText(ad.description)
        priceEditText.setText(ad.price.toString()) // Convert Double to String
        districtEditText.setText(ad.district)

        // Select the correct radio buttons
        selectRadioButton(priceTypeRadioGroup, ad.priceType)
        selectRadioButton(conditionRadioGroup, ad.condition)

        // Select the correct category in the spinner
        val categories = resources.getStringArray(R.array.categories)
        val categoryIndex = categories.indexOf(ad.category)
        if (categoryIndex != -1) {
            categorySpinner.setSelection(categoryIndex)
        }
    }

    private fun selectRadioButton(radioGroup: RadioGroup, value: String?) {
        for (i in 0 until radioGroup.childCount) {
            val radioButton = radioGroup.getChildAt(i) as? RadioButton
            if (radioButton?.text.toString().equals(value, ignoreCase = true)) {
                radioButton?.isChecked = true
                break
            }
        }
    }

    private fun handleUpdate() {
        val title = titleEditText.text.toString()
        val description = descriptionEditText.text.toString()
        val price = priceEditText.text.toString().toDoubleOrNull()
        val district = districtEditText.text.toString()
        val priceType = (findViewById<RadioButton>(priceTypeRadioGroup.checkedRadioButtonId))?.text.toString()
        val condition = (findViewById<RadioButton>(conditionRadioGroup.checkedRadioButtonId))?.text.toString()
        val category = categorySpinner.selectedItem.toString()
        val token = sessionManager.fetchAuthToken()

        if (title.isEmpty() || description.isEmpty() || price == null || district.isEmpty() || category.isEmpty() || token == null) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)
        val adRequest = AdRequest(title, description, category, ad.type ?: "for_sale", price, priceType, district, condition)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Correct call to updateAdvert
                val response = ApiClient.apiService.updateAdvert("Bearer $token", ad.id!!, adRequest)
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    if (response.isSuccessful) {
                        Toast.makeText(this@EditAdActivity, "Ad updated successfully!", Toast.LENGTH_SHORT).show()
                        finish() // Go back to the My Ads screen
                    } else {
                        Toast.makeText(this@EditAdActivity, "Failed to update ad: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Toast.makeText(this@EditAdActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            updateButton.text = ""
            updateProgressBar.visibility = View.VISIBLE
            updateButton.isEnabled = false
        } else {
            updateButton.text = "Update Ad"
            updateProgressBar.visibility = View.GONE
            updateButton.isEnabled = true
        }
    }
}