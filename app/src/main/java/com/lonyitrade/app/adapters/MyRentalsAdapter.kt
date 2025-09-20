package com.lonyitrade.app.adapters

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

        // New views for buttons
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

        // --- Bind data ---
        holder.propertyTypeTextView.text = rental.propertyType
        holder.priceTextView.text = "UGX ${rental.monthlyRent ?: 0} / month"
        holder.locationTextView.text = "${rental.city}, ${rental.district}"
        holder.roomsTextView.text = "${rental.rooms ?: 0} Rooms"
        holder.landlordNameTextView.text = "${rental.landlordName} (${rental.landlordType})"
        holder.landlordPhoneTextView.text = rental.landlordPhone

        // --- Show Edit/Delete buttons ---
        holder.editDeleteLayout.visibility = View.VISIBLE

        // --- Set click listeners ---
        holder.editButton.setOnClickListener { onEditClick(rental) }
        holder.deleteButton.setOnClickListener { onDeleteClick(rental, position) }

        // Load Image
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