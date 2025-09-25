// File: MyRentalsAdapter.kt
package com.lonyitrade.app.adapters

import android.content.Intent
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.lonyitrade.app.R
import com.lonyitrade.app.api.ApiClient
import com.lonyitrade.app.data.models.Rental
import com.lonyitrade.app.RentalDetailActivity

class MyRentalsAdapter(
    private var rentalList: MutableList<Rental>,
    private val onEditClick: (Rental) -> Unit,
    private val onDeleteClick: (Rental, Int) -> Unit
) : RecyclerView.Adapter<MyRentalsAdapter.RentalViewHolder>() {

    class RentalViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val photoImageView: ImageView = view.findViewById(R.id.rentalPhotoImageView)
        val progressBar: ProgressBar = view.findViewById(R.id.rentalImageProgressBar)
        val propertyTypeTextView: TextView = view.findViewById(R.id.rentalPropertyTypeTextView)
        val priceTextView: TextView = view.findViewById(R.id.rentalPriceTextView)
        val locationTextView: TextView = view.findViewById(R.id.rentalLocationTextView)
        val roomsTextView: TextView = view.findViewById(R.id.rentalRoomsTextView)
        val landlordNameTextView: TextView = view.findViewById(R.id.landlordNameTextView)
        val landlordPhoneTextView: TextView = view.findViewById(R.id.landlordPhoneTextView)
        val editDeleteLayout: LinearLayout = view.findViewById(R.id.editDeleteLayout)
        val editButton: Button = view.findViewById(R.id.editButton)
        val deleteButton: Button = view.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RentalViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.my_rental_item_card, parent, false)
        return RentalViewHolder(view)
    }

    override fun onBindViewHolder(holder: RentalViewHolder, position: Int) {
        val rental = rentalList[position]

        // Handle item click to open details page
        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, RentalDetailActivity::class.java).apply {
                putExtra("RENTAL_EXTRA", rental)
            }
            holder.itemView.context.startActivity(intent)
        }

        holder.propertyTypeTextView.text = rental.property_type ?: "N/A"
        holder.priceTextView.text = "UGX ${rental.monthly_rent ?: 0} / month"
        holder.locationTextView.text = "${rental.city ?: "N/A"}, ${rental.district ?: "N/A"}"
        holder.roomsTextView.text = "${rental.rooms ?: 0} Rooms"
        holder.landlordNameTextView.text = "${rental.landlord_name ?: "N/A"} (${rental.landlord_type ?: "N/A"})"
        holder.landlordPhoneTextView.text = rental.landlord_phone ?: "N/A"

        holder.editDeleteLayout.visibility = View.VISIBLE

        holder.editButton.setOnClickListener { onEditClick(rental) }
        holder.deleteButton.setOnClickListener { onDeleteClick(rental, position) }

        holder.progressBar.visibility = View.VISIBLE
        if (!rental.photos.isNullOrEmpty() && rental.photos.first() != null) {
            val imageUrl = ApiClient.BASE_URL.trimEnd('/') + "/" + rental.photos.first()!!.trimStart('/')
            Glide.with(holder.itemView.context)
                .load(imageUrl)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>, isFirstResource: Boolean): Boolean {
                        holder.progressBar.visibility = View.GONE
                        return false
                    }
                    override fun onResourceReady(resource: Drawable, model: Any, target: Target<Drawable>, dataSource: DataSource, isFirstResource: Boolean): Boolean {
                        holder.progressBar.visibility = View.GONE
                        return false
                    }
                })
                .error(R.drawable.ic_add_photo)
                .into(holder.photoImageView)
        } else {
            holder.progressBar.visibility = View.GONE
            holder.photoImageView.setImageResource(R.drawable.ic_add_photo)
        }
    }

    override fun getItemCount() = rentalList.size

    fun removeItem(position: Int) {
        if (position >= 0 && position < rentalList.size) {
            rentalList.removeAt(position)
            notifyItemRemoved(position)
        }
    }
}