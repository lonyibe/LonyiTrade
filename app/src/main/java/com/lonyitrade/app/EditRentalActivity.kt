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
import com.lonyitrade.app.data.models.Rental
import com.lonyitrade.app.data.models.RentalRequest
import com.lonyitrade.app.utils.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditRentalActivity : AppCompatActivity() {

    private lateinit var rental: Rental
    private lateinit var sessionManager: SessionManager

    // UI Elements
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
    private lateinit var updateButton: Button
    private lateinit var updateProgressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_rental)

        sessionManager = SessionManager(this)
        rental = intent.getParcelableExtra("RENTAL_EXTRA") ?: run {
            Toast.makeText(this, "Error loading rental data", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initializeViews()
        populateFields()

        updateButton.setOnClickListener {
            handleUpdate()
        }
    }

    private fun initializeViews() {
        propertyTypeSpinner = findViewById(R.id.editPropertyTypeSpinner)
        rentalRoomsEditText = findViewById(R.id.editRentalRoomsEditText)
        rentalDescriptionEditText = findViewById(R.id.editRentalDescriptionEditText)
        rentalRulesEditText = findViewById(R.id.editRentalRulesEditText)
        rentalCityEditText = findViewById(R.id.editRentalCityEditText)
        rentalDistrictEditText = findViewById(R.id.editRentalDistrictEditText)
        rentalLocationDescEditText = findViewById(R.id.editRentalLocationDescEditText)
        rentalRentEditText = findViewById(R.id.editRentalRentEditText)
        rentalPriceTypeRadioGroup = findViewById(R.id.editRentalPriceTypeRadioGroup)
        landlordTypeRadioGroup = findViewById(R.id.editLandlordTypeRadioGroup)
        landlordNameEditText = findViewById(R.id.editLandlordNameEditText)
        landlordPhoneEditText = findViewById(R.id.editLandlordPhoneEditText)
        landlordEmailEditText = findViewById(R.id.editLandlordEmailEditText)
        landlordWhatsappEditText = findViewById(R.id.editLandlordWhatsappEditText)
        updateButton = findViewById(R.id.updateRentalButton)
        updateProgressBar = findViewById(R.id.updateRentalProgressBar)

        val propertyTypes = arrayOf("House", "Apartment", "Studio", "Condo", "Duplex", "Hostel", "Other")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, propertyTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        propertyTypeSpinner.adapter = adapter
    }

    private fun populateFields() {
        val propertyTypePosition = (propertyTypeSpinner.adapter as ArrayAdapter<String>).getPosition(rental.property_type)
        if (propertyTypePosition != -1) {
            propertyTypeSpinner.setSelection(propertyTypePosition)
        }
        rentalRoomsEditText.setText(rental.rooms?.toString() ?: "")
        rentalDescriptionEditText.setText(rental.description)
        rentalRulesEditText.setText(rental.rules)
        rentalCityEditText.setText(rental.city)
        rentalDistrictEditText.setText(rental.district)
        rentalLocationDescEditText.setText(rental.location_description)
        rentalRentEditText.setText(rental.monthly_rent?.toString() ?: "")
        landlordNameEditText.setText(rental.landlord_name)
        landlordPhoneEditText.setText(rental.landlord_phone)
        landlordEmailEditText.setText(rental.landlord_email)
        landlordWhatsappEditText.setText(rental.landlord_whatsapp)

        selectRadioButton(rentalPriceTypeRadioGroup, rental.price_type)
        selectRadioButton(landlordTypeRadioGroup, rental.landlord_type)
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
        val token = sessionManager.fetchAuthToken()
        val rentalId = rental.id ?: return

        val propertyType = propertyTypeSpinner.selectedItem.toString()
        val rooms = rentalRoomsEditText.text.toString().toIntOrNull()
        val description = rentalDescriptionEditText.text.toString()
        val rules = rentalRulesEditText.text.toString()
        val city = rentalCityEditText.text.toString()
        val district = rentalDistrictEditText.text.toString()
        val locationDesc = rentalLocationDescEditText.text.toString()
        val monthlyRent = rentalRentEditText.text.toString().toDoubleOrNull()
        val priceType = findViewById<RadioButton>(rentalPriceTypeRadioGroup.checkedRadioButtonId)?.text.toString()
        val landlordType = findViewById<RadioButton>(landlordTypeRadioGroup.checkedRadioButtonId)?.text.toString()
        val landlordName = landlordNameEditText.text.toString()
        val landlordPhone = landlordPhoneEditText.text.toString()
        val landlordEmail = landlordEmailEditText.text.toString()
        val landlordWhatsapp = landlordWhatsappEditText.text.toString().ifEmpty { null }

        if (token == null || rooms == null || monthlyRent == null) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)
        val rentalRequest = RentalRequest(
            property_type = propertyType,
            city = city,
            district = district,
            rooms = rooms,
            location_description = locationDesc,
            monthly_rent = monthlyRent,
            price_type = priceType,
            rules = rules,
            description = description,
            landlord_name = landlordName,
            landlord_phone = landlordPhone,
            landlord_email = landlordEmail,
            landlord_whatsapp = landlordWhatsapp,
            landlord_type = landlordType
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.apiService.updateRental("Bearer $token", rentalId, rentalRequest)
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    if (response.isSuccessful) {
                        Toast.makeText(this@EditRentalActivity, "Rental updated successfully!", Toast.LENGTH_SHORT).show()
                        setResult(RESULT_OK)
                        finish()
                    } else {
                        Toast.makeText(this@EditRentalActivity, "Failed to update rental", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Toast.makeText(this@EditRentalActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
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
            updateButton.text = "Update Rental"
            updateProgressBar.visibility = View.GONE
            updateButton.isEnabled = true
        }
    }
}