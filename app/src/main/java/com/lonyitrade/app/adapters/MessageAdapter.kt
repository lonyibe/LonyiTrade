package com.lonyitrade.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.lonyitrade.app.R
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
        return if (message.senderId == currentUserId) {
            VIEW_TYPE_SENT
        } else {
            VIEW_TYPE_RECEIVED
        }
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

    // ViewHolder for messages sent by the current user
    inner class SentMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageTextView: TextView = itemView.findViewById(R.id.messageText)
        private val timeTextView: TextView = itemView.findViewById(R.id.messageTime)

        fun bind(message: Message) {
            messageTextView.text = message.content
            timeTextView.text = formatTime(message.createdAt)
        }
    }

    // ViewHolder for messages received from other users
    inner class ReceivedMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageTextView: TextView = itemView.findViewById(R.id.messageText)
        private val timeTextView: TextView = itemView.findViewById(R.id.messageTime)

        fun bind(message: Message) {
            messageTextView.text = message.content
            timeTextView.text = formatTime(message.createdAt)
        }
    }

    // A utility function to format the timestamp, now accessible to inner classes
    private fun formatTime(timestamp: String): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            parser.timeZone = TimeZone.getTimeZone("UTC")
            val date = parser.parse(timestamp)
            val formatter = SimpleDateFormat("h:mm a", Locale.getDefault())
            if (date != null) {
                formatter.format(date)
            } else {
                timestamp.substringAfter('T').substringBefore('.')
            }
        } catch (e: Exception) {
            timestamp.substringAfter('T').substringBefore('.')
        }
    }
}