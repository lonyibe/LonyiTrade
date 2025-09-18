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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.lonyitrade.app.data.models.Ad
import com.lonyitrade.app.utils.SessionManager
import com.lonyitrade.app.viewmodels.SharedViewModel

class PostAdFragment : Fragment() {

    private lateinit var sessionManager: SessionManager
    private var selectedPhotoUri: Uri? = null

    private val sharedViewModel: SharedViewModel by activityViewModels()

    // View variables for the sell form
    private lateinit var sellFormLayout: View
    private lateinit var titleEditText: EditText
    private lateinit var descriptionEditText: EditText
    private lateinit var priceEditText: EditText
    private lateinit var priceTypeRadioGroup: RadioGroup
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

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
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
        priceTypeRadioGroup = view.findViewById(R.id.priceTypeRadioGroup)
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
        val photo2ImageView = view.findViewById<ImageView>(R.id.photo2ImageView)
        val photo3ImageView = view.findViewById<ImageView>(R.id.photo3ImageView)
        val photo4ImageView = view.findViewById<ImageView>(R.id.photo4ImageView)


        // Handle photo upload button click
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
            if (sellFormLayout.visibility == View.VISIBLE) {
                val title = titleEditText.text.toString()
                val description = descriptionEditText.text.toString()
                val price = priceEditText.text.toString()
                val district = sellDistrictEditText.text.toString()
                val conditionId = sellConditionRadioGroup.checkedRadioButtonId
                val condition = when(conditionId) {
                    R.id.conditionNew -> "New"
                    R.id.conditionUsed -> "Used"
                    R.id.conditionRefurbished -> "Refurbished"
                    else -> "N/A"
                }

                val newAd = Ad("sell", title, description, price, null, condition, district, sessionManager.getPhoneNumber(), false, if (selectedPhotoUri != null) listOf(selectedPhotoUri.toString()) else emptyList())
                sharedViewModel.addAd(newAd)

                Toast.makeText(requireContext(), "Selling ad posted: Title - $title, District - $district", Toast.LENGTH_SHORT).show()
            } else if (buyFormLayout.visibility == View.VISIBLE) {
                val itemName = itemTitleEditText.text.toString()
                val itemDescription = itemDescriptionEditText.text.toString()
                val budget = budgetEditText.text.toString()
                val district = buyDistrictEditText.text.toString()
                val contactPhone = buyContactPhoneEditText.text.toString()
                val showOnWhatsapp = buyWhatsappSwitch.isChecked
                val conditionId = buyConditionRadioGroup.checkedRadioButtonId
                val condition = when(conditionId) {
                    R.id.buy_conditionNew -> "New"
                    R.id.buy_conditionUsed -> "Used"
                    R.id.buy_conditionRefurbished -> "Refurbished"
                    else -> "N/A"
                }

                val newAd = Ad("buy", itemName, itemDescription, null, budget, condition, district, contactPhone, showOnWhatsapp, emptyList())
                sharedViewModel.addAd(newAd)

                Toast.makeText(requireContext(), "Buying request posted: Item - $itemName, Budget - $budget, District - $district, Contact: $contactPhone (WhatsApp: $showOnWhatsapp)", Toast.LENGTH_SHORT).show()
            }
        }
    }
}