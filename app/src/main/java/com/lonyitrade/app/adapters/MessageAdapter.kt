package com.lonyitrade.app.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.lonyitrade.app.FullScreenImageActivity
import com.lonyitrade.app.R
import com.lonyitrade.app.api.ApiClient
import com.lonyitrade.app.data.models.Message
import java.text.SimpleDateFormat
import java.util.*

class MessageAdapter(private val messageList: List<Message>, private val currentUserId: String) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }

    override fun getItemViewType(position: Int): Int {
        val message = messageList[position]
        return if (message.senderId == currentUserId) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_SENT) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message_sent, parent, false)
            SentMessageViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message_received, parent, false)
            ReceivedMessageViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messageList[position]
        when (holder.itemViewType) {
            VIEW_TYPE_SENT -> (holder as SentMessageViewHolder).bind(message)
            VIEW_TYPE_RECEIVED -> (holder as ReceivedMessageViewHolder).bind(message)
        }
    }

    override fun getItemCount() = messageList.size

    private fun formatTime(timestamp: String?): String {
        return try {
            if (timestamp.isNullOrEmpty()) return ""
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            parser.timeZone = TimeZone.getTimeZone("UTC")
            val date = parser.parse(timestamp)
            val formatter = SimpleDateFormat("h:mm a", Locale.getDefault())
            date?.let { formatter.format(it) } ?: ""
        } catch (e: Exception) {
            timestamp ?: ""
        }
    }

    inner class SentMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageTextView: TextView = itemView.findViewById(R.id.messageText)
        private val messageImageView: ImageView = itemView.findViewById(R.id.messageImage)
        private val timeTextView: TextView = itemView.findViewById(R.id.messageTime)
        private val statusIcon: ImageView = itemView.findViewById(R.id.messageStatusIcon)

        fun bind(message: Message) {
            timeTextView.text = formatTime(message.createdAt)

            // Handle media URL
            if (!message.mediaUrl.isNullOrEmpty()) {
                messageImageView.visibility = View.VISIBLE
                val imageUrl = ApiClient.BASE_URL.trimEnd('/') + "/" + message.mediaUrl.trimStart('/')
                Glide.with(itemView.context).load(imageUrl).placeholder(R.drawable.ic_add_photo).into(messageImageView)
                messageImageView.setOnClickListener {
                    val intent = Intent(itemView.context, FullScreenImageActivity::class.java)
                    intent.putExtra("IMAGE_URL", message.mediaUrl)
                    itemView.context.startActivity(intent)
                }
            } else {
                messageImageView.visibility = View.GONE
            }

            // Handle text content
            if (!message.content.isNullOrEmpty()) {
                messageTextView.visibility = View.VISIBLE
                messageTextView.text = message.content
            } else {
                messageTextView.visibility = View.GONE
            }

            // Update status icon
            statusIcon.visibility = View.VISIBLE
            when (message.status) {
                "sent" -> {
                    statusIcon.setImageResource(R.drawable.ic_single_tick)
                    statusIcon.setColorFilter(ContextCompat.getColor(itemView.context, R.color.google_files_text_secondary))
                }
                "delivered" -> {
                    statusIcon.setImageResource(R.drawable.ic_double_tick)
                    statusIcon.setColorFilter(ContextCompat.getColor(itemView.context, R.color.google_files_text_secondary))
                }
                "read" -> {
                    statusIcon.setImageResource(R.drawable.ic_double_tick_blue)
                    statusIcon.clearColorFilter() // Use the original blue color from the drawable
                }
                else -> statusIcon.visibility = View.GONE // Hide for unknown status or temporary messages
            }
        }
    }

    inner class ReceivedMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageTextView: TextView = itemView.findViewById(R.id.messageText)
        private val messageImageView: ImageView = itemView.findViewById(R.id.messageImage)
        private val timeTextView: TextView = itemView.findViewById(R.id.messageTime)

        fun bind(message: Message) {
            timeTextView.text = formatTime(message.createdAt)

            // Handle media URL
            if (!message.mediaUrl.isNullOrEmpty()) {
                messageImageView.visibility = View.VISIBLE
                val imageUrl = ApiClient.BASE_URL.trimEnd('/') + "/" + message.mediaUrl.trimStart('/')
                Glide.with(itemView.context).load(imageUrl).placeholder(R.drawable.ic_add_photo).into(messageImageView)
                messageImageView.setOnClickListener {
                    val intent = Intent(itemView.context, FullScreenImageActivity::class.java)
                    intent.putExtra("IMAGE_URL", message.mediaUrl)
                    itemView.context.startActivity(intent)
                }
            } else {
                messageImageView.visibility = View.GONE
            }

            // Handle text content
            if (!message.content.isNullOrEmpty()) {
                messageTextView.visibility = View.VISIBLE
                messageTextView.text = message.content
            } else {
                messageTextView.visibility = View.GONE
            }
        }
    }
}
